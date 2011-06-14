/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.test.services;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tapestry5.grid.ColumnSort;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SessionFactory;

import fi.finlit.edith.EDITH;
import fi.finlit.edith.EdithTestConstants;
import fi.finlit.edith.domain.*;
import fi.finlit.edith.dto.DocumentNoteSearchInfo;
import fi.finlit.edith.dto.DocumentRevision;
import fi.finlit.edith.dto.OrderBy;
import fi.finlit.edith.dto.SelectedText;
import fi.finlit.edith.ui.services.AdminService;
import fi.finlit.edith.ui.services.DocumentNoteDao;
import fi.finlit.edith.ui.services.DocumentDao;
import fi.finlit.edith.ui.services.NoteDao;
import fi.finlit.edith.ui.services.NoteWithInstances;
import fi.finlit.edith.ui.services.UserDao;
import fi.finlit.edith.ui.services.svn.RevisionInfo;

public class DocumentNoteRepositoryTest extends AbstractServiceTest {
    @Inject
    @Symbol(EdithTestConstants.TEST_DOCUMENT_KEY)
    private String testDocument;

    @Inject
    private UserDao userRepository;

    @Inject
    private NoteDao noteRepository;

    @Inject
    private AdminService adminService;

    @Inject
    private DocumentNoteDao documentNoteRepository;

    @Inject
    private DocumentDao documentRepository;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    @Symbol(EDITH.EXTENDED_TERM)
    private boolean extendedTerm;

    private Document document;

    private DocumentRevision docRev;

    private long latestRevision;

    private DocumentNoteSearchInfo searchInfo;

    private DocumentNote documentNote1;

    private DocumentNote documentNote2;

    private DocumentNote documentNote3;

    private DocumentNote documentNote4;

    @Inject
    @Symbol(EdithTestConstants.NOTE_TEST_DATA_KEY)
    private File noteTestData;


    private int countDocumentNotes(List<NoteWithInstances> notes){
        int count = 0;
        for (NoteWithInstances n : notes){
            count += n.getDocumentNotes().size();
        }
        return count;
    }

    @Before
    public void setUp() {
        adminService.removeNotesAndTerms();

        document = documentRepository.getOrCreateDocumentForPath(testDocument);
        List<RevisionInfo> revisions = documentRepository.getRevisions(document);
        latestRevision = revisions.get(revisions.size() - 1).getSvnRevision();

        docRev = document.getRevision(latestRevision);
        documentNote1 = noteRepository.createDocumentNote(createNote(), docRev, "1", "l\u00E4htee h\u00E4ihins\u00E4 Mikko Vilkastuksen", 0);
        documentNote2 = noteRepository.createDocumentNote(createNote(), docRev, "2", "koska suutarille k\u00E4skyn k\u00E4r\u00E4jiin annoit, saadaksesi naimalupaa.", 0);
        documentNote3 = noteRepository.createDocumentNote(createNote(), docRev, "3", "tulee, niin seisoo s\u00E4\u00E4t\u00F6s-kirjassa.", 0);
        documentNote4 = noteRepository.createDocumentNote(createNote(), docRev, "4", "kummallenkin m\u00E4\u00E4r\u00E4tty, niin emmep\u00E4 tiet\u00E4isi t\u00E4ss\u00E4", 0);

        searchInfo = new DocumentNoteSearchInfo();
        searchInfo.setCurrentDocument(document);

        addExtraNote("testo");
        addExtraNote("testo2");
    }

    @Test
    public void GetByLocalId_Returns_NonNull_Result() {
        assertNotNull(documentNoteRepository.getById(documentNote1.getId()));
        assertNotNull(documentNoteRepository.getById(documentNote2.getId()));
        assertNotNull(documentNoteRepository.getById(documentNote3.getId()));
        assertNotNull(documentNoteRepository.getById(documentNote4.getId()));
    }

