package fi.finlit.edith.ui.test.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fi.finlit.edith.EDITH;
import fi.finlit.edith.domain.Document;
import fi.finlit.edith.domain.DocumentNote;
import fi.finlit.edith.domain.Note;
import fi.finlit.edith.domain.Term;
import fi.finlit.edith.dto.DocumentRevision;
import fi.finlit.edith.dto.SelectedText;
import fi.finlit.edith.ui.services.AdminService;
import fi.finlit.edith.ui.services.DocumentRepository;
import fi.finlit.edith.ui.services.DocumentNoteRepository;
import fi.finlit.edith.ui.services.NoteAdditionFailedException;
import fi.finlit.edith.ui.services.svn.SubversionException;
import fi.finlit.edith.ui.services.svn.SubversionService;

public class DocumentRepositoryTest extends AbstractServiceTest {

    @Inject
    private DocumentRepository documentDao;

    @Inject
    private DocumentNoteRepository documentNoteRepository;

    @Inject
    private SubversionService subversionService;

    @Inject
    private AdminService adminService;

    @Inject
    @Symbol(EDITH.SVN_DOCUMENT_ROOT)
    private String documentRoot;

    @Inject
    @Symbol(EDITH.EXTENDED_TERM)
    private boolean extendedTerm;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() {
        closeStreams();
        adminService.removeNotesAndTermsAndDocuments();
        subversionService.destroy();
        subversionService.initialize();
    }

    @Test
    public void AddDocument() throws IOException{
        File file = File.createTempFile("test", null);
        FileUtils.writeStringToFile(file, "test file", "UTF-8");
        String targetPath = "/documents/" + UUID.randomUUID().toString();
        documentDao.addDocument(targetPath, file);

        Document document = documentDao.getOrCreateDocumentForPath(targetPath);
        assertFalse(documentDao.getRevisions(document).isEmpty());
    }

    @Test
    public void AddDocuments_From_Zip() {
        File file = new File("src/test/resources/tei.zip");
        assertEquals(5, documentDao.addDocumentsFromZip("/documents/parent", file));

        assertTrue(subversionService.getLatestRevision("/documents/parent/Kullervo.xml") > 0);
        assertTrue(subversionService.getLatestRevision("/documents/parent/Nummisuutarit_100211.xml") > 0);
        assertTrue(subversionService.getLatestRevision("/documents/parent/Nummisuutarit.xml") > 0);
        assertTrue(subversionService.getLatestRevision("/documents/parent/Olviretki_Schleusingenissa_manuscript.xml") > 0);
        assertTrue(subversionService.getLatestRevision("/documents/parent/xmlparsertest.xml") > 0);
    }

