/*
 * Copyright (c) 2018 Mysema
 */
package com.mysema.edith.guice;

import static com.mysema.edith.EdithTestConstants.NOTE_TEST_DATA_KEY;
import static com.mysema.edith.EdithTestConstants.TEST_DOCUMENT_CONTENT_KEY;
import static com.mysema.edith.EdithTestConstants.TEST_DOCUMENT_FILE_KEY;
import static com.mysema.edith.EdithTestConstants.TEST_DOCUMENT_KEY;

import java.io.File;
import java.util.Map;

import org.tmatesoft.svn.core.SVNURL;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mysema.edith.EDITH;
import com.mysema.edith.services.AuthService;
import com.mysema.edith.services.StaticAuthService;

public class ServiceTestModule extends ServiceModule {
    
    @Override
    protected void configure() {
        super.configure();
        bind(AuthService.class, StaticAuthService.class);
    }
    
    @Override
    protected Map<String, Object> getProperties() throws Exception {
        Map<String, Object> properties = super.getProperties();
        File svnRepo = new File("target/repo");
        properties.put(NOTE_TEST_DATA_KEY, new File("etc/demo-material/notes/nootit.xml"));
        File testDocumentFile = new File("etc/demo-material/tei/Nummisuutarit rakenteistettuna.xml");
        properties.put(TEST_DOCUMENT_FILE_KEY, testDocumentFile);
        properties.put(TEST_DOCUMENT_CONTENT_KEY, Files.toString(testDocumentFile, Charsets.UTF_8));
        properties.put(TEST_DOCUMENT_KEY, "/documents/trunk/Nummisuutarit rakenteistettuna.xml");
        properties.put(EDITH.REPO_FILE_PROPERTY, svnRepo.getAbsolutePath());
        properties.put(EDITH.REPO_URL_PROPERTY, SVNURL.fromFile(svnRepo).toString());       
        return properties;
    }

//    public static void contributeApplicationDefaults(
//            MappedConfiguration<String, String> configuration) throws IOException, SVNException {
//        File svnRepo = new File("target/repo");
//        configuration.add(NOTE_TEST_DATA_KEY, "etc/demo-material/notes/nootit.xml");
//        File testDocumentFile = new File("etc/demo-material/tei/Nummisuutarit rakenteistettuna.xml");
//        configuration.add(TEST_DOCUMENT_FILE_KEY, testDocumentFile.getPath());
//        configuration.add(TEST_DOCUMENT_CONTENT_KEY, FileUtils.readFileToString(testDocumentFile, "UTF-8"));
//        configuration.add(TEST_DOCUMENT_KEY, "/documents/trunk/Nummisuutarit rakenteistettuna.xml");
//        configuration.add(EDITH.REPO_FILE_PROPERTY, svnRepo.getAbsolutePath());
//        configuration.add(EDITH.REPO_URL_PROPERTY, SVNURL.fromFile(svnRepo).toString());        
//    }
//
//    public static SaltSource buildSaltSource() throws Exception {
//        SaltSourceImpl saltSource = new SaltSourceImpl();
//        saltSource.setSystemWideSalt("DEADBEEF");
//        saltSource.afterPropertiesSet();
//        return saltSource;
//    }
//        
//    public static Response buildResponse() {
//        return new TestableResponseImpl();
//    }
//
//    public static void contributeServiceOverride(MappedConfiguration<Class<?>, Object> configuration) {
//        AuthService authService = new StaticAuthService();
//        configuration.add(AuthService.class, authService);
//    }
//    
//    public static PasswordEncoder buildPaswordEncoder() {
//        return new ShaPasswordEncoder();
//    }

}