    @Test
    public void GetOfDocument_Resturns_Right_Amount_Of_Results() {
        assertEquals(4, documentNoteRepository.getOfDocument(docRev).size());
    }

    @Test
    public void GetOfDocument_With_Note_Updates() {
        assertEquals(4, documentNoteRepository.getOfDocument(docRev).size());

        for (DocumentNote documentNote : documentNoteRepository.getOfDocument(docRev)) {
//            documentNote = documentNote.createCopy();
            documentNote.getNote().setLemma(documentNote.getNote().getLemma() + "XXX");
            documentNoteRepository.save(documentNote);
        }

        assertEquals(4, documentNoteRepository.getOfDocument(docRev).size());
    }

    @Test
    public void QueryNotes_Returns_More_Than_Zero_Results() {
        assertTrue(documentNoteRepository.queryNotes("annoit").getAvailableRows() > 0);
    }

    @Test
    public void QueryNotes_Sorting_Is_Case_Insensitive() {
        noteRepository.createDocumentNote(createNote(), docRev, "a");
        noteRepository.createDocumentNote(createNote(), docRev, "b");
        noteRepository.createDocumentNote(createNote(), docRev, "A");
        noteRepository.createDocumentNote(createNote(), docRev, "B");
        GridDataSource gds = documentNoteRepository.queryNotes("*");
        int n = gds.getAvailableRows();
        List<SortConstraint> sortConstraints = new ArrayList<SortConstraint>();
        sortConstraints.add(new SortConstraint(new PropertyModelMock(), ColumnSort.ASCENDING));
        gds.prepare(0, 100, sortConstraints);
        String previous = null;
        for (int i = 0; i < n; ++i) {
            String current = gds.getRowValue(i).toString().toLowerCase();
            if (previous != null) {
                assertThat("The actual value was probably in upper case!", previous,
                        lessThanOrEqualTo(current));
            }
            previous = current;
        }
    }

    @Test
    public void Remove_Sets_Deleted_Flag() {
        DocumentNote documentNote = documentNoteRepository.getById(documentNote1.getId());
        documentNoteRepository.remove(documentNote);
        assertTrue(documentNoteRepository.getById(documentNote.getId()).isDeleted());
    }

    @Test
    public void Remove_By_Id() {
        DocumentNote documentNote = documentNoteRepository.getById(documentNote1.getId());
        documentNoteRepository.remove(documentNote.getId());
        assertTrue(documentNoteRepository.getById(documentNote.getId()).isDeleted());
    }



    @Test
    public void Store_And_Retrieve_Person_Note() {
        DocumentNote documentNote = noteRepository
                .createDocumentNote(createNote(), docRev,
                        "kummallenkin m\u00E4\u00E4r\u00E4tty, niin emmep\u00E4 tiet\u00E4isi t\u00E4ss\u00E4");
        Note note = documentNote.getNote();
        note.setFormat(NoteFormat.PERSON);
        NameForm normalizedForm = new NameForm("Aleksis", "Kivi",
                "Suomen hienoin kirjailija ikinä.");
        Set<NameForm> otherForms = new HashSet<NameForm>();
        otherForms.add(new NameForm("Alexis", "Stenvall", "En jättebra skrivare."));
        note.setPerson(new Person(normalizedForm, otherForms));
        Interval timeOfBirth = Interval.createDate(new LocalDate(1834, 10, 10));
        Interval timeOfDeath = Interval.createDate(new LocalDate(1872, 12, 31));
        note.getPerson().setTimeOfBirth(timeOfBirth);
        note.getPerson().setTimeOfDeath(timeOfDeath);
        noteRepository.save(note);
        Note persistedNote = documentNoteRepository.getById(documentNote.getId()).getNote();
        assertEquals(note.getPerson().getNormalizedForm().getName(), persistedNote.getPerson()
                .getNormalizedForm().getName());
        assertEquals(note.getPerson().getNormalizedForm().getDescription(), persistedNote
                .getPerson().getNormalizedForm().getDescription());
        assertEquals(note.getFormat(), persistedNote.getFormat());

        assertEquals(note.getPerson().getTimeOfBirth().getStart(), persistedNote.getPerson().getTimeOfBirth().getStart());
        assertEquals(note.getPerson().getTimeOfBirth().getEnd(),   persistedNote.getPerson().getTimeOfBirth().getEnd());
        assertEquals(note.getPerson().getTimeOfBirth().getDate(), persistedNote.getPerson().getTimeOfBirth().getDate());

        assertEquals(note.getPerson().getTimeOfDeath().getStart(), persistedNote.getPerson().getTimeOfDeath().getStart());
        assertEquals(note.getPerson().getTimeOfDeath().getEnd(), persistedNote.getPerson().getTimeOfDeath().getEnd());
        assertEquals(note.getPerson().getTimeOfDeath().getDate(), persistedNote.getPerson().getTimeOfDeath().getDate());
    }

