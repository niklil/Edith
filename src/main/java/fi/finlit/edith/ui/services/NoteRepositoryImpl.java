/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import static fi.finlit.edith.domain.QDocumentNote.documentNote;
import static fi.finlit.edith.domain.QNote.note;
import static fi.finlit.edith.domain.QTermWithNotes.termWithNotes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.ComparableExpressionBase;
import com.mysema.query.types.path.StringPath;
import com.mysema.rdfbean.object.BeanQuery;
import com.mysema.rdfbean.object.BeanSubQuery;
import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SessionFactory;

import fi.finlit.edith.EDITH;
import fi.finlit.edith.domain.*;

public class NoteRepositoryImpl extends AbstractRepository<Note> implements NoteRepository {

    private static final Logger logger = LoggerFactory.getLogger(NoteRepositoryImpl.class);

    private static final class LoopContext {
        private Note note;
        private String text;
        private Paragraph paragraphs;
        private int counter;
        private boolean inBib;
        private boolean inA;
        private String reference;
        private String url;

        private LoopContext() {
            note = null;
            text = null;
            counter = 0;
        }
    }

    private final TimeService timeService;

    private final UserRepository userRepository;

    private final AuthService authService;

    private final boolean extendedTerm;

    public NoteRepositoryImpl(@Inject SessionFactory sessionFactory,
            @Inject UserRepository userRepository,
            @Inject TimeService timeService,
            @Inject AuthService authService,
            @Inject @Symbol(EDITH.EXTENDED_TERM) boolean extendedTerm) {
        super(sessionFactory, note);
        this.userRepository = userRepository;
        this.timeService = timeService;
        this.authService = authService;
        this.extendedTerm = extendedTerm;
    }

    @Override
    public NoteComment createComment(Concept concept, String message) {
        NoteComment comment = new NoteComment(concept, message, authService.getUsername());
        getSession().save(comment);
        return comment;
    }

