/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.pages.document;

import java.util.List;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.ajax.MultiZoneUpdate;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.finlit.edith.EDITH;
import fi.finlit.edith.dto.NoteSearchInfo;
import fi.finlit.edith.dto.SelectedText;
import fi.finlit.edith.sql.domain.DocumentNote;
import fi.finlit.edith.sql.domain.Note;
import fi.finlit.edith.sql.domain.NoteType;
import fi.finlit.edith.sql.domain.Term;
import fi.finlit.edith.ui.components.InfoMessage;
import fi.finlit.edith.ui.components.note.DocumentNotes;
import fi.finlit.edith.ui.components.note.NoteEdit;
import fi.finlit.edith.ui.components.note.SearchResults;
import fi.finlit.edith.ui.pages.Documents;
import fi.finlit.edith.ui.services.DocumentNoteDao;
import fi.finlit.edith.ui.services.NoteDao;

@Import(library = {
        "classpath:js/jquery-1.5.1.min.js", "classpath:js/TapestryExt.js",
        "classpath:js/jquery-ui-1.8.12.custom.min.js", "classpath:js/jquery.dynatree.min.js",
        "TextSelector.js", "Annotate.js", "classpath:js/jqModal.js",
        "classpath:js/jquery.cookie.js",
        "context:js/ckeditor/ckeditor.js", "context:js/ckeditor/adapters/jquery.js"
        },
        stylesheet= {"context:styles/tei.css",
        "context:styles/smoothness/jquery-ui-1.8.12.custom.css",
        "context:styles/dynatree/skin/ui.dynatree.css", "Annotate.css"})
@SuppressWarnings("unused")
public class Annotate extends AbstractDocumentPage {

    private static final String EDIT_ZONE = "editZone";

    private static final Logger logger = LoggerFactory.getLogger(Annotate.class);

    @Property
    @Persist
    private SelectedText createTermSelection;

    @Inject
    private DocumentNoteDao documentNoteRepository;

    @Inject
    private Block documentView;

    @Inject
    private Block emptyBlock;

    @Inject
    @Property
    private Block closeDialog;

    @InjectComponent
    private InfoMessage infoMessage;

    @Inject
    private Messages messages;

    @Property
    private boolean moreThanOneSelectable;


    @Property
    private DocumentNote note;

    @Inject
    private NoteDao noteRepository;

    @Property
    private String noteRevisionId;

    @Property
    private String selectedNoteId;

    @Inject
    @Property
    private Block notesForLemma;

    @Inject
    private JavaScriptSupport renderSupport;

    @Inject
    private ComponentResources resources;

    @Persist
    private NoteSearchInfo searchInfo;

    @Property
    private NoteType type;

    @Property
    private List<Note> notes;

    @Property
    private String personId;

    @Property
    private Long noteToLinkId;

    @Inject
    @Symbol(EDITH.EXTENDED_TERM)
    private boolean slsMode;

    @InjectComponent
    private SearchResults searchResults;

    @InjectComponent
    private DocumentNotes documentNotes;

    @InjectComponent
    private NoteEdit noteEdit;

    @Inject
    private PageRenderLinkSource linkSource;

    @AfterRender
    void addScript() {
        String link = resources.createEventLink("edit", "CONTEXT").toAbsoluteURI();
        renderSupport.addScript("editLink = '" + link + "';");
    }

    public NoteSearchInfo getSearchInfo() {
        if (searchInfo == null) {
            searchInfo = new NoteSearchInfo();
            searchInfo.getDocuments().add(getDocument());
            searchInfo.setCurrentDocument(getDocument());
        }
        return searchInfo;
    }

    void setupRender() {
        searchInfo = null;
    }

    void onActivate() {
        if (createTermSelection == null) {
            createTermSelection = new SelectedText();
        }
    }