    @Test
    public void Store_And_Retrieve_Person_With_The_Same_Birth_And_Death_Date() {
        DocumentNote documentNote = noteRepository
                .createDocumentNote(createNote(), docRev,
                        "kummallenkin m\u00E4\u00E4r\u00E4tty, niin emmep\u00E4 tiet\u00E4isi t\u00E4ss\u00E4");
        Note note = documentNote.getNote();
        note.setFormat(NoteFormat.PERSON);
        Interval timeOfBirth = Interval.createYear(1834);
        Interval timeOfDeath = Interval.createYear(1834);
        note.setPerson(new Person());
        note.getPerson().setTimeOfBirth(timeOfBirth);
        note.getPerson().setTimeOfDeath(timeOfDeath);
        noteRepository.save(note);
        Note persistedNote = documentNoteRepository.getById(documentNote.getId()).getNote();
        assertNotNull(persistedNote.getPerson().getTimeOfBirth());
        assertNotNull(persistedNote.getPerson().getTimeOfDeath());
    }

    @Test
    public void Store_And_Retrieve_Place_Note() {
        DocumentNote documentNote = noteRepository
                .createDocumentNote(createNote(), docRev,
                        "kummallenkin m\u00E4\u00E4r\u00E4tty, niin emmep\u00E4 tiet\u00E4isi t\u00E4ss\u00E4");
        Note note = documentNote.getNote();
        note.setFormat(NoteFormat.PLACE);
        NameForm normalizedForm = new NameForm("Tampere", "Kaupunki Hämeessä.");
        Set<NameForm> otherForms = new HashSet<NameForm>();
        otherForms.add(new NameForm("Tammerfors", "Ruotsinkielinen nimitys."));
        note.setPlace(new Place(normalizedForm, otherForms));
        noteRepository.save(note);
        Note persistedNote = documentNoteRepository.getById(documentNote.getId()).getNote();
        assertEquals(note.getPlace().getNormalizedForm().getName(), persistedNote.getPlace()
                .getNormalizedForm().getName());
        assertEquals(note.getPlace().getNormalizedForm().getDescription(), persistedNote.getPlace()
                .getNormalizedForm().getDescription());
        assertEquals(note.getFormat(), persistedNote.getFormat());
    }

    @Test
    public void Query_For_All_Notes() {
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        assertEquals(6, notes.size());
    }

    @Test
    public void Query_and_Delete(){
        searchInfo.setDocuments(Collections.singleton(document));
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        assertFalse(notes.isEmpty());

        // remove note
        assertFalse(notes.get(0).getDocumentNotes().isEmpty());
        DocumentNote documentNote = notes.get(0).getDocumentNotes().iterator().next();
        DocumentRevision newRevision = documentRepository.removeNotes(documentNote.getDocumentRevision(), documentNote);
        assertNotNull(newRevision);

        // assert that query returns less results
        List<NoteWithInstances> newResults = noteRepository.findNotesWithInstances(searchInfo);
        assertEquals(notes.size() - 1, newResults.size());

    }

