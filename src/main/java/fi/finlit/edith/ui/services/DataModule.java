/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.providers.dao.SaltSource;
import org.springframework.security.providers.encoding.PasswordEncoder;

import fi.finlit.edith.domain.Profile;
import fi.finlit.edith.domain.User;
import fi.finlit.edith.ui.services.svn.SubversionService;

/**
 * DataModule provides
 *
 * @author tiwe
 * @version $Id$
 */
public final class DataModule {
    private DataModule() {}

    private static final Logger logger = LoggerFactory.getLogger(DataModule.class);

    public static void contributeSeedEntity(
            OrderedConfiguration<Object> configuration,
            SaltSource saltSource,
            PasswordEncoder passwordEncoder,
            @Inject SubversionService subversionService) throws IOException {

        logger.info("Initializing DataModule");

        subversionService.initialize();

        // users
        addUsers(configuration, saltSource, passwordEncoder);
    }

    @SuppressWarnings("unchecked")
    private static void addUsers(OrderedConfiguration<Object> configuration,
            SaltSource saltSource, PasswordEncoder passwordEncoder) throws IOException {
        List<String> lines = IOUtils.readLines(DataModule.class.getResourceAsStream("/users.csv"));
        for (String line : lines){
            String[] values = line.split(";");
            User user = new User();
            user.setUsername(values[0].toLowerCase(Locale.getDefault()));
            user.setEmail(values[2]);
            user.setFirstName(values[0]);
            user.setLastName(values[1]);
            if (values[2].endsWith("mysema.com")){
                user.setProfile(Profile.Admin);
            }else{
                user.setProfile(Profile.User);
            }

            // encode password
            UserDetailsImpl userDetails = new UserDetailsImpl(
                    user.getUsername(), user.getPassword(),
                    user.getProfile().getAuthorities());
            String password = passwordEncoder.encodePassword(user.getUsername(),saltSource.getSalt(userDetails));
            user.setPassword(password);

            configuration.add("user-" + user.getUsername(), user);
        }
    }

}