    @Override
    @Deprecated // Use griddatasource version
    public List<Note> findAllNotes(DocumentNoteSearchInfo searchInfo) {
        long start = System.currentTimeMillis();
        Assert.notNull(searchInfo);
        BooleanBuilder filters = new BooleanBuilder();
        QNote note = QNote.note;
        QDocumentNote documentNote = QDocumentNote.documentNote;

        Predicate nonOrphan = null, orphan = null;

        // document
        if (!searchInfo.getDocuments().isEmpty()){
            // create filter condition for non orphan matches
            BooleanBuilder builder = new BooleanBuilder();
            builder.and(BooleanExpression.allOf(documentNote.note().eq(note),
                    documentNote.document().in(searchInfo.getDocuments())));

            //Remove deleted if orphans is false
            //XXX Would be better if the orphan search could be done without
            // using document references
            if (!searchInfo.isOrphans()) {
                builder.and(documentNote.deleted.isFalse());
            }
            nonOrphan = builder.getValue();
        }

        // orphans
        if (searchInfo.isOrphans()){
            // create filter condition for orphan matches
            // notes which don't have any document notes at all
            orphan = sub(documentNote).where(documentNote.note().eq(note)).notExists();
        }

        // creators
        if (!searchInfo.getCreators().isEmpty()) {
            BooleanBuilder filter = new BooleanBuilder();
            Collection<String> usernames = new ArrayList<String>(searchInfo.getCreators().size());
            for (UserInfo userInfo : searchInfo.getCreators()) {
                filter.or(note.concept(extendedTerm).allEditors.contains(userRepository.getUserInfoByUsername(userInfo.getUsername())));
                usernames.add(userInfo.getUsername());
            }
            // FIXME This is kind of useless except that we have broken data in production.
            filter.or(note.concept(extendedTerm).lastEditedBy().username.in(usernames));
            filters.and(filter);
        }

        // formats
        if (!searchInfo.getNoteFormats().isEmpty()) {
            filters.and(note.format.in(searchInfo.getNoteFormats()));
        }

        // types
        if (!searchInfo.getNoteTypes().isEmpty()) {
            BooleanBuilder filter = new BooleanBuilder();
            for (NoteType type : searchInfo.getNoteTypes()) {
                filter.or(note.concept(extendedTerm).types.contains(type));
            }
            filters.and(filter);
        }

        // get matching notes
        Session session = getSession();
        List<Note> notes = new ArrayList<Note>();
        OrderSpecifier<?> order = getOrderBy(searchInfo, note);
        if (nonOrphan != null){
            if (filters.getValue() != null){ // TODO : simplify after next Querydsl release
                notes.addAll(session.from(note, documentNote).where(nonOrphan, filters.getValue()).orderBy(order).listDistinct(note));
            }else{
                notes.addAll(session.from(note, documentNote).where(nonOrphan).orderBy(order).listDistinct(note));
            }
        }
        if (orphan != null){
            if (filters.getValue() != null){ // TODO : simplify after next Querydsl release
                notes.addAll(session.from(note).where(orphan, filters.getValue()).orderBy(order).list(note));
            }else{
                notes.addAll(session.from(note).where(orphan).orderBy(order).list(note));
            }

        }
        if (orphan == null && nonOrphan == null){
            if (filters.getValue() != null){ // TODO : simplify after next Querydsl release
                notes.addAll(session.from(note).where(filters.getValue()).orderBy(order).list(note));
            }else{
                notes.addAll(session.from(note).orderBy(order).list(note));
            }

        }
        logDuration("NoteRepository.findNotes", start);
        return notes;
    }
    @Override
    public GridDataSource findNotes(DocumentNoteSearchInfo search) {
        BooleanBuilder builder = new BooleanBuilder();
        BooleanBuilder documentRefs = new BooleanBuilder();

        // document
        if (!search.getDocuments().isEmpty()){

            BeanSubQuery subQuery = sub(documentNote);
            
            //BooleanBuilder filter = new BooleanBuilder();
            
            subQuery.where(documentNote.note().eq(note),
                        documentNote.document().in(search.getDocuments()),
                        documentNote.deleted.eq(false));
            
            //builder.and(subQuery.where(filter).exists());

//            // create filter condition for non orphan matches
//            if (!search.isOrphans()) {
//                subQuery.where(BooleanExpression.allOf(documentNote.note().eq(note),
//                    documentNote.document().in(search.getDocuments()),
//                    documentNote.replacedBy().isNull(),
//                    documentNote.deleted.eq(false)));
//            } else {
//                subQuery.where(BooleanExpression.allOf(documentNote.note().eq(note),
//                        documentNote.document().in(search.getDocuments()),
//                        documentNote.replacedBy().isNull()));
//            }
//            
            documentRefs.or(subQuery.exists());
        }

        // orphans
        if (search.isOrphans()){
            // create filter condition for orphan matches
            // notes which don't have any document notes at all
            //documentRefs.or(sub(documentNote).where(documentNote.note().isNull()).exists());
            documentRefs.or(note.documentNoteCount.eq(0));
        }

        //Add documentrefs to rest of search
        if(documentRefs.hasValue()) {
            builder.and(documentRefs);
        }
        
        //language
        if (search.getLanguage() != null) {
            builder.and(note.term().language.eq(search.getLanguage()));
        }
        
        // fulltext
        if (StringUtils.isNotBlank(search.getFullText())) {
            BooleanBuilder filter = new BooleanBuilder();
            for (StringPath path : Arrays.asList(note.lemma, note.term().basicForm,
                    note.term().meaning, note.concept(extendedTerm).description,
                    note.concept(extendedTerm).sources,
                    note.concept(extendedTerm).comments.any().message)) {
                filter.or(path.containsIgnoreCase(search.getFullText()));
            }
            builder.and(filter);
        }

        // creators
        if (!search.getCreators().isEmpty()) {
            BooleanBuilder filter = new BooleanBuilder();
            Collection<String> usernames = new ArrayList<String>(search.getCreators().size());
            for (UserInfo userInfo : search.getCreators()) {
                filter.or(note.concept(extendedTerm).allEditors.contains(
                        userRepository.getUserInfoByUsername(userInfo.getUsername())));
                usernames.add(userInfo.getUsername());
            }
            // FIXME This is kind of useless except that we have broken data in production.
            filter.or(note.concept(extendedTerm).lastEditedBy().username.in(usernames));
            builder.and(filter);
        }

        // formats
        if (!search.getNoteFormats().isEmpty()) {
            builder.and(note.format.in(search.getNoteFormats()));
        }

        // types
        if (!search.getNoteTypes().isEmpty()) {
            BooleanBuilder filter = new BooleanBuilder();
            for (NoteType type : search.getNoteTypes()) {
                filter.or(note.concept(extendedTerm).types.contains(type));
            }
            builder.and(filter);
        }
        
        //TODO We need datasource parameter to get distinct results from here
        return createGridDataSource(note, getOrderBy(search, note), false, builder.getValue());
    }