    @Test
    public void AddNote() throws Exception {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp2-p";
        String text = "sun ullakosta ottaa";

        DocumentNote note = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));

        String content = getContent(document.getSvnPath(), -1);
        String localId = note.getId();
        System.err.println("ID: " + localId);
        System.err.println(content);
        assertTrue(content.contains(start(localId) + text + end(localId)));
    }

    @Test
    public void AddRemoveNote() throws IOException, NoteAdditionFailedException{
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        int count = documentNoteRepository.queryNotes("*").getAvailableRows();

        // add
        String element = "play-act-sp4-p";
        String text = "min\u00E4; ja nytp\u00E4, luulen,";
        DocumentNote documentNote = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));

        assertEquals(count+1, documentNoteRepository.queryNotes("*").getAvailableRows());
        // remove
        documentDao.removeNotes(document.getRevision(documentNote.getSVNRevision()), documentNote);
        DocumentNote deletedDocumentNote = documentNoteRepository.getById(documentNote.getId());
        assertTrue(deletedDocumentNote.isDeleted());

        GridDataSource dataSource = documentNoteRepository.queryNotes("*");
        int available = dataSource.getAvailableRows();
        dataSource.prepare(0, 1000, new ArrayList<SortConstraint>());
        assertEquals(0, available);
    }

    @Test
    public void Add_Note_With_The_Same_Lemma() throws IOException, NoteAdditionFailedException {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp2-p";
        String text = "s";

        DocumentNote note1 = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, 1, 1, text));
        DocumentNote note2 = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, 2, 2, text));
        assertEquals(2, documentNoteRepository.getAll().size());
        List<DocumentNote> documentNotes = documentNoteRepository.getOfDocument(note2.getDocRevision());
        assertEquals(2, documentNotes.size());
    }

    @Test
    public void GetAll() {
        assertEquals(10, documentDao.getAll().size());
    }

    private String getContent(String svnPath, long svnRevision) throws IOException{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = register(subversionService.getStream(svnPath, svnRevision));
        IOUtils.copy(in, out);
        in.close();
        out.close();
        return new String(out.toByteArray(), "UTF-8");
    }

    private Document getDocument(String path){
        return documentDao.getOrCreateDocumentForPath(documentRoot + path);
    }

    @Test
    public void GetDocumentForPath() {
        assertNotNull(documentDao.getOrCreateDocumentForPath("/documents/" + UUID.randomUUID().toString()));
    }

    @Test
    public void GetDocumentsOfFolder() {
        assertEquals(10, documentDao.getDocumentsOfFolder(documentRoot).size());
    }

    @Test
    public void GetDocumentStream() throws IOException {
        for (Document document : documentDao.getAll()) {
            register(documentDao.getDocumentStream(new DocumentRevision(document, -1)));
        }
    }

    @Test
    public void GetRevisions() {
        for (Document document : documentDao.getAll()) {
            assertFalse(documentDao.getRevisions(document).isEmpty());
        }
    }

    @Test
    public void RemoveAllNotes() throws Exception{
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp2-p";
        String text = "sun ullakosta ottaa";

        DocumentNote noteRevision = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));
        DocumentRevision docRevision = noteRevision.getDocumentRevision();

        List<DocumentNote> revs = documentNoteRepository.getOfDocument(docRevision);
        assertTrue(revs.size() > 0);

        docRevision = documentDao.removeAllNotes(document);
        revs = documentNoteRepository.getOfDocument(docRevision);
        assertEquals(0, revs.size());
    }

    @Test
    public void RemoveNotes() throws Exception {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp2-p";
        String text = "sun ullakosta ottaa";

        DocumentNote documentNote = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));
        documentDao.removeNotes(document.getRevision(-1), new DocumentNote[] { documentNote });

        String content = getContent(document.getSvnPath(), -1);
        assertFalse(content.contains(start(documentNote.getId()) + text + end(documentNote.getId())));
    }

    @Test
    public void RemoveNotes_several() throws Exception {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp2-p";
        String text = "sun ullakosta ottaa";
        String text2 = "ottaa";
        String text3 = "ullakosta";

        DocumentNote noteRev = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));
        // note2 won't be removed
        DocumentNote noteRev2 = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText( element, element, text2));
        DocumentNote noteRev3 = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text3));
        documentDao.removeNotes(document.getRevision(-1), new DocumentNote[] { noteRev, noteRev3 });

        String content = getContent(document.getSvnPath(), -1);
        assertFalse(content.contains(start(noteRev.getId()) + text + end(noteRev.getId())));
        assertTrue(content.contains(start(noteRev2.getId()) + text2 + end(noteRev2.getId())));
        assertFalse(content.contains(start(noteRev3.getId()) + text3 + end(noteRev3.getId())));
    }

    @Test
    public void UpdateNote() throws Exception {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp2-p";
        String text = "sun ullakosta ottaa";

        DocumentNote noteRevision = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));

        String newText = "sun ullakosta";
        documentDao.updateNote(noteRevision, new SelectedText(element, element, newText));

        String content = getContent(document.getSvnPath(), -1);
        String localId = noteRevision.getId();
        assertFalse(content.contains(start(localId) + text + end(localId)));
        assertTrue(content.contains(start(localId) + newText + end(localId)));
    }

    @Test
    public void UpdateNote2() throws IOException, NoteAdditionFailedException{
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp3-p";
        String text = "\u00E4st";

        DocumentNote noteRevision = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));

        //T-äst-ä
        String newText = "T\u00E4st\u00E4";
        documentDao.updateNote(noteRevision, new SelectedText(element, element, newText));

        String content = getContent(document.getSvnPath(), -1);
        String localId = noteRevision.getId();
