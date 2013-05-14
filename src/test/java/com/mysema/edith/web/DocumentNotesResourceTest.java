package com.mysema.edith.web;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mysema.edith.EdithTestConstants;
import com.mysema.edith.domain.Document;
import com.mysema.edith.domain.DocumentNote;
import com.mysema.edith.domain.Note;
import com.mysema.edith.domain.Term;
import com.mysema.edith.domain.User;
import com.mysema.edith.dto.DocumentNoteTO;
import com.mysema.edith.dto.SelectedText;
import com.mysema.edith.dto.SelectionTO;
import com.mysema.edith.services.DocumentDao;
import com.mysema.edith.services.NoteAdditionFailedException;
import com.mysema.edith.services.NoteDao;
import com.mysema.edith.services.UserDao;

public class DocumentNotesResourceTest extends AbstractResourceTest {
    
    @Inject @Named(EdithTestConstants.TEST_DOCUMENT_KEY)
    private String testDocument;
    
    @Inject
    private DocumentNotesResource documentNotes;
        
    @Inject
    private DocumentDao documentDao;
    
    @Inject 
    private NoteDao noteDao;
    
    @Inject
    private UserDao userDao;
    
    private Note note;
    
    private Note createNote(User editor) {
        Note note = new Note();
        note.setTerm(new Term());
        note.setAllEditors(Sets.newHashSet(editor));
        note.setLastEditedBy(editor);
        return note;
    }
    
    @Before
    public void setUp() {
        User editor = new User();
        editor.setUsername("test"+System.currentTimeMillis());
        userDao.save(editor);
        
        note  = createNote(editor);
        noteDao.save(note);
    }
    
    @Test
    public void GetById() {
        Document document = documentDao.getDocumentForPath(testDocument);
        DocumentNote documentNote = noteDao.createDocumentNote(note, document, 
                "l\u00E4htee h\u00E4ihins\u00E4 Mikko Vilkastuksen");
        assertNotNull(documentNotes.getById(documentNote.getId()));
    }
    
    @Test
    public void Create() {
        Document document = documentDao.getDocumentForPath(testDocument);
        DocumentNoteTO info = new DocumentNoteTO();
        info.setDocument(document.getId());
        info.setFullSelection("a");
        info.setNote(note.getId());
        DocumentNoteTO created = documentNotes.create(info);
        
        assertNotNull(created.getId());
    }
    
    @Test
    @Ignore
    public void Create_Selection() throws IOException, NoteAdditionFailedException {
        String PREFIX = "TEI-text0-body0-";
        Document document = documentDao.getDocumentForPath(testDocument);
        String element = "div0-div1-sp1-p0";
        String text = "sun ullakosta ottaa";
        
        SelectionTO selection = new SelectionTO();
        selection.setNoteId(note.getId());
        selection.setDocumentId(document.getId());
        selection.setText(new SelectedText(PREFIX + element, PREFIX + element, 1, 4, text));

        DocumentNoteTO created = documentNotes.create(selection);
        
        assertNotNull(created.getId());
    }

}