    @Override
    public List<NoteWithInstances> findNotesWithInstances(DocumentNoteSearchInfo searchInfo) {
        List<Note> notes = findAllNotes(searchInfo);
        long start = System.currentTimeMillis();
        if (!notes.isEmpty()){
            // get related document notes
            List<DocumentNote> documentNotes = getActiveDocumentNotes(searchInfo.getCurrentDocument(), notes);

            // map document notes to notes
            Map<Note, Set<DocumentNote>> noteToDocumentNotes = new HashMap<Note, Set<DocumentNote>>();
            for (DocumentNote dn : documentNotes){
                Set<DocumentNote> dnSet = noteToDocumentNotes.get(dn.getNote());
                if (dnSet == null){
                    dnSet = new HashSet<DocumentNote>();
                    noteToDocumentNotes.put(dn.getNote(), dnSet);
                }
                dnSet.add(dn);
            }

            // create return value
            List<NoteWithInstances> rv = new ArrayList<NoteWithInstances>(notes.size());
            for (Note n : notes){
                if (noteToDocumentNotes.containsKey(n)){
                    rv.add(new NoteWithInstances(n, noteToDocumentNotes.get(n)));
                }else{
                    rv.add(new NoteWithInstances(n, Collections.<DocumentNote>emptySet()));
                }
            }

            logDuration("NoteRepository.findNotesWithInstances", start);
            return rv;

        }else{
            List<NoteWithInstances> rv = new ArrayList<NoteWithInstances>(notes.size());
            for (Note n : notes){
                rv.add(new NoteWithInstances(n, Collections.<DocumentNote>emptySet()));
            }

            logDuration("NoteRepository.findNotesWithInstaces", start);
            return rv;
        }

    }

    private List<DocumentNote> getActiveDocumentNotes(Document document, Collection<Note> notes){
        long start = System.currentTimeMillis();

        BeanQuery query = getSession().from(documentNote);

        // of current document
        query.where(documentNote.document().eq(document));

        // of given note
        query.where(documentNote.note().in(notes));

        // not deleted
        query.where(documentNote.deleted.eq(false));

        List<DocumentNote> rv = query.list(documentNote);

        logDuration("NoteRepository.getActiveDocumentNotes", start);
        return rv;
    }

    @Override
    public GridDataSource queryNotes(String searchTerm) {
        QNote note = QNote.note;
        Assert.notNull(searchTerm);
        BooleanBuilder builder = new BooleanBuilder();
        if (!searchTerm.equals("*")) {
            for (StringPath path : Arrays.asList(
                    note.lemma,
                    note.term().basicForm,
                    note.term().meaning)) {
                // ,
                // documentNote.description, FIXME
                // note.subtextSources)
                builder.or(path.containsIgnoreCase(searchTerm));
            }
        }

        return createGridDataSource(note, note.term().basicForm.lower().asc(), false, builder.getValue());
    }


    @Override
    public NoteComment getCommentById(String id) {
        return getSession().getById(id, NoteComment.class);
    }


    private OrderSpecifier<?> getOrderBy(DocumentNoteSearchInfo searchInfo, QNote note) {
        ComparableExpressionBase<?> comparable = null;
        OrderBy orderBy = searchInfo.getOrderBy() == null ? OrderBy.LEMMA : searchInfo.getOrderBy();
        switch (orderBy) {
        case KEYTERM:
            comparable = note.term().basicForm;
        case DATE:
            comparable = note.editedOn;
            break;
        case USER:
            comparable = note.concept(extendedTerm).lastEditedBy().username.toLowerCase();
            break;
        case STATUS:
            comparable = note.concept(extendedTerm).status.ordinal();
            break;
        default:
            comparable = note.lemma.toLowerCase();
            break;
        }
        return searchInfo.isAscending() ? comparable.asc() : comparable.desc();
    }

    @Override
    public List<Note> getOrphans() {
        return getSession().from(note)
                .where(sub(documentNote).where(documentNote.note().eq(note)).notExists())
                .list(note);
    }

    @Override
    public List<String> getOrphanIds() {
        return getSession().from(note)
                .where(sub(documentNote).where(documentNote.note().eq(note)).notExists())
                .list(note.id);
    }