    @Test
    public void Query_For_All_Notes_Ordered_By_Lemma_Ascending_By_Default() {
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        DocumentNote previous = null;
        for (NoteWithInstances note : notes){
            for (DocumentNote documentNote : note.getDocumentNotes()) {
                if (previous != null) {
                    assertThat(previous.getNote().getLemma().toLowerCase(),
                            lessThanOrEqualTo(documentNote.getNote().getLemma().toLowerCase()));
                }
                previous = documentNote;
            }
        }
    }

    @Test
    public void Query_For_Notes_Based_On_Document() {
        searchInfo.getDocuments().add(docRev.getDocument());
        assertEquals(4, noteRepository.findNotesWithInstances(searchInfo).size());
    }

    @Test
    public void Query_For_Notes_Based_On_Creator() {
        UserInfo userInfo = userRepository.getUserInfoByUsername("testo");
        searchInfo.getCreators().add(userInfo);
        assertEquals(1, noteRepository.findNotesWithInstances(searchInfo).size());
    }

    @Test
    @Ignore // FIXME
    public void Query_For_Notes_Based_On_Editors() {
        UserInfo userInfo = userRepository.getUserInfoByUsername("testo");
        searchInfo.getCreators().add(userInfo);
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        assertEquals(1, notes.size());
        documentNoteRepository.save(notes.get(0).getDocumentNotes().iterator().next());
        notes = noteRepository.findNotesWithInstances(searchInfo);
        assertEquals(1, notes.size());
        assertEquals("timo", notes.get(0).getNote().getConcept(extendedTerm).getLastEditedBy().getUsername());
    }

    @Test
    public void Query_For_Notes_Based_On_Creators() {
        UserInfo userInfo1 = userRepository.getUserInfoByUsername("testo");
        UserInfo userInfo2 = userRepository.getUserInfoByUsername("testo2");
        searchInfo.getCreators().add(userInfo1);
        searchInfo.getCreators().add(userInfo2);
        assertEquals(2, noteRepository.findNotesWithInstances(searchInfo).size());
    }

    @Test
    public void Query_For_Notes_Based_On_Note_Type() {
        searchInfo.getNoteTypes().add(NoteType.HISTORICAL);
        assertEquals(2, noteRepository.findNotesWithInstances(searchInfo).size());
    }

    @Test
    public void Query_For_Notes_Based_On_Note_Type_Two_Filters() {
        searchInfo.getNoteTypes().add(NoteType.HISTORICAL);
        searchInfo.getNoteTypes().add(NoteType.DICTUM);
        assertEquals(2, noteRepository.findNotesWithInstances(searchInfo).size());
    }

    @Test
    public void Query_For_Notes_Based_On_Note_Format() {
        searchInfo.getNoteFormats().add(NoteFormat.PERSON);
        assertEquals(2, noteRepository.findNotesWithInstances(searchInfo).size());
    }

    @Test
    public void Query_For_All_Notes_Order_By_Creator_Ascending() {
        searchInfo.setOrderBy(OrderBy.USER);
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        DocumentNote previous = null;
        for (NoteWithInstances note : notes){
            for (DocumentNote documentNote : note.getDocumentNotes()) {
                if (previous != null) {
                    String previousUsername, currentUsername;
                    previousUsername = previous.getNote().getConcept(extendedTerm).getLastEditedBy().getUsername();
                    currentUsername = documentNote.getNote().getConcept(extendedTerm).getLastEditedBy().getUsername();
                    assertThat(previousUsername, lessThanOrEqualTo(currentUsername));
                }
                previous = documentNote;
            }
        }
    }

