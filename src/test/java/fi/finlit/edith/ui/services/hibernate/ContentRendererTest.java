/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services.hibernate;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.internal.services.MarkupWriterImpl;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.junit.Test;

import fi.finlit.edith.sql.domain.DocumentNote;
import fi.finlit.edith.sql.domain.Interval;
import fi.finlit.edith.sql.domain.LinkElement;
import fi.finlit.edith.sql.domain.NameForm;
import fi.finlit.edith.sql.domain.Note;
import fi.finlit.edith.sql.domain.NoteFormat;
import fi.finlit.edith.sql.domain.Paragraph;
import fi.finlit.edith.sql.domain.Person;
import fi.finlit.edith.sql.domain.Place;
import fi.finlit.edith.sql.domain.StringElement;
import fi.finlit.edith.sql.domain.UrlElement;
import fi.finlit.edith.ui.services.DocumentDao;
import fi.finlit.edith.ui.services.content.ContentRenderer;

public class ContentRendererTest extends AbstractHibernateTest {

    private static final String doc1 = "/documents/trunk/Nummisuutarit rakenteistettuna.xml";

    private static final String doc2 = "/documents/trunk/Nummisuutarit rakenteistettuna-annotoituna.xml";

    @Inject
    private ContentRenderer renderer;

    @Inject
    private DocumentDao documentDao;

    private final MarkupWriter writer = new MarkupWriterImpl();

    @Test
    public void renderDocument() throws Exception {
        renderer.renderDocument(documentDao.getOrCreateDocumentForPath(doc1), writer);
    }

    @Test
    public void renderDocumentAsXML() throws IOException, XMLStreamException{
        List<DocumentNote> docNotes = Arrays.asList(createDocumentNote(NoteFormat.NOTE), createDocumentNote(NoteFormat.PERSON));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderDocumentAsXML(documentDao.getOrCreateDocumentForPath(doc2), docNotes, out);
    }

    @Test
    public void renderPageLinks() throws Exception {
        renderer.renderPageLinks(documentDao.getOrCreateDocumentForPath(doc1),
                writer);
    }

    @Test
    public void renderDocumentWithNotes() throws Exception {
        renderer.renderDocument(documentDao.getOrCreateDocumentForPath(doc2), writer);
    }

    @Test
    public void renderDocumentNotesAsXML(){
        List<DocumentNote> docNotes = Arrays.asList(createDocumentNote(NoteFormat.NOTE), createDocumentNote(NoteFormat.PERSON));
        renderer.renderDocumentNotesAsXML(documentDao.getOrCreateDocumentForPath(doc2), docNotes, writer);
    }

    private DocumentNote createDocumentNote(NoteFormat noteFormat) {
        Note note = new Note();

        note.setLemma("taloon");
        note.setLemmaMeaning("johonkin ineen");
        Paragraph paragraph = new Paragraph();
        LinkElement element = new LinkElement("Kalevala");
        element.setReference("kalevala");
        paragraph.addElement(element);
        note.setSubtextSources(paragraph.toString());
        note.setDescription(paragraph.copy().toString());
        note.setSources(paragraph.copy().toString());
        note.setFormat(noteFormat);
        DocumentNote documentNote = new DocumentNote();
        documentNote.setNote(note);
        documentNote.setId(1234L);
        return documentNote;
    }

    @Test
    public void Render_Normal_Note() {
        List<DocumentNote> documentNotes = new ArrayList<DocumentNote>();
        documentNotes.add(createDocumentNote(NoteFormat.NOTE));
        renderer.renderDocumentNotes(documentNotes, writer);
        String document = writer.toString();
        assertTrue(document.startsWith("<ul class=\"notes\">"));
        assertTrue(document
                .contains("<li><a href=\"#start1234\" class=\"notelink\"><em>taloon</em></a>"));
        assertTrue(document.contains("'johonkin ineen', Vrt. <a href=\"http://www.google.com/kalevala\">Kalevala</a>"));
        assertTrue(document.contains("<a href=\"http://www.google.com/kalevala\">Kalevala</a>"));
        assertTrue(document, document
                .contains("(<a href=\"http://www.google.com/kalevala\">Kalevala</a>)</SPAN></li>"));
        assertTrue(document.endsWith("</ul>"));
        System.err.println(document);
    }

    @Test
    public void Render_Person_Note() {
        List<DocumentNote> documentNotes = new ArrayList<DocumentNote>();
        DocumentNote documentNote = createDocumentNote(NoteFormat.PERSON);
        Person person = new Person(new NameForm("Fred", "Armisen", null), new HashSet<NameForm>());
        person.setTimeOfBirth(Interval.createYear(1970));
        person.setTimeOfDeath(Interval.createYear(2098));
        documentNote.getNote().setPerson(person);
        documentNotes.add(documentNote);
        renderer.renderDocumentNotes(documentNotes, writer);
        String document = writer.toString();
        assertTrue(document.startsWith("<ul class=\"notes\">"));
        assertTrue(document
                .contains("<li><a href=\"#start1234\" class=\"notelink\"><em>taloon</em></a>"));
        assertTrue(document.contains("Fred Armisen"));
        assertTrue(document.contains("1970\u20132098."));
        assertTrue(document.contains("<span>\u2013</span>"));
        assertTrue(document.contains("<a href=\"http://www.google.com/kalevala\">Kalevala</a>"));
        assertTrue(document
                .contains("(<a href=\"http://www.google.com/kalevala\">Kalevala</a>)</SPAN></li>"));
        assertTrue(document.endsWith("</ul>"));
    }

    @Test
    public void Render_Place_Note() {
        List<DocumentNote> documentNotes = new ArrayList<DocumentNote>();
        DocumentNote documentNote = createDocumentNote(NoteFormat.PLACE);

        Paragraph description = Paragraph.parseSafe(documentNote.getNote().getDescription());
        description.addElement(new StringElement(" foo "));
        UrlElement urlElement = new UrlElement("Google");
        urlElement.setUrl("http://www.google.com/");
        description.addElement(urlElement);
        description.addElement(new UrlElement("happyness"));
        description.addElement(new LinkElement("maya"));
        documentNote.getNote().setDescription(description.toString());
        Place place = new Place(new NameForm("New York", null), new HashSet<NameForm>());
        documentNote.getNote().setPlace(place);
        documentNotes.add(documentNote);
        renderer.renderDocumentNotes(documentNotes, writer);
        String document = writer.toString();
//        System.err.println(document);
        assertTrue(document.startsWith("<ul class=\"notes\">"));
        assertTrue(document
                .contains("<li><a href=\"#start1234\" class=\"notelink\"><em>taloon</em></a>"));
        assertTrue(document.contains("New York"));
        assertTrue(document.contains("<span>\u2013</span>"));
        assertTrue(document
                .contains("<a href=\"http://www.google.com/kalevala\">Kalevala</a> foo <a href=\"http://www.google.com/\">Google</a>"));
        assertTrue(document.contains("<a>happyness</a>"));
        assertTrue(document.contains("<a>maya</a>"));
        assertTrue(document
                .contains("(<a href=\"http://www.google.com/kalevala\">Kalevala</a>)</SPAN></li>"));
        assertTrue(document.endsWith("</ul>"));
    }
}