    @Override
    public DocumentNote createDocumentNote(Note n, DocumentRevision docRevision, String longText) {
        return createDocumentNote(n, docRevision, String.valueOf(timeService.currentTimeMillis()),
                longText, 0);
    }

    @Override
    public DocumentNote createDocumentNote(Note n, DocumentRevision docRevision, String localId,
            String longText, int position) {
        return createDocumentNote(new DocumentNote(), n, docRevision, localId,
                longText, position);
    }

    @Override
    public DocumentNote createDocumentNote(DocumentNote documentNote, Note n, DocumentRevision docRevision, String localId,
            String longText, int position) {
        UserInfo createdBy = userRepository.getCurrentUser();

        long currentTime = timeService.currentTimeMillis();
        documentNote.setCreatedOn(currentTime);
        n.setEditedOn(currentTime);

        Concept concept = n.getConcept(extendedTerm);
        if (concept != null) {
            concept.setLastEditedBy(createdBy);
            if (concept.getAllEditors() == null) {
                concept.setAllEditors(new HashSet<UserInfo>());
            }
            concept.getAllEditors().add(createdBy);

        }

        documentNote.setSVNRevision(docRevision.getRevision());
        documentNote.setLongText(longText);

        String createdLemma = Note.createLemmaFromLongText(longText);
        if (n.getLemma() == null) {
            n.setLemma(createdLemma);
        }

        //Extended term version gets the lemma also to basicTerm automatically
        if (extendedTerm && n.getTerm() != null && n.getTerm().getBasicForm() == null) {
            n.getTerm().setBasicForm(createdLemma);
        }
        //And to the short version
        if (extendedTerm && documentNote.getShortText() == null) {
            documentNote.setShortText(createdLemma);
        }

        documentNote.setDocument(docRevision.getDocument());
        documentNote.setDocRevision(docRevision);
        documentNote.setNote(n);
        n.incDocumentNoteCount();
        documentNote.setPosition(position);
        getSession().save(n);
        getSession().save(documentNote);
        getSession().flush();

//        documentNoteRepository.removeOrphans(documentNote.getNote().getId());

        return documentNote;
    }

    @Override
    public Note find(String lemma) {
        return getSession().from(note).where(note.lemma.eq(lemma)).uniqueResult(note);
    }

    private void handleEndElement(XMLStreamReader reader, LoopContext data) {
        String localName = reader.getLocalName();

        if (localName.equals("note")) {
            data.note.getConcept(extendedTerm).setLastEditedBy(userRepository.getCurrentUser());
            data.note.setEditedOn(timeService.currentTimeMillis());
            save(data.note);
            data.counter++;
        } else if (localName.equals("lemma")) {
            data.note.setLemma(data.text);
        } else if (localName.equals("lemma-meaning")) {
            data.note.setLemmaMeaning(data.text);
        } else if (localName.equals("source")) {
            data.note.getConcept(extendedTerm).setSources(data.paragraphs.toString());
            data.paragraphs = null;
        } else if (localName.equals("description")) {
            data.note.getConcept(extendedTerm).setDescription(data.paragraphs.toString());
            data.paragraphs = null;
        } else if (localName.equals("bibliograph")) {
            data.inBib = false;
            data.reference = null;
        } else if (localName.equals("a")) {
            data.inA = false;
            data.url = null;
        }
    }

    private void handleStartElement(XMLStreamReader reader, LoopContext data) {
        String localName = reader.getLocalName();
        if (localName.equals("note")) {
            data.note = new Note();
            if (extendedTerm) {
                data.note.setTerm(new Term());
            }
        } else if (localName.equals("source") || localName.equals("description")) {
            data.paragraphs = new Paragraph();
        }
        if (localName.equals("bibliograph")) {
            data.inBib = true;
            if (reader.getAttributeCount() > 0) {
                data.reference = reader.getAttributeValue(0);
            }
        } else if (localName.equals("a")) {
            data.inA = true;
            if (reader.getAttributeCount() > 0) {
                data.url = reader.getAttributeValue(0);
            }
        }
    }

