/*
 * Copyright (c) 2012 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.edith.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.mysema.edith.domain.Document;
import com.mysema.edith.domain.DocumentNote;
import com.mysema.edith.dto.DocumentInfo;
import com.mysema.edith.dto.DocumentNoteInfo;
import com.mysema.edith.services.DocumentDao;
import com.mysema.edith.services.DocumentNoteDao;
import com.mysema.edith.services.SubversionService;

@Transactional
@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentsResource extends AbstractResource<DocumentInfo>{

    private final DocumentDao dao;

    private final DocumentNoteDao documentNoteDao;

    private final SubversionService subversionService;

    @Inject
    public DocumentsResource(
            DocumentDao dao,
            DocumentNoteDao documentNoteDao,
            SubversionService subversionService) {
        this.dao = dao;
        this.documentNoteDao = documentNoteDao;
        this.subversionService = subversionService;
    }

    @Override
    @GET @Path("{id}")
    public DocumentInfo getById(@PathParam("id") Long id) {
        return convert(dao.getById(id), new DocumentInfo());
    }

    @GET @Path("{id}/document-notes")
    public List<DocumentNoteInfo> getDocumentNotes(@PathParam("id") Long id) {
        List<DocumentNote> docNotes = documentNoteDao.getOfDocument(id);
        List<DocumentNoteInfo> result = new ArrayList<DocumentNoteInfo>(docNotes.size());
        for (DocumentNote docNote : docNotes) {
            result.add(convert(docNote, new DocumentNoteInfo()));
        }
        return result;
    }

    @Override
    @POST
    public DocumentInfo update(DocumentInfo info) {
        Document entity = dao.getById(info.getId());
        if (entity != null) {
            dao.save(convert(info, entity));
        }
        return info;
    }

    @Override
    @PUT
    public DocumentInfo add(DocumentInfo info) {
        dao.save(convert(info, new Document()));
        return info;
    }

    @Override
    @DELETE @Path("{id}")
    public void delete(@PathParam("id") Long id) {
        dao.remove(id);
    }

    // TODO addDocumentsFromZip

    // TODO document rendering
    @GET
    @Path("{id}/raw")
    public String getRawDocument(@PathParam("id") Long id) throws IOException {
        return IOUtils.toString(dao.getDocumentStream(dao.getById(id)));
    }
}