    @Test
    public void Query_For_All_Notes_Order_By_Creator_Descending() {
        searchInfo.setOrderBy(OrderBy.USER);
        searchInfo.setAscending(false);
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        DocumentNote previous = null;
        for (NoteWithInstances note : notes){
            for (DocumentNote documentNote : note.getDocumentNotes()) {
                if (previous != null) {
                    String previousUsername, currentUsername;
                    previousUsername = previous.getNote().getConcept(extendedTerm).getLastEditedBy().getUsername();
                    currentUsername = documentNote.getNote().getConcept(extendedTerm).getLastEditedBy().getUsername();
                    assertThat(previousUsername, greaterThanOrEqualTo(currentUsername));
                }
                previous = documentNote;
            }
        }
    }

    @Test
    public void Query_For_All_Notes_Order_By_Date_Of_Creation_Ascending() {
        searchInfo.setOrderBy(OrderBy.DATE);
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        NoteWithInstances previous = null;
        for (NoteWithInstances note : notes){
            if (previous != null){
                assertThat(previous.getNote().getEditedOn(), lessThanOrEqualTo(note.getNote().getEditedOn()));
            }
            previous = note;
        }
    }

    @Test
    public void Query_For_Orphans() {
        noteRepository.importNotes(noteTestData);
        assertEquals(15, noteRepository.getAll().size());
        searchInfo.setOrphans(true);
        assertEquals(9, noteRepository.findNotesWithInstances(searchInfo).size());
    }

    @Test
    public void Query_For_All_Notes_Order_By_Status() {
        searchInfo.setOrderBy(OrderBy.STATUS);
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        DocumentNote edited = notes.get(2).getDocumentNotes().iterator().next();
        edited.getNote().getConcept(extendedTerm).setStatus(NoteStatus.FINISHED);
        documentNoteRepository.save(edited);
        notes = noteRepository.findNotesWithInstances(searchInfo);
        DocumentNote previous = null;
        for (NoteWithInstances note : notes){
            for (DocumentNote documentNote : note.getDocumentNotes()) {
                if (previous != null) {
                    NoteStatus previousStatus = previous.getNote().getConcept(extendedTerm).getStatus();
                    NoteStatus currentStatus = documentNote.getNote().getConcept(extendedTerm).getStatus();
                    assertTrue(previousStatus + " " + currentStatus,
                            previousStatus.compareTo(currentStatus) <= 0);
                }
                previous = documentNote;
            }
        }

    }

