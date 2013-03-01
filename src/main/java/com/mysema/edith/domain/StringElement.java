/*
 * Copyright (c) 2012 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.edith.domain;

public class StringElement implements ParagraphElement {

    private final String string;

    public StringElement(String string) {
        this.string = string;
    }

    @Override
    public ParagraphElement copy() {
        return new StringElement(string);
    }

    @Override
    public String toString() {
        return string;
    }
}