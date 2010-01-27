/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package fi.finlit.edith.ui.services;

import static fi.finlit.edith.domain.QDocument.document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.tmatesoft.svn.core.SVNException;

import com.mysema.rdfbean.dao.AbstractRepository;

import fi.finlit.edith.EDITH;
import fi.finlit.edith.domain.Document;
import fi.finlit.edith.domain.DocumentRepository;
import fi.finlit.edith.domain.DocumentRevision;
import fi.finlit.edith.domain.Note;
import fi.finlit.edith.domain.NoteRepository;

/**
 * DocumentRepositoryImpl provides
 *
 * @author tiwe
 * @version $Id$
 */
public class DocumentRepositoryImpl extends AbstractRepository<Document> implements DocumentRepository{

    private static final String ENCODING = "UTF-8";
    
    private final String documentRoot;
    
    private final SubversionService svnService;
    
    private final NoteRepository noteRepository;
    
    public DocumentRepositoryImpl(
            @Inject @Symbol(EDITH.SVN_DOCUMENT_ROOT) String documentRoot,
            @Inject SubversionService svnService,
            @Inject NoteRepository noteRepository) throws SVNException {
        super(document);
        this.documentRoot = documentRoot;
        this.svnService = svnService;
        this.noteRepository = noteRepository;
    }

    @Override
    public void addDocument(String svnPath, File file){
        svnService.importFile(svnPath, file);        
    }
    
    private Document createDocument(String path, String title, String description){
        Document document = new Document();
        document.setSvnPath(path);
        document.setTitle(title);
        document.setDescription(description);
        return save(document);
    }    

    @Override
    public Collection<Document> getAll() {        
        return getDocumentsOfFolder(documentRoot);
    }
    
    @Override
    public File getDocumentFile(DocumentRevision document) throws IOException {
        return svnService.getFile(document.getSvnPath(), document.getRevision());
    }
    
    @Override
    public Document getDocumentForPath(String svnPath) {
        Document document = getDocumentMetadata(svnPath);
        if (document == null){
            document = createDocument(svnPath, svnPath.substring(svnPath.lastIndexOf('/')+1), null);
        }
        return document;
    }
    
    private Document getDocumentMetadata(String svnPath){
        return getSession().from(document)
            .where(document.svnPath.eq(svnPath))
            .uniqueResult(document);
    }
    
    @Override
    public List<Document> getDocumentsOfFolder(String svnFolder) {
        Collection<String> entries = svnService.getEntries(svnFolder, /* HEAD */ -1);
        List<Document> documents = new ArrayList<Document>(entries.size());
        for (String entry : entries){
            String path = svnFolder + "/" + entry;
            Document document = getDocumentMetadata(path);
            if (document == null){
                document = createDocument(path, entry, null);
            }
            documents.add(document);
        }
        return documents;
    }

    @Override
    public List<Long> getRevisions(Document document){
        return svnService.getRevisions(document.getSvnPath());
    }
    
    @Override
    public void remove(Document document){
        svnService.delete(document.getSvnPath());  
    }

    @Override
    public Note addNote(Document doc, long revision, String startId, String endId, String text) throws IOException {               
        File docFile = svnService.getFile(doc.getSvnPath(), revision);            
        // TODO
        String localId = "XXX";
        
        File tempFile = File.createTempFile("tei", null);
        addNote(docFile, tempFile, startId, endId, text);
        
        
        // SVN update on top of the just serialized file
        svnService.update(doc.getSvnPath(), tempFile);
        
        // SVN commit the file
        long newRevision = svnService.commit(doc.getSvnPath(), tempFile);
        
        // persisted noteRevision has svnRevision of newly created commit
        Note note = noteRepository.createNote(doc, newRevision, localId, text, text); 
        return note;
    }

    public void addNote(File source, File target, String startId, String endId, String text) {
        // TODO Auto-generated method stub
        
    }

}