    @Test
    public void Save_As_Copy() {
        DocumentNote documentNote = documentNoteRepository.getOfDocument(docRev).get(0);
        Note initialNote = noteRepository.getById(documentNote.getNote().getId());
        Concept concept = initialNote.getConcept(extendedTerm);

        concept.setDescription(new Paragraph().toString());
        initialNote.setFormat(NoteFormat.PLACE);
        initialNote.setLemmaMeaning("fajflkjsalj");
        initialNote.setPerson(new Person(new NameForm(), new HashSet<NameForm>()));
        initialNote.setPlace(new Place(new NameForm(), new HashSet<NameForm>()));
        concept.setSources(new Paragraph().toString());
        concept.setSubtextSources(new Paragraph().toString());
        initialNote.setTerm(new Term());
        initialNote.getTerm().setBasicForm("foobar");
        concept.getTypes().add(NoteType.HISTORICAL);
        noteRepository.save(initialNote);

        documentNote = documentNoteRepository.getById(documentNote.getId());
        documentNote.getConcept(extendedTerm).setDescription(new Paragraph().toString());
        documentNote.getConcept(extendedTerm).setDescription(new Paragraph().addElement(new StringElement("foo")).toString());
        documentNote.getNote().setFormat(NoteFormat.PERSON);
        documentNote.getNote().setLemmaMeaning("totally different");
        documentNote.getNote().getPerson().getNormalizedForm().setFirst("something else");
        documentNote.getNote().getPlace().getNormalizedForm().setName("barfo");
        documentNote.getConcept(extendedTerm).setSources(new Paragraph().toString());
        documentNote.getConcept(extendedTerm).setSources(new Paragraph().addElement(new StringElement("bar")).toString());
        documentNote.getConcept(extendedTerm).setSubtextSources(new Paragraph().toString());
        documentNote.getConcept(extendedTerm).setSubtextSources(new Paragraph().addElement(new StringElement("foooo")).toString());
        documentNote.getNote().getTerm().setBasicForm("baaaaar");
        documentNote.getConcept(extendedTerm).getTypes().add(NoteType.WORD_EXPLANATION);
        documentNote.getConcept(extendedTerm).getComments().add(new NoteComment(documentNote.getConcept(extendedTerm), "jeejee", "vesa"));
        Note note = documentNoteRepository.getById(documentNote.getId()).getNote();
        documentNoteRepository.saveAsCopy(documentNote);

        DocumentNote copyOfDocumentNote = documentNoteRepository.getById(documentNote.getId());
        Note copyOfNote = copyOfDocumentNote.getNote();
        assertThat(copyOfNote.getId(), not(note.getId()));
        assertThat(copyOfNote, not(note));
        assertThat(copyOfNote.getConcept(extendedTerm).getDescription(), not(note.getConcept(extendedTerm).getDescription()));
        assertThat(copyOfNote.getFormat(), not(note.getFormat()));
        assertThat(copyOfNote.getLemmaMeaning(), not(note.getLemmaMeaning()));
        assertEquals(copyOfNote.getPerson(), note.getPerson());
        assertEquals(copyOfNote.getPlace(), note.getPlace());
        assertThat(copyOfNote.getConcept(extendedTerm).getSources(), not(note.getConcept(extendedTerm).getSources()));
        assertThat(copyOfNote.getConcept(extendedTerm).getSubtextSources(), not(note.getConcept(extendedTerm).getSubtextSources()));
        assertEquals(copyOfNote.getTerm(), note.getTerm());
        assertThat(copyOfNote.getConcept(extendedTerm).getTypes(), not(note.getConcept(extendedTerm).getTypes()));
        assertThat(copyOfNote.getConcept(extendedTerm).getComments(), not(note.getConcept(extendedTerm).getComments()));
    }

    @Test
    public void Get_Document_Notes_Of_Note() {
        List<DocumentNote> documentNotesOfDocument = documentNoteRepository.getOfDocument(docRev);
        assertFalse(documentNotesOfDocument.isEmpty());
        List<DocumentNote> documentNotesOfNote = documentNoteRepository
                .getOfNote(documentNotesOfDocument.get(0).getNote().getId());
        assertFalse(documentNotesOfNote.isEmpty());
    }

    @Test
    public void Get_Document_Notes_Of_Term() {
        List<DocumentNote> documentNotesOfDocument = documentNoteRepository.getOfDocument(docRev);
        Term term = new Term();
        term.setBasicForm("foobar");
        term.setMeaning("a placeholder");
        DocumentNote documentNote = documentNotesOfDocument.get(0);
        documentNote.getNote().setTerm(term);
        documentNoteRepository.save(documentNote);
        List<DocumentNote> documentNotesOfTerm = documentNoteRepository.getOfTerm(term.getId());
        assertEquals(documentNote, documentNotesOfTerm.get(0));
    }

    @Test
    public void Add_The_Same_Word_Twice() {
        String text = "l\u00E4htee";
        Note note = noteRepository.createDocumentNote(createNote(), docRev, text).getNote();
        noteRepository.createDocumentNote(note, docRev, text);
        assertEquals(6, countDocumentNotes(noteRepository.findNotesWithInstances(searchInfo)));
    }

