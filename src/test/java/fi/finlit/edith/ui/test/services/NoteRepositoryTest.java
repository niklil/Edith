/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.test.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.junit.Before;
import org.junit.Test;

import fi.finlit.edith.domain.Document;
import fi.finlit.edith.domain.DocumentNote;
import fi.finlit.edith.domain.DocumentNoteRepository;
import fi.finlit.edith.domain.DocumentNoteSearchInfo;
import fi.finlit.edith.domain.DocumentRepository;
import fi.finlit.edith.domain.Note;
import fi.finlit.edith.domain.NoteComment;
import fi.finlit.edith.domain.NoteRepository;
import fi.finlit.edith.ui.services.AdminService;
import fi.finlit.edith.ui.services.svn.RevisionInfo;

/**
 * NoteRepositoryTest provides
 *
 * @author tiwe
 * @version $Id$
 */
public class NoteRepositoryTest extends AbstractServiceTest {

    @Inject
    private AdminService adminService;

    @Inject
    private NoteRepository noteRepository;

    @Inject
    private DocumentRepository documentRepository;

    @Inject
    private DocumentNoteRepository documentNoteRepository;

    @Inject
    @Symbol(ServiceTestModule.NOTE_TEST_DATA_KEY)
    private File noteTestData;

    @Inject
    @Symbol(ServiceTestModule.TEST_DOCUMENT_KEY)
    private String testDocument;

    @Test
    public void createComment() {
        Note note = new Note();
        noteRepository.save(note);
        NoteComment comment = noteRepository.createComment(note, "boomboomboom");
        Collection<NoteComment> comments = noteRepository.getById(note.getId()).getComments();
        assertEquals(1, comments.size());
        assertEquals(comment.getMessage(), comments.iterator().next().getMessage());
    }

    @Test
    public void createLemmaForLongText() {
        assertEquals("word", Note.createLemmaFromLongText("word"));
        assertEquals("word1 word2", Note.createLemmaFromLongText("word1 word2"));
        assertEquals("word1 \u2013 \u2013 word3", Note.createLemmaFromLongText("word1 word2 word3"));
        assertEquals("word1 \u2013 \u2013 word3",
                Note.createLemmaFromLongText("word1\t word2 \nword3"));
        assertEquals("foo \u2013 \u2013 bar",
                Note.createLemmaFromLongText(" \n      foo \n \t baz    bar    \n\t\t"));
        assertEquals("foo", Note.createLemmaFromLongText(" \n      foo \n \t   \n\t\t"));
    }

    @Test
    public void createNote() {
        Document document = documentRepository.getDocumentForPath(testDocument);

        String longText = "two words";
        DocumentNote documentNote = noteRepository.createDocumentNote(new Note(), document.getRevision(-1), "10",
                longText);

        assertNotNull(documentNote);
    }

    @Test
    public void createNote_Note_With_The_Lemma_Already_Exists_Notes_Are_Same() {
        Document document = documentRepository.getDocumentForPath(testDocument);

        String longText = "two words";
        DocumentNote documentNote = noteRepository.createDocumentNote(new Note(), document.getRevision(-1), "10",
                longText);
        DocumentNote documentNote2 = noteRepository.createDocumentNote(documentNote.getNote(), document.getRevision(-1), "11",
                longText);
        assertEquals(documentNote.getNote().getId(), documentNote2.getNote().getId());
    }

    @Test
    public void find() {
        Document document = documentRepository.getDocumentForPath(testDocument);
        noteRepository.createDocumentNote(new Note(), document.getRevision(-1), "lid1234", "foobar");
        Note note = noteRepository.find("foobar");
        assertNotNull(note);
    }

    @Test
    public void importNote() throws Exception {
        noteRepository.importNotes(noteTestData);
        Note note = noteRepository.find("kereitten");
        assertNotNull(note);
        assertEquals("kereitten", note.getLemma());
        assertEquals("'keritte'", note.getLemmaMeaning());
        assertEquals(
                "(murt. kerii ’keri\u00E4’, ks. <bibliograph>Itkonen 1989</bibliograph> , 363).",
                note.getDescription().toString().replaceAll("\\s+", " ").trim());
        assertEquals("<bibliograph>v</bibliograph>",
                note.getSources().toString().replaceAll("\\s+", " ").trim());
    }