    @Override
    public int importNotes(File file) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(new FileInputStream(file));
        } catch (XMLStreamException e) {
            throw new ServiceException(e);
        } catch (FileNotFoundException e) {
            throw new ServiceException(e);
        }

        LoopContext data = new LoopContext();

        while (true) {
            int event = -1;
            try {
                event = reader.next();
            } catch (XMLStreamException e) {
                throw new ServiceException(e);
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                handleStartElement(reader, data);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                handleEndElement(reader, data);
            } else if (event == XMLStreamConstants.CHARACTERS) {
                if (data.paragraphs == null) {
                    data.text = reader.getText().replaceAll("\\s+", " ");
                } else {
                    String text = reader.getText().replaceAll("\\s+", " ");
                    if (data.inBib) {
                        LinkElement el = new LinkElement(text);
                        if (data.reference != null) {
                            el.setReference(data.reference);
                        }
                        data.paragraphs.addElement(el);
                    } else if (data.inA) {
                        UrlElement el = new UrlElement(text);
                        if (data.url != null) {
                            el.setUrl(data.url);
                        }
                    } else {
                        data.paragraphs.addElement(new StringElement(text));
                    }

                }
            } else if (event == XMLStreamConstants.END_DOCUMENT) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    throw new ServiceException(e);
                }
                break;
            }
        }
        return data.counter;
    }

    @Override
    public GridDataSource queryDictionary(String searchTerm) {
        Assert.notNull(searchTerm);
        if (!searchTerm.equals("*")) {
            BooleanBuilder builder = new BooleanBuilder();
            builder.or(termWithNotes.basicForm.containsIgnoreCase(searchTerm));
            builder.or(termWithNotes.meaning.containsIgnoreCase(searchTerm));
            return createGridDataSource(termWithNotes, termWithNotes.basicForm.lower().asc(),
                    false, builder.getValue());
        }
        return createGridDataSource(termWithNotes, termWithNotes.basicForm.lower().asc(), false);
    }

    @Override
    public GridDataSource queryPersons(String searchTerm) {
        Assert.notNull(searchTerm);
        QPerson person = QPerson.person;
        if (!searchTerm.equals("*")) {
            BooleanBuilder builder = new BooleanBuilder();
            builder.or(person.normalizedForm().first.containsIgnoreCase(searchTerm));
            builder.or(person.normalizedForm().last.containsIgnoreCase(searchTerm));
            return createGridDataSource(person, person.normalizedForm().last.lower().asc(), false,
                    builder.getValue());
        }
        return createGridDataSource(person, person.normalizedForm().last.asc(), false);
    }

    @Override
    public GridDataSource queryPlaces(String searchTerm) {
        Assert.notNull(searchTerm);
        QPlace place = QPlace.place;
        if (!searchTerm.equals("*")) {
            BooleanBuilder builder = new BooleanBuilder();
            builder.or(place.normalizedForm().last.containsIgnoreCase(searchTerm));
            return createGridDataSource(place, place.normalizedForm().last.lower().asc(), false,
                    builder.getValue());
        }
        return createGridDataSource(place, place.normalizedForm().last.asc(), false);
    }

    @Override
    public void remove(DocumentNote documentNoteToBeRemoved, long revision) {
        Assert.notNull(documentNoteToBeRemoved, "note was null");

        documentNoteToBeRemoved.getNote().decDocumentNoteCount();

        documentNoteToBeRemoved.setSVNRevision(revision);
        documentNoteToBeRemoved.setDeleted(true);

        getSession().save(documentNoteToBeRemoved);

    }

    @Override
    public void removePermanently(DocumentNote note) {
        Note n = note.getNote();
        getSession().delete(note);
        n.decDocumentNoteCount();
        save(n);
    }

    @Override
    public NoteComment removeComment(String commentId) {
        NoteComment comment = getSession().getById(commentId, NoteComment.class);
        getSession().delete(comment);
        return comment;
    }

    @Override
    public List<Note> findNotes(String lemma) {
        return getSession().from(note).where(note.lemma.eq(lemma)).list(note);
    }

    private BeanSubQuery sub(EntityPath<?> entity) {
        return new BeanSubQuery().from(entity);
    }

    @Override
    public void save(Note note) {
        getSession().save(note);
    }

    @Override
    public void removeNote(Note note) {
        getSession().delete(note);
    }

    @Override
    public void removeNotes(Collection<Note> notes) {
        for (Note note : notes){
            removeNote(note);
        }
    }

    private void logDuration(String method, long start) {
        long duration = System.currentTimeMillis()-start;
        if (duration > 500){
            logger.warn(method + " took " + duration+"ms");
        }
    }

}