    Object onSuccessFromSelectNoteForm() {
        // FIXME: If an area which has multiple notes is clicked the url will
        // be in the following form n100/12/13. With this we will handle this
        // case and just return the id 100.
        int firstSlash = selectedNoteId.indexOf('/');
        //Strip first n from the id
        long id = Long.parseLong(firstSlash == -1 ? selectedNoteId.substring(1) : selectedNoteId.substring(1, firstSlash));
        DocumentNote documentNote = documentNoteRepository.getById(id);
        documentNotes.setNoteId(documentNote.getNote().getId());
        documentNotes.setSelectedNote(documentNote);
        noteEdit.setDocumentNoteOnEdit(documentNote);
        return new MultiZoneUpdate("documentNotesZone", documentNotes.getBlock())
            .add("noteEditZone", noteEdit.getBlock());

    }

    Object onDelete(EventContext context) {
        return new MultiZoneUpdate(EDIT_ZONE, emptyBlock)
                .add("documentZone", documentView).add("commentZone", emptyBlock);
    }

    Object onEdit(EventContext context) {
        return new MultiZoneUpdate(EDIT_ZONE, noteEdit);
    }

    private MultiZoneUpdate noteHasChanged(DocumentNote documentNote, String msg) {
        documentNotes.setNoteId(documentNote.getNote().getId());
        documentNotes.setSelectedNote(documentNote);
        noteEdit.setDocumentNoteOnEdit(documentNote);
        infoMessage.addInfoMsg(msg);
        return zoneWithInfo(msg)
                .add("listZone", searchResults.getBlock())
                .add("documentNotesZone", documentNotes.getBlock())
                .add("noteEditZone", noteEdit.getBlock()).add("documentZone", documentView);
    }

    Object onSuccessFromConnectTermForm() {
        logger.info("connect term with note id " + noteToLinkId);

        try {
            Note n = noteRepository.getById(noteToLinkId);
            DocumentNote documentNote = noteRepository.createDocumentNote(n,
                    getDocument(), createTermSelection.getSelection());
            documentNote = getDocumentRepository().updateNote(documentNote, createTermSelection);

            return noteHasChanged(documentNote, "note-connect-success");
        } catch (Exception e) {
            return zoneWithError("note-connect-failed", e);
        }
    }

    Object onSuccessFromCreateTermForm() {
        logger.info(createTermSelection.toString());
        DocumentNote documentNote = null;
        try {
            Note n = createNote();
            documentNote = getDocumentRepository().addNote(n, getDocument(), createTermSelection);
        } catch (Exception e) {
            return zoneWithError("note-addition-failed", e);
        }

        return noteHasChanged(documentNote, "create-success");
    }

    private Note createNote() {
        Note n = new Note();
        if (slsMode) {
            n.setTerm(new Term());
        }
        return n;
}


    public void setSearchInfo(NoteSearchInfo searchInfo) {
        this.searchInfo = searchInfo;
    }

    Object onChooseBackingNote() {
        return handleUserChoice(null);
    }

    Object onChooseBackingNote(String noteId) {
        return handleUserChoice(noteId);
    }

    private Object handleUserChoice(String noteId) {
        return new MultiZoneUpdate(EDIT_ZONE, noteEdit).add("dialogZone", closeDialog);
    }

    public boolean isSlsMode() {
        return slsMode;
    }

    public NoteEdit getNoteEdit() {
        return noteEdit;
    }

    public InfoMessage getInfoMessage() {
        return infoMessage;
    }

    public SearchResults getSearchResults() {
        return searchResults;
    }

    public DocumentNotes getDocumentNotes() {
        return documentNotes;
    }

    public Block getDocumentView() {
        return documentView;
    }

    public MultiZoneUpdate zoneWithInfo(String msg) {
        getInfoMessage().addInfoMsg(msg);
        return new MultiZoneUpdate("infoMessageZone", getInfoMessage().getBlock());
    }

    public MultiZoneUpdate zoneWithError(String msg, Throwable e) {
        logger.error(msg, e);
        getInfoMessage().addErrorMsg(msg);
        return new MultiZoneUpdate("infoMessageZone", getInfoMessage().getBlock());
    }

    public String getDocumentsAjaxURL() {
        return linkSource.createPageRenderLink(Documents.class).toString();
    }
}
