/*
 * Copyright (c) 2012 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.edith.guice;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;
import com.mysema.edith.web.DocumentNotesResource;
import com.mysema.edith.web.DocumentsResource;
import com.mysema.edith.web.NotesResource;
import com.mysema.edith.web.PersonsResource;
import com.mysema.edith.web.PlacesResource;
import com.mysema.edith.web.TermsResource;
import com.mysema.edith.web.UsersResource;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class WebModule extends ServletModule {
    
    @Override
    protected void configureServlets() {
        // bind resource classes here
        bind(DocumentNotesResource.class).in(Scopes.SINGLETON);
        bind(DocumentsResource.class).in(Scopes.SINGLETON);
        bind(NotesResource.class).in(Scopes.SINGLETON);
        bind(PersonsResource.class).in(Scopes.SINGLETON);
        bind(PlacesResource.class).in(Scopes.SINGLETON);
        bind(TermsResource.class).in(Scopes.SINGLETON);        
        bind(UsersResource.class).in(Scopes.SINGLETON);
        
        bind(GuiceContainer.class);
        bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
        serve("/api/*").with(GuiceContainer.class);
    }
    
}
