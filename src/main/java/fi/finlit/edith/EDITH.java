/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package fi.finlit.edith;


public final class EDITH {
    
    private EDITH(){}
    
    private static final String BASE = "http://www.finlit.fi/semantics/edith/";
    
    public static final String NS = BASE + "#";
    
    public static final String DATA = BASE + "data#";
    
    public static final String REPO_URL_PROPERTY = "svn.repo.url";
    
    public static final String REPO_FILE_PROPERTY = "svn.repo.file";
    
    public static final String SVN_DOCUMENT_ROOT = "svn.document.root";
    
    public static final String SVN_CACHE_DIR = "svn.cache.dir";

}
