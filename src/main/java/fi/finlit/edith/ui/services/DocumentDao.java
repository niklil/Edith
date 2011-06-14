/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import fi.finlit.edith.domain.DocumentNote;
import fi.finlit.edith.domain.Note;
import fi.finlit.edith.dto.SelectedText;
import fi.finlit.edith.sql.domain.Document;
import fi.finlit.edith.ui.services.svn.FileItemWithDocumentId;
import fi.finlit.edith.ui.services.svn.RevisionInfo;

@Transactional
public interface DocumentDao extends Dao<Document, String> {

    /**
     * Import the given File to the given svnPath
     *
     * @param svnPath
     * @param file
     */
    void addDocument(String svnPath, File file);

    /**
     * Import documents from the given ZIP file
     *
     * @param parentSvnPath
     * @param file
     * @return amount of imported documents
     */
    int addDocumentsFromZip(String parentSvnPath, File file);

    /**
     * Add the given note for the given Document
     *
     * @param docRevision
     * @param selection
     * @return
     * @throws IOException
     * @throws NoteAdditionFailedException
     */
    DocumentNote addNote(Note note, Document document, SelectedText selection) throws IOException, NoteAdditionFailedException;

    /**
     * Get a Document handle for the given path or create a new one if none could be found
     *
     * @param svnPath
     * @return
     */
    Document getOrCreateDocumentForPath(String svnPath);

    /**
     * Get a Document handle for the given path
     *
     * @param svnPath
     * @return
     */
    Document getDocumentForPath(String svnPath);

    /**
     * Get the Documents of the given directory path and its subpaths
     *
     * @param svnFolder
     * @return
     */
    List<Document> getDocumentsOfFolder(String svnFolder);

    /**
     * Get the file for the given document for reading
     *
     * @param docRevision
     * @return
     * @throws IOException
     */
    InputStream getDocumentStream(Document document) throws IOException;

    /**
     * Get the SVN revisions for the given document in ascending order
     *
     * @param document
     * @return
     */
    List<RevisionInfo> getRevisions(Document document);

    /**
     * Remove all notes from the given Document
     *
     * @param document
     * @return
     * @throws IOException
     */
    @Deprecated //Move to service
    void removeAllNotes(Document document);

    /**
     * Remove the given anchors from the given Document
     *
     * @param docRevision
     * @param notes
     * @throws IOException
     */
    @Deprecated //Move to service
    void removeNotes(Document document, DocumentNote... notes);

    /**
     * Permanently removes document notes and their anchors.
     *
     * @param docRevision
     * @param notes
     * @return
     */
    @Deprecated //Move to service
    void removeNotesPermanently(Document document, DocumentNote... notes);

    /**
     * Update the boundaries of the given note
     *
     * @param note
     * @param selection
     * @throws IOException
     */
    @Deprecated //Move to service
    DocumentNote updateNote(DocumentNote note, SelectedText selection) throws IOException;

    /**
     * Remove the given document
     *
     * @param doc
     */
    void remove(Document doc);

    /**
     * Remove the document by id.
     */
    void remove(Long id);

    /**
     * Remove the given documents
     *
     * @param documents
     */
    void removeAll(Collection<Document> documents);

    void move(Long id, String newPath);

    void rename(Long id, String newPath);

    List<FileItemWithDocumentId> fromPath(String path, Long id);

}
