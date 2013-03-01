/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.edith.services;

import java.util.List;

import com.google.inject.persist.Transactional;
import com.mysema.edith.domain.QTerm;
import com.mysema.edith.domain.Term;

@Transactional
public class TermDaoImpl extends AbstractDao<Term> implements TermDao {

    private static final QTerm term = QTerm.term;
    
    @Override
    public List<Term> findByStartOfBasicForm(String partial, int maxResults) {
        return query().from(term)
                .where(term.basicForm.startsWith(partial)).limit(maxResults)
                .list(term);
    }

    @Override
    public void remove(Long id) {
        Term term = getById(id);
        remove(term);
    }

    @Override
    public Term getById(Long id) {
        return query().from(term).where(term.id.eq(id)).uniqueResult(term);
    }

    @Override
    public void save(Term term) {
        persist(term);
    }

}