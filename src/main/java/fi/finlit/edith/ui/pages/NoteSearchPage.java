/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.pages;


import org.apache.tapestry5.Asset;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.RenderSupport;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.IncludeJavaScriptLibrary;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.mysema.tapestry.core.Context;

import fi.finlit.edith.domain.DocumentNote;
import fi.finlit.edith.domain.DocumentRepository;
import fi.finlit.edith.domain.DocumentNote;
import fi.finlit.edith.domain.DocumentNoteRepository;
import fi.finlit.edith.ui.services.PrimaryKeyEncoder;

/**
 * NoteSearch provides
 *
 * @author tiwe
 * @version $Id$
 */
//FIXME Use dto instead of NoteRevision to get the editing ability right
@SuppressWarnings("unused")
@IncludeJavaScriptLibrary( { "classpath:jquery-1.4.1.js", "NoteSearchPage.js" })
public class NoteSearchPage {

    @Property
    private String searchTerm;

    @Property
    private String editMode;

    private Context context;

    @Property
    private GridDataSource notes;

    @Property
    private DocumentNote note;

    @Inject
    private DocumentNoteRepository noteRevisionRepo;

    @Inject
    private DocumentRepository documentRepository;

    @Property
    private PrimaryKeyEncoder<DocumentNote> encoder;

    @Inject
    @Path("NoteSearchPage.css")
    private Asset stylesheet;

    @Environmental
    private RenderSupport support;

    @AfterRender
    void addStylesheet() {
        // This is needed to have the page specific style sheet after
        // other css includes
        support.addStylesheetLink(stylesheet, null);
    }

    void onActionFromCancel() {
        context = new Context(searchTerm);
    }

    void onActionFromDelete(String noteRevisionId) {
//        noteRevisionRepo.remove(noteRevisionId);
        DocumentNote noteRevision = noteRevisionRepo.getById(noteRevisionId);
        documentRepository.removeNotes(noteRevision.getDocumentRevision(), noteRevision);
    }

    void onActionFromToggleEdit() {
        context = new Context(searchTerm, "edit");
    }

    void onActivate(EventContext ctx) {
        if (ctx.getCount() >= 1) {
            searchTerm = ctx.get(String.class, 0);
        }
        if (ctx.getCount() >= 2) {
            editMode = ctx.get(String.class, 1);
        }
        context = new Context(ctx);
    }

    Object onPassivate() {
        return context == null ? null : context.toArray();
    }

    void onPrepare() {
        encoder = new PrimaryKeyEncoder<DocumentNote>(noteRevisionRepo);
    }

    void onSuccessFromEdit() {
        // TODO Validations
        noteRevisionRepo.saveAll(encoder.getAllValues());
        context = new Context(searchTerm);
    }

    void onSuccessFromSearch() {
        context = new Context(searchTerm);
    }

    void setupRender() {
        notes = noteRevisionRepo.queryNotes(searchTerm == null ? "*" : searchTerm);
    }

}
