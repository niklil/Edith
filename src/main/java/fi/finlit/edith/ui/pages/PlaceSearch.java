/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.pages;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.RenderSupport;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.IncludeJavaScriptLibrary;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.mysema.tapestry.core.Context;

import fi.finlit.edith.domain.Place;
import fi.finlit.edith.ui.services.NoteRepository;
import fi.finlit.edith.ui.services.PlaceRepository;

@SuppressWarnings("unused")
@IncludeJavaScriptLibrary( { "classpath:jquery-1.4.1.js", "deleteDialog.js" })
public class PlaceSearch {

    @Property
    private String searchTerm;

    private Context context;

    @Property
    private GridDataSource places;

    @Property
    private Place place;

    @Inject
    private NoteRepository noteRepository;

    @Inject
    private PlaceRepository placeRepository;

    @Environmental
    private RenderSupport support;

    void onActivate(EventContext ctx) {
        if (ctx.getCount() >= 1) {
            searchTerm = ctx.get(String.class, 0);
        }
        context = new Context(ctx);
    }

    void onSuccessFromSearch() {
        context = new Context(searchTerm);
    }

    public void setupRender() {
        places = noteRepository.queryPlaces(searchTerm == null ? "*" : searchTerm);
    }

    Object onPassivate() {
        return context == null ? null : context.toArray();
    }

    void onActionFromDelete(String placeId) {
        placeRepository.remove(placeId);
    }

}