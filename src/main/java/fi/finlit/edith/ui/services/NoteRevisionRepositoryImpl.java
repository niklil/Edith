/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package fi.finlit.edith.ui.services;

import static fi.finlit.edith.domain.QNoteRevision.noteRevision;
import static fi.finlit.edith.domain.QUserInfo.userInfo;

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

import fi.finlit.edith.domain.Document;
import fi.finlit.edith.domain.NoteRevision;
import fi.finlit.edith.domain.NoteRevisionRepository;
import fi.finlit.edith.domain.QNoteRevision;
import fi.finlit.edith.domain.UserInfo;

/**
 * NoteRepositoryImpl provides
 *
 * @author tiwe
 * @version $Id$
 */
public class NoteRevisionRepositoryImpl extends AbstractRepository<NoteRevision> implements NoteRevisionRepository {
    
    private static final QNoteRevision otherNote = new QNoteRevision("other");
    
    @Inject
    private AuthService authService;
    
    public NoteRevisionRepositoryImpl() {
        super(noteRevision);
    }
        
    @Override
    public GridDataSource queryNotes(String searchTerm) {
        Assert.notNull(searchTerm);        
        BooleanBuilder builder = new BooleanBuilder();        
        if (!searchTerm.equals("*")){
            for (PString path : Arrays.asList(
                    noteRevision.lemma, 
                    noteRevision.longText,
                    noteRevision.basicForm,
                    noteRevision.revisionOf.term.meaning,
                    noteRevision.description
                    )){
                builder.or(path.contains(searchTerm, false));
            }    
        }        
        builder.and(noteRevision.eq(noteRevision.revisionOf.latestRevision));
        return createGridDataSource(noteRevision, noteRevision.basicForm.asc(), builder.getValue());
    }

    @Override
    public NoteRevision getByLocalId(Document document, long revision, String localId) {
        Assert.notNull(document);
        Assert.notNull(localId);
        return getSession().from(noteRevision)
            .where(noteRevision.revisionOf.document.eq(document),
                   noteRevision.revisionOf.localId.eq(localId),
                   noteRevision.svnRevision.loe(revision),            
                   latestFor(revision))
            .uniqueResult(noteRevision);
    }
    
    @Override
    public List<NoteRevision> getOfDocument(Document document, long revision) {
        Assert.notNull(document);
        return getSession().from(noteRevision)
            .where(noteRevision.revisionOf.document.eq(document),
                   noteRevision.svnRevision.loe(revision),
                   latestFor(revision))
            .list(noteRevision);
    }

    @Override
    public NoteRevision save(NoteRevision note) {
        UserInfo createdBy = getSession().from(userInfo)
            .where(userInfo.username.eq(authService.getUsername()))
            .uniqueResult(userInfo);  
        note.setCreatedOn(System.currentTimeMillis());
        note.setCreatedBy(createdBy);
        note.getRevisionOf().setLatestRevision(note);
        return super.save(note);
    }
    
    private BeanSubQuery sub(PEntity<?> entity){
        return new BeanSubQuery().from(entity);
    }
    

    private EBoolean latestFor(long svnRevision){
        return sub(otherNote).where(
            otherNote.ne(noteRevision),
            otherNote.revisionOf.eq(noteRevision.revisionOf),
            otherNote.svnRevision.loe(svnRevision),
            otherNote.createdOn.gt(noteRevision.createdOn)).notExists();
    }
    
}