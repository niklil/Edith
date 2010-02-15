/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.pages;

import java.io.File;
import java.util.List;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.tmatesoft.svn.core.SVNException;

import fi.finlit.edith.domain.Document;
import fi.finlit.edith.domain.DocumentRepository;
import fi.finlit.edith.domain.DocumentRevision;
import fi.finlit.edith.domain.NoteRepository;
import fi.finlit.edith.ui.services.AdminService;
import fi.finlit.edith.ui.services.RevisionInfo;

/**
 * TestDataPage provides
 *
 * @author tiwe
 * @version $Id$
 */
// TODO : move test data creation to test data service
public class DummyDataPage {

    static final String TEST_DOCUMENT_SVNPATH = "/documents/trunk/Nummisuutarit rakenteistettuna-annotoituna.xml";

    static final String TEST_NOTES_FILEPATH = "etc/demo-material/notes/nootit.xml";

    @Inject
    private NoteRepository noteRepository;

    @Inject
    private DocumentRepository documentRepository;

    @Inject
    private AdminService adminService;

    void onActivate(){
    }

    void onAddNoteSearchTestData() throws Exception{
        noteRepository.importNotes(new File(TEST_NOTES_FILEPATH));
    }

    void onAddAnnotateTestData() throws SVNException{
        Document document = documentRepository.getDocumentForPath(TEST_DOCUMENT_SVNPATH);
        List<RevisionInfo> revisions = documentRepository.getRevisions(document);
        long latestRevision = revisions.get(revisions.size() - 1).getSvnRevision();

        DocumentRevision docRev = document.getRevision(latestRevision);
        noteRepository.createNote(docRev, "1", "l\u00E4htee h\u00E4ihins\u00E4 Mikko Vilkastuksen");
        noteRepository.createNote(docRev, "2", "koska suutarille k\u00E4skyn k\u00E4r\u00E4jiin annoit, saadaksesi naimalupaa.");
        noteRepository.createNote(docRev, "3", "tulee, niin seisoo s\u00E4\u00E4t\u00F6s-kirjassa.");
        noteRepository.createNote(docRev, "4", "kummallenkin m\u00E4\u00E4r\u00E4tty, niin emmep\u00E4 tiet\u00E4isi t\u00E4ss\u00E4");
    }

    void onRemoveNotes(){
        adminService.removeNotesAndTerms();
    }

}
