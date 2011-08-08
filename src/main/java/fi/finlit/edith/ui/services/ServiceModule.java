/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.finlit.edith.ui.services.content.ContentRenderer;
import fi.finlit.edith.ui.services.content.ContentRendererImpl;
import fi.finlit.edith.ui.services.svn.SubversionService;
import fi.finlit.edith.ui.services.svn.SubversionServiceImpl;

/**
 * ServiceModule provides service bindings and RDFBean configuration elements
 *
 * @author tiwe
 * @version $Id$
 *
 */
public final class ServiceModule {

    private static final Logger logger = LoggerFactory.getLogger(HibernateDataModule.class);

    public static void bind(ServiceBinder binder) {
        // services
        binder.bind(SubversionService.class, SubversionServiceImpl.class);
        binder.bind(ContentRenderer.class, ContentRendererImpl.class);
        binder.bind(AuthService.class, SpringSecurityAuthService.class);
    }

    @Startup
    public static void initData(SubversionService subversionService) {
        logger.info("Starting up subversion");
        subversionService.initialize();
    }

    public static void contributeApplicationDefaults(
            MappedConfiguration<String, String> configuration) throws IOException {
        // app config
        Properties properties = new Properties();

        InputStream stream = null;
        try {
            stream = ServiceModule.class.getResourceAsStream("/edith.properties");
            properties.load(stream);
            if (properties.getProperty(SymbolConstants.APPLICATION_VERSION) == null) {
                configuration.add(SymbolConstants.APPLICATION_VERSION,
                        String.valueOf(Calendar.getInstance().getTimeInMillis()));
            }
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                configuration.add(entry.getKey().toString(), entry.getValue().toString());
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private ServiceModule() {
    }
}