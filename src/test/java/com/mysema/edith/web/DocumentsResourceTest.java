package com.mysema.edith.web;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mysema.edith.EdithTestConstants;
import com.mysema.edith.domain.Document;
import com.mysema.edith.dto.DocumentTO;
import com.mysema.edith.services.DocumentDao;

public class DocumentsResourceTest extends AbstractResourceTest {
    
    @Inject @Named(EdithTestConstants.TEST_DOCUMENT_KEY)
    private String testDocument;
    
    @Inject
    private DocumentDao documentDao;
    
    @Inject
    private DocumentsResource documents;
    
    @Test
    public void GetById() {       
        Document document = documentDao.getDocumentForPath(testDocument);
        assertNotNull(documents.getById(document.getId()));
    }
    
    @Test
    public void Add() {
        DocumentTO doc = new DocumentTO();
        doc.setPath("abc" + System.currentTimeMillis());
        doc.setTitle("title");
        DocumentTO created = documents.create(doc);
        
        assertNotNull(created.getClass());
    }

}
