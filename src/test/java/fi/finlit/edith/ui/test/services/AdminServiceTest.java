/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.test.services;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.junit.After;
import org.junit.Test;

import fi.finlit.edith.domain.NoteRepository;
import fi.finlit.edith.domain.DocumentNoteRepository;
import fi.finlit.edith.ui.services.AdminService;

/**
 * AdminServiceTest provides
 *
 * @author tiwe
 * @version $Id$
 */
public class AdminServiceTest extends AbstractServiceTest{

    @Inject
    private NoteRepository noteRepo;

    @Inject
    private DocumentNoteRepository noteRevisionRepo;

    @Inject
    private AdminService adminService;

    @Inject @Symbol(ServiceTestModule.NOTE_TEST_DATA_KEY)
    private File noteTestData;

    @After
    public void tearDown(){
        System.out.println();
    }

    @Test
    public void removeNotes() throws Exception {
        noteRepo.importNotes(noteTestData);
        assertTrue(noteRevisionRepo.queryNotes("*").getAvailableRows() > 0);

        adminService.removeNotes();
        assertTrue(noteRevisionRepo.queryNotes("*").getAvailableRows() == 0);
    }

    @Test
    public void removeNotesAndTerms() throws Exception {
        noteRepo.importNotes(noteTestData);
        assertTrue(noteRevisionRepo.queryNotes("*").getAvailableRows() > 0);

        adminService.removeNotesAndTerms();
        assertTrue(noteRevisionRepo.queryNotes("*").getAvailableRows() == 0);
    }

    @Override
    protected Class<?> getServiceClass() {
        return AdminService.class;
    }

}