    @Test
    public void Add_Note_With_Existing_Orphan() {
        noteRepository.importNotes(noteTestData);
        String lemma = "riksi\u00E4";
        Note note = noteRepository.find(lemma);
        assertNotNull(note);
        Document document = documentRepository.getDocumentForPath(testDocument);
        DocumentNote documentNote = noteRepository.createDocumentNote(note, document.getRevision(-1), "123456",
                lemma);
        assertNotNull(documentNote);
        assertEquals(note.getId(), documentNote.getNote().getId());
        assertNotNull(documentNoteRepository.getByLocalId(document.getRevision(-1), "123456")
                .getNote());
        assertEquals(1, documentNoteRepository.queryNotes(lemma).getAvailableRows());
    }

    @Test
    public void Add_Note_With_Existing_Orphan_Verify_Sources_And_Description_Correct() {
        noteRepository.importNotes(noteTestData);
        String lemma = "riksi\u00E4";
        Note note = noteRepository.find(lemma);
        assertNotNull(note);
        Document document = documentRepository.getDocumentForPath(testDocument);
        DocumentNote documentNote = noteRepository.createDocumentNote(note, document.getRevision(-1), "123456",
                lemma);
        assertNotNull(documentNote);
        assertEquals(note.getId(), documentNote.getNote().getId());
        assertEquals(note.getDescription().getElements(),
                documentNoteRepository.getByLocalId(document.getRevision(-1), "123456").getNote()
                        .getDescription().getElements());
        assertEquals(note.getSources().getElements(),
                documentNoteRepository.getByLocalId(document.getRevision(-1), "123456").getNote()
                        .getSources().getElements());
    }

    @Test
    public void Import_The_Same_Notes_Twice() {
        assertEquals(9, noteRepository.importNotes(noteTestData));
        assertEquals(9, noteRepository.importNotes(noteTestData));
        assertEquals(18, documentNoteRepository.getAll().size());
    }

    @Test
    public void importNotes() throws Exception {
        assertEquals(9, noteRepository.importNotes(noteTestData));
        assertEquals(9, noteRepository.getAll().size());
        assertNotNull(noteRepository.find("kereitten"));
    }

    @Test
    public void queryDictionary() throws Exception {
        assertEquals(9, noteRepository.importNotes(noteTestData));
        assertEquals(0, noteRepository.queryDictionary("*").getAvailableRows());
    }

    @Test
    public void queryDictionary2() throws Exception {
        assertEquals(9, noteRepository.importNotes(noteTestData));
        GridDataSource dataSource = noteRepository.queryDictionary("a");
        int count1 = dataSource.getAvailableRows();
        int count2 = dataSource.getAvailableRows();
        assertEquals(count1, count2);
    }

    @Test
    public void remove() {
        Document document = documentRepository.getDocumentForPath(testDocument);
        List<RevisionInfo> revisions = documentRepository.getRevisions(document);
        long latestRevision = revisions.get(revisions.size() - 1).getSvnRevision();

        String longText = UUID.randomUUID().toString();
        noteRepository.createDocumentNote(new Note(), document.getRevision(latestRevision), "10", longText);
    }

    @Test
    public void removeComment() {
        Note note = new Note();
        noteRepository.save(note);
        NoteComment comment = noteRepository.createComment(note, "boomboomboom");
        Collection<NoteComment> comments = noteRepository.getById(note.getId()).getComments();
        assertEquals(1, comments.size());
        assertEquals(comment.getMessage(), comments.iterator().next().getMessage());
        noteRepository.removeComment(comment.getId());
        comments = noteRepository.getById(note.getId()).getComments();
        assertTrue(comments.isEmpty());
    }

    @Before
    public void setUp() {
        adminService.removeNotesAndTerms();
    }

    @Test
    public void Find_Notes() {
        noteRepository.importNotes(noteTestData);
        assertEquals(1, noteRepository.findNotes("kereitten").size());
    }

    @Test
    public void Remove_Based_On_Revision() {
        noteRepository.importNotes(noteTestData);
        Collection<DocumentNote> documentNotes = documentNoteRepository.getAll();
        assertFalse(documentNotes.isEmpty());
        for (DocumentNote documentNote : documentNotes) {
            noteRepository.remove(documentNote, documentNote.getSVNRevision());
        }
        assertTrue(documentNoteRepository.query(new DocumentNoteSearchInfo()).isEmpty());
    }
}
