/*
 * Copyright (c) 2018 Mysema
 */
package com.mysema.edith.services;

import javax.persistence.EntityManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.edith.Identifiable;
import com.mysema.query.jpa.HQLTemplates;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;

public abstract class AbstractDao<T extends Identifiable> implements Dao<T, Long> {

    @Inject
    private Provider<EntityManager> em;

    protected JPAQuery query() {
        return new JPAQuery(em.get(), HQLTemplates.DEFAULT);
    }
    
    protected JPAQuery from(EntityPath<?> entity) {
        return query().from(entity);
    }
    
    protected JPADeleteClause delete(EntityPath<?> entity) {
        return new JPADeleteClause(em.get(), entity, HQLTemplates.DEFAULT);
    }

    protected void detach(Object entity) {
        em.get().detach(entity);
    }

    protected <E> E find(Class<E> type, Long id) {
        return em.get().find(type, id);
    }

    protected void persist(Object entity) {
        em.get().persist(entity);
    }

    protected <E> E merge(E entity) {
        return em.get().merge(entity);
    }

    protected <E extends Identifiable> E persistOrMerge(E entity) {
        if (entity.getId() != null) {
            return merge(entity);
        }
        persist(entity);
        return entity;
    }
    
    protected void remove(Object entity) {
        em.get().remove(entity);
    }


}