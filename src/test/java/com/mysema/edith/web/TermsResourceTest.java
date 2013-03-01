package com.mysema.edith.web;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.google.inject.Inject;
import com.mysema.edith.domain.Term;
import com.mysema.edith.domain.TermLanguage;
import com.mysema.edith.dto.TermInfo;
import com.mysema.edith.services.TermDao;

public class TermsResourceTest extends AbstractResourceTest {

    @Inject
    private TermDao termDao;
    
    @Inject
    private TermsResource terms;
    
    @Test
    public void GetById() {       
        Term term = new Term();
        term.setBasicForm("a");
        term.setLanguage(TermLanguage.ENGLISH);
        term.setMeaning("b");
        term.setOtherLanguage("fi");
        termDao.save(term);
        
        assertNotNull(terms.getById(term.getId()));
    }
    
    @Test
    public void Add() {
        TermInfo term = new TermInfo();
        term.setBasicForm("a");
        term.setLanguage(TermLanguage.ENGLISH);
        term.setMeaning("b");
        term.setOtherLanguage("fi");
        terms.add(term);
    }
    
}