    @Test
    @Ignore
    public void Query_For_Document_Notes_And_Retrieve_The_One_Attached_To_Current_Document() {
        // FIXME
        String text = "l\u00E4htee";
        Note note = noteRepository.createDocumentNote(createNote(), docRev, text).getNote();
        DocumentNote documentNote = new DocumentNote();
        documentNote.setNote(note);
        documentNoteRepository.save(documentNote);

        searchInfo.setCurrentDocument(document);
        List<NoteWithInstances> notes = noteRepository.findNotesWithInstances(searchInfo);
        for (NoteWithInstances n : notes){
            for (DocumentNote current : n.getDocumentNotes()) {
                if (current.getNote().getLemma().equals(text)) {
                    if (current.getDocument() != null) {
                        System.err.println(current.getDocument().getId());
                    } else {
                        System.err.println("null");
                    }
                }
            }
        }

    }

    @Test
    public void Get_Document_Notes_Of_Person() {
        DocumentNote documentNote = documentNoteRepository.getOfDocument(docRev).iterator().next();
        Person person = new Person(new NameForm("Tom", "Sawyer"), new HashSet<NameForm>());
        documentNote.getNote().setPerson(person);
        documentNoteRepository.save(documentNote);
        assertEquals(1, documentNoteRepository.getOfPerson(person.getId()).size());
    }

    @Test
    public void Get_Document_Notes_Of_Place() {
        DocumentNote documentNote = documentNoteRepository.getOfDocument(docRev).iterator().next();
        Place place = new Place(new NameForm("Helsinki", "Capital of Finland"),
                new HashSet<NameForm>());
        documentNote.getNote().setPlace(place);
        documentNoteRepository.save(documentNote);
        assertEquals(1, documentNoteRepository.getOfPlace(place.getId()).size());
    }

    @Test
    public void Get_Document_Notes_Of_Note_In_Document() {
        DocumentNote documentNote = documentNoteRepository.getOfDocument(docRev).iterator().next();
        List<DocumentNote> documentNotes = documentNoteRepository.getOfNoteInDocument(documentNote
                .getNote().getId(), documentNote.getDocument().getId());
        assertEquals(1, documentNotes.size());
    }

    @Test
    public void Get_Publishable_Notes_Of_Document() throws Exception {
        String element = "play-act-sp2-p";
        String text = "sun ullakosta ottaa";
        DocumentNote documentNote = documentRepository.addNote(createNote(), docRev, new SelectedText(element, element, text));
        docRev = documentNote.getDocRevision();
        documentNote.setPublishable(true);
        assertTrue(documentNoteRepository.getPublishableNotesOfDocument(docRev).isEmpty());
        documentNoteRepository.save(documentNote);
        assertEquals(5, documentNoteRepository.getOfDocument(docRev).size());
        assertEquals(1, documentNoteRepository.getPublishableNotesOfDocument(docRev).size());
    }

    private void addExtraNote(String username) {
        DocumentNote documentNote = new DocumentNote();
        UserInfo userInfo = userRepository.getUserInfoByUsername(username);
        if (userInfo == null) {
            userInfo = new UserInfo(username);
        }
        Note note = createNote();
        note.setLemma("TheLemma");
        note.getConcept(extendedTerm).setTypes(new HashSet<NoteType>());
        note.getConcept(extendedTerm).getTypes().add(NoteType.HISTORICAL);
        note.setFormat(NoteFormat.PERSON);
        note.getConcept(extendedTerm).setLastEditedBy(userInfo);
        note.getConcept(extendedTerm).setAllEditors(new HashSet<UserInfo>());
        note.getConcept(extendedTerm).getAllEditors().add(userInfo);
        documentNote.setNote(note);
        documentNote.setLongText("thelongtext");
        documentNote.setCreatedOn(new DateTime().getMillis());
        Session session = null;
        try {
            session = sessionFactory.openSession();
            session.save(documentNote);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private Note createNote() {
        Note note = new Note();
        if (extendedTerm) {
            note.setTerm(new Term());
        }
        return note;
    }
}
