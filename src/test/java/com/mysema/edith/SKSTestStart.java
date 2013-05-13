/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.edith;

import java.io.File;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;

import com.mysema.commons.jetty.JettyHelper;

public class SKSTestStart {

    public static void main(String[] args) throws Exception{
        FSRepositoryFactory.setup();
        File svnRepo = new File("target/repo");

        System.setProperty("org.mortbay.jetty.webapp.parentLoaderPriority", "true");
        System.setProperty("production.mode", "true");
        System.setProperty(EDITH.REPO_FILE_PROPERTY, svnRepo.getAbsolutePath());
        System.setProperty(EDITH.REPO_URL_PROPERTY, SVNURL.fromFile(svnRepo).toString());
        
        System.setProperty(EDITH.EXTENDED_TERM, "false");
        
        JettyHelper.startJetty("src/main/webapp", "/", 8080, 8443);
    }

}