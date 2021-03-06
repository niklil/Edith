/*
 * Copyright (c) 2018 Mysema
 */
package com.mysema.edith.services;

import java.util.List;

import com.mysema.edith.domain.Person;

/**
 * @author tiwe
 *
 */
public interface PersonDao extends Dao<Person, Long> {

    /**
     * @param partial
     * @param limit
     * @return
     */
    List<Person> findByStartOfFirstAndLastName(String partial, int limit);

    /**
     * @param personId
     */
    void remove(Long personId);

    /**
     * @param person
     */
    Person save(Person person);

}
