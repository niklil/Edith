/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import static fi.finlit.edith.domain.QDocumentNote.documentNote;

import java.util.Arrays;
import java.util.List;

import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.util.Assert;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.expr.EBoolean;
import com.mysema.query.types.path.PEntity;
import com.mysema.query.types.path.PString;
import com.mysema.rdfbean.dao.AbstractRepository;
import com.mysema.rdfbean.object.BeanSubQuery;
import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SessionFactory;

import fi.finlit.edith.domain.DocumentNote;
import fi.finlit.edith.domain.DocumentNoteRepository;
import fi.finlit.edith.domain.DocumentRevision;
import fi.finlit.edith.domain.QDocumentNote;
import fi.finlit.edith.domain.QNote;
import fi.finlit.edith.domain.UserInfo;
import fi.finlit.edith.domain.UserRepository;

/**
 * NoteRepositoryImpl provides
 *
 * @author tiwe
 * @version $Id$
 */
public class DocumentNoteRepositoryImpl extends AbstractRepository<DocumentNote> implements
        DocumentNoteRepository {

    // FIXME
    private static final QDocumentNote otherNote = new QDocumentNote("other");

    private final TimeService timeService;

    private final UserRepository userRepository;

    public DocumentNoteRepositoryImpl(@Inject SessionFactory sessionFactory,
            @Inject UserRepository userRepository, @Inject TimeService timeService) {
        super(sessionFactory, documentNote);
        // FIXME
        // super(sessionFactory, null);
        this.userRepository = userRepository;
        this.timeService = timeService;
    }

    @Override
    public DocumentNote getByLocalId(DocumentRevision docRevision, String localId) {
        Assert.notNull(docRevision);
        Assert.notNull(localId);
        return getSession().from(documentNote).where(
                documentNote.document().eq(docRevision.getDocument()),
                documentNote.localId.eq(localId),
                documentNote.svnRevision.loe(docRevision.getRevision()),
                documentNote.deleted.eq(false), latestFor(docRevision.getRevision())).uniqueResult(
                documentNote);
    }

    @Override
    public List<DocumentNote> getOfDocument(DocumentRevision docRevision) {
        Assert.notNull(docRevision);
        return getSession().from(documentNote).where(
                documentNote.document().eq(docRevision.getDocument()),
                documentNote.svnRevision.loe(docRevision.getRevision()),
                documentNote.deleted.eq(false), latestFor(docRevision.getRevision())).orderBy(
                documentNote.createdOn.asc()).list(documentNote);
    }

    private EBoolean latestFor(long svnRevision) {
        return sub(otherNote).where(
                otherNote.ne(documentNote),
                otherNote.note().eq(documentNote.note()),
                otherNote.svnRevision.loe(svnRevision),
                otherNote.createdOn.gt(documentNote.createdOn)
                ).notExists();
    }

    @Override
    public GridDataSource queryNotes(String searchTerm) {
        QNote note = documentNote.note();
        Assert.notNull(searchTerm);
        BooleanBuilder builder = new BooleanBuilder();
        // FIXME
        if (!searchTerm.equals("*")) {
            for (PString path : Arrays.asList(note.lemma, documentNote.longText,
                    note.term().basicForm, note.term().meaning,
                    // documentNote.description, FIXME
                    note.subtextSources)) {
                builder.or(path.contains(searchTerm, false));
            }
        }
        // FIXME
        // builder.and(documentNote.eq(documentNote.note.latestRevision));
        builder.and(documentNote.deleted.eq(false));
        return createGridDataSource(documentNote, note.term().basicForm.lower().asc(), false,
                builder.getValue());
    }

    @Override
    public void remove(DocumentNote note) {
        Assert.notNull(note, "note was null");
        DocumentNote deleted = note.createCopy();

        deleted.setDeleted(true);
        // FIXME
        // deleted.getRevisionOf().setLatestRevision(deleted);
        deleted.setCreatedBy(userRepository.getCurrentUser());

        getSession().save(deleted);
        // FIXME
        // getSession().save(deleted.getRevisionOf());
    }

    @Override
    public void remove(String noteRevisionId) {
        DocumentNote note = super.getById(noteRevisionId);
        remove(note);
    }

    @Override
    public DocumentNote save(DocumentNote documentNote) {
        UserInfo createdBy = userRepository.getCurrentUser();
        documentNote.setCreatedOn(timeService.currentTimeMillis());
        documentNote.setCreatedBy(createdBy);
        // FIXME
        // note.getRevisionOf().setLatestRevision(note);
        getSession().save(documentNote.getNote());
        getSession().save(documentNote);
        // FIXME
        // getSession().save(note.getRevisionOf());
        return documentNote;
    }

    private BeanSubQuery sub(PEntity<?> entity) {
        return new BeanSubQuery().from(entity);
    }

}
