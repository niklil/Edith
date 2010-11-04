/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import static fi.finlit.edith.domain.QDocumentNote.documentNote;
import static fi.finlit.edith.domain.QNote.note;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.util.Assert;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.ComparableExpressionBase;
import com.mysema.query.types.path.StringPath;
import com.mysema.rdfbean.object.BeanSubQuery;
import com.mysema.rdfbean.object.SessionFactory;

import fi.finlit.edith.domain.*;
import fi.finlit.edith.ui.services.svn.SubversionService;

/**
 * NoteRepositoryImpl provides
 *
 * @author tiwe
 * @version $Id$
 */
public class DocumentNoteRepositoryImpl extends AbstractRepository<DocumentNote> implements
        DocumentNoteRepository {

    private static final QDocumentNote otherNote = new QDocumentNote("other");

    private final TimeService timeService;

    private final UserRepository userRepository;

    private final SubversionService subversionService;

    public DocumentNoteRepositoryImpl(@Inject SessionFactory sessionFactory,
            @Inject UserRepository userRepository, @Inject TimeService timeService,
            @Inject SubversionService subversionService) {
        super(sessionFactory, documentNote);
        this.userRepository = userRepository;
        this.timeService = timeService;
        this.subversionService = subversionService;
    }

    @Override
    public DocumentNote getByLocalId(DocumentRevision docRevision, String localId) {
        Assert.notNull(docRevision);
        Assert.notNull(localId);
        return getSession()
                .from(documentNote)
                .where(documentNote.document().eq(docRevision.getDocument()),
                        documentNote.localId.eq(localId),
                        documentNote.svnRevision.loe(docRevision.getRevision()),
                        documentNote.deleted.eq(false),
                        latestFor(documentNote, docRevision.getRevision()))
                .uniqueResult(documentNote);
    }

    @Override
    public List<DocumentNote> getOfDocument(DocumentRevision docRevision) {
        Assert.notNull(docRevision);
        return getSession()
                .from(documentNote)
                .where(documentNote.document().eq(docRevision.getDocument()),
                        documentNote.svnRevision.loe(docRevision.getRevision()),
                        documentNote.deleted.not(),
                        latestFor(documentNote, docRevision.getRevision()))
                .orderBy(documentNote.createdOn.asc()).list(documentNote);
    }

    @Override
    public List<DocumentNote> getPublishableNotesOfDocument(DocumentRevision docRevision) {
        List<DocumentNote> result = new ArrayList<DocumentNote>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(subversionService.getStream(
                    docRevision.getSvnPath(), docRevision.getRevision()));
        } catch (XMLStreamException e) {
            throw new ServiceException(e);
        } catch (FileNotFoundException e) {
            throw new ServiceException(e);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        while (true) {
            int event = -1;
            try {
                event = reader.next();
            } catch (XMLStreamException e) {
                throw new ServiceException(e);
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("anchor")
                        && reader.getAttributeValue(0).startsWith("start")) {
                    String attr = reader.getAttributeValue(0).replace("start", "");
                    DocumentNote current = getByLocalId(docRevision, attr);
                    if (current != null && current.isPublishable()) {
                        result.add(current);
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

        return result;
    }

    private BooleanExpression latestFor(QDocumentNote docNote, long svnRevision) {
        return sub(otherNote).where(otherNote.ne(docNote),
                // otherNote.note().eq(documentNote.note()),
                otherNote.localId.eq(docNote.localId), otherNote.svnRevision.loe(svnRevision),
                otherNote.createdOn.gt(docNote.createdOn)).notExists();
    }

    private BooleanExpression latest(QDocumentNote docNote) {
        return sub(otherNote).where(otherNote.ne(docNote), otherNote.localId.eq(docNote.localId),
                otherNote.note().eq(docNote.note()), otherNote.createdOn.gt(docNote.createdOn))
                .notExists();
    }

    @Override
    public GridDataSource queryNotes(String searchTerm) {
        QNote note = documentNote.note();
        Assert.notNull(searchTerm);
        BooleanBuilder builder = new BooleanBuilder();
        if (!searchTerm.equals("*")) {
            for (StringPath path : Arrays.asList(note.lemma, documentNote.longText,
                    note.term().basicForm, note.term().meaning)) {
                // ,
                // documentNote.description, FIXME
                // note.subtextSources)
                builder.or(path.containsIgnoreCase(searchTerm));
            }
        }
        builder.and(documentNote.deleted.eq(false));
        builder.and(latest(documentNote));

        return createGridDataSource(documentNote, note.term().basicForm.lower().asc(), false,
                builder.getValue());
    }

    @Override
    public void remove(DocumentNote docNote) {
        Assert.notNull(docNote, "note was null");
        // XXX What was the point in having .createCopy?
        DocumentNote deleted = docNote;
        deleted.setDeleted(true);
        // deleted.setCreatedBy(userRepository.getCurrentUser());
        getSession().save(deleted);
    }

    @Override
    public void remove(String documentNoteId) {
        DocumentNote note = super.getById(documentNoteId);
        remove(note);
    }

    @Override
    public DocumentNote save(DocumentNote docNote) {
        if (docNote.getNote() == null) {
            throw new ServiceException("Note was null for " + docNote);
        }
        UserInfo createdBy = userRepository.getCurrentUser();
        long currentTime = timeService.currentTimeMillis();
        docNote.setCreatedOn(currentTime);
        docNote.getNote().setEditedOn(currentTime);
        docNote.getNote().setLastEditedBy(createdBy);
        if (docNote.getNote().getAllEditors() == null) {
            docNote.getNote().setAllEditors(new HashSet<UserInfo>());
        }
        docNote.getNote().getAllEditors().add(createdBy);
        getSession().save(docNote);
        if (docNote.getNote().getComments() != null) {
            for (NoteComment comment : docNote.getNote().getComments()) {
                getSession().save(comment);
            }
        }

        return docNote;
    }

    @Override
    public DocumentNote saveAsCopy(DocumentNote docNote) {
        if (docNote.getNote() == null) {
            throw new ServiceException("Note was null for " + docNote);
        }
        docNote.setNote(copy(docNote.getNote()));
        return save(docNote);
    }

    // TODO This doesn't belong here. Though getSession() does :/
    private Note copy(Note note) {
        Note copy = new Note();
        Set<NoteComment> comments = new HashSet<NoteComment>();
        for (NoteComment comment : note.getComments()) {
            NoteComment copyOfComment = comment.copy();
            copyOfComment.setNote(copy);
            comments.add(copyOfComment);
            // getSession().save(copyOfComment);
        }
        copy.setComments(comments);
        if (note.getDescription() != null) {
            copy.setDescription(note.getDescription().copy());
        }
        copy.setFormat(note.getFormat());
        copy.setLemma(note.getLemma());
        copy.setLemmaMeaning(note.getLemmaMeaning());
        copy.setPerson(note.getPerson());
        copy.setPlace(note.getPlace());
        if (note.getSources() != null) {
            copy.setSources(note.getSources().copy());
        }
        copy.setSubtextSources(note.getSubtextSources());
        copy.setTerm(note.getTerm());
        copy.setTypes(note.getTypes());
        return copy;
    }

    private BeanSubQuery sub(EntityPath<?> entity) {
        return new BeanSubQuery().from(entity);
    }

    @Override
    public Notes query(DocumentNoteSearchInfo searchInfo) {
        Assert.notNull(searchInfo);
        BooleanBuilder filters = new BooleanBuilder();
        filters.and(documentNote.deleted.eq(false));
        // document & orphans
        BooleanBuilder documentAndOrphanFilter = null;
        if (!searchInfo.getDocuments().isEmpty()) {
            filters.and(documentNote.document().in(searchInfo.getDocuments()));
        }
        // creators
        if (!searchInfo.getCreators().isEmpty()) {
            BooleanBuilder filter = new BooleanBuilder();
            Collection<String> usernames = new ArrayList<String>(searchInfo.getCreators().size());
            for (UserInfo userInfo : searchInfo.getCreators()) {
                filter.or(documentNote.note().allEditors.contains(userRepository
                        .getUserInfoByUsername(userInfo.getUsername())));
                usernames.add(userInfo.getUsername());
            }
            // FIXME This is kind of useless except that we have broken data in production.
            filter.or(documentNote.note().lastEditedBy().username.in(usernames));
            filters.and(filter);
        }
        // formats
        if (!searchInfo.getNoteFormats().isEmpty()) {
            filters.and(documentNote.note().format.in(searchInfo.getNoteFormats()));
        }
        // types
        if (!searchInfo.getNoteTypes().isEmpty()) {
            BooleanBuilder filter = new BooleanBuilder();
            for (NoteType type : searchInfo.getNoteTypes()) {
                filter.or(documentNote.note().types.contains(type));
            }
            filters.and(filter);
        }
        filters.and(sub(otherNote).where(otherNote.ne(documentNote),
                otherNote.note().eq(documentNote.note()),
                otherNote.localId.eq(documentNote.localId),
                otherNote.createdOn.gt(documentNote.createdOn)).notExists());

        return new Notes(searchInfo.isOrphans() ? getOrphans() : null, getSession()
                .from(documentNote).where(filters).orderBy(getOrderBy(searchInfo))
                .list(documentNote));
    }

    @Override
    public List<Note> getOrphans() {
        return getSession().from(note)
                .where(sub(documentNote).where(documentNote.note().eq(note)).notExists())
                .list(note);
    }

    private OrderSpecifier<?> getOrderBy(DocumentNoteSearchInfo searchInfo) {
        ComparableExpressionBase<?> comparable = null;
        switch (searchInfo.getOrderBy()) {
        case DATE:
            comparable = documentNote.createdOn;
            break;
        case USER:
            comparable = documentNote.note().lastEditedBy().username.toLowerCase();
            break;
        case STATUS:
            comparable = documentNote.note().status.ordinal();
            break;
        default:
            comparable = documentNote.note().lemma.toLowerCase();
            break;
        }
        return searchInfo.isAscending() ? comparable.asc() : comparable.desc();
    }

    @Override
    public List<DocumentNote> getOfNote(String noteId) {
        Assert.notNull(noteId);
        return getSession()
                .from(documentNote)
                .where(documentNote.note().id.eq(noteId), documentNote.deleted.eq(false),
                        latest(documentNote)).list(documentNote);
    }

    @Override
    public List<DocumentNote> getOfNoteInDocument(String noteId, String documentId) {
        Assert.notNull(noteId);
        Assert.notNull(documentId);
        return getSession()
                .from(documentNote)
                .where(documentNote.note().id.eq(noteId),
                        documentNote.document().id.eq(documentId), documentNote.deleted.eq(false),
                        latest(documentNote)).list(documentNote);
    }

    @Override
    public List<DocumentNote> getOfTerm(String termId) {
        Assert.notNull(termId);
        return getSession()
                .from(documentNote)
                .where(documentNote.note().term().id.eq(termId), documentNote.deleted.eq(false),
                        latest(documentNote)).list(documentNote);
    }

    @Override
    public List<DocumentNote> getOfPerson(String personId) {
        Assert.notNull(personId);
        return getSession()
                .from(documentNote)
                .where(documentNote.note().person().id.eq(personId),
                        documentNote.deleted.eq(false), latest(documentNote)).list(documentNote);
    }

    @Override
    public List<DocumentNote> getOfPlace(String placeId) {
        Assert.notNull(placeId);
        return getSession()
                .from(documentNote)
                .where(documentNote.note().place().id.eq(placeId), documentNote.deleted.eq(false),
                        latest(documentNote)).list(documentNote);
    }

    @Override
    public List<DocumentNote> getNotesLessDocumentNotes() {
        return getSession().from(documentNote).where(documentNote.note().isNull()).list(documentNote);
    }
}