//        System.out.println(content);
        assertFalse(content.contains(start(localId) + text + end(localId)));
        assertTrue(content.contains(start(localId) + newText + end(localId)));
        // Täst<anchor xml:id="start1266836640612"/>ä<anchor xml:id="end1266836640612"/> rientää
    }

    @Test
    public void UpdateNote_With_Publishable_State() throws IOException, NoteAdditionFailedException {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        String element = "play-act-sp3-p";
        String text = "\u00E4st";

        DocumentNote noteRevision = documentDao.addNote(createNote(), document.getRevision(-1), new SelectedText(element, element, text));
        noteRevision.setPublishable(true);

        String newText = "T\u00E4st\u00E4";
        DocumentNote updatedRevision = documentDao.updateNote(noteRevision, new SelectedText(element, element, newText));
        assertTrue(updatedRevision.isPublishable());
    }

    @Test(expected = RuntimeException.class)
    public void Remove() throws Exception {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        InputStream stream = documentDao.getDocumentStream(document.getRevision(-1));
        assertNotNull(stream);
        IOUtils.closeQuietly(stream);
        documentDao.remove(document);
        documentDao.getDocumentStream(document.getRevision(-1));
    }

    private Note createNote() {
        Note note = new Note();
        if (extendedTerm) {
            note.setTerm(new Term());
        }
        return note;
    }

    @Test
    public void Move_Updates_Document_Location_And_Title() {
        String newTitle = "Pummisuutarit rakeistettuna.xml";
        String oldTitle = "Nummisuutarit rakenteistettuna.xml";
        documentDao.move(getDocument("/" + oldTitle).getId(), "/" + newTitle);
        boolean found = false;
        for (Document document : documentDao.getAll()) {
            if (newTitle.equals(document.getTitle()) && ("/documents/trunk/" + newTitle).equals(document.getSvnPath())) {
                found = true;
            }
            if (oldTitle.equals(document.getTitle()) || ("/documents/trunk/" + oldTitle).equals(document.getSvnPath())) {
                fail("Old document was still available!");
            }
        }
        assertTrue(found);
    }

    @Test(expected = SubversionException.class)
    public void Moved_File_Is_No_Longer_Available_In_Old_Location() throws IOException {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        documentDao.move(document.getId(), "/Pummisuutarit rakeistettuna.xml");
        documentDao.getDocumentStream(document.getRevision(-1));
    }

    @Test
    public void Moved_File_Is_Available_In_New_Location() throws IOException {
        Document document = getDocument("/Nummisuutarit rakenteistettuna.xml");
        documentDao.move(document.getId(), "/Pummisuutarit rakeistettuna.xml");
        Document movedDocument = getDocument("/Pummisuutarit rakeistettuna.xml");
        assertNotNull(documentDao.getDocumentStream(movedDocument.getRevision(-1)));
    }

    @Test(expected = SubversionException.class)
    public void Renamed_File_Is_No_Longer_Available_With_Old_Name() throws IOException {
        Document document = getDocument("/letters/letter_to_the_editor.xml");
        documentDao.move(document.getId(), "/letters/letter_to_the_reader.xml");
        documentDao.getDocumentStream(document.getRevision(-1));
    }

    @Test
    public void Renamed_File_Is_Available_In_New_Location() throws IOException {
        Document document = getDocument("/letters/letter_to_the_editor.xml");
        documentDao.move(document.getId(), "/letters/letter_to_the_reader.xml");
        Document movedDocument = getDocument("/letters/letter_to_the_reader.xml");
        assertNotNull(documentDao.getDocumentStream(movedDocument.getRevision(-1)));
    }

    @Test
    public void From_Path() {
        fail("not yet implemented!");
    }
}
