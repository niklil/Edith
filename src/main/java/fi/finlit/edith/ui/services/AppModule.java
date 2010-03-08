/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import java.io.IOException;

import nu.localhost.tapestry5.springsecurity.services.RequestInvocationDefinition;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.services.AliasContribution;
import org.apache.tapestry5.services.ComponentClassResolver;
import org.apache.tapestry5.urlrewriter.URLRewriterRule;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.providers.encoding.ShaPasswordEncoder;
import org.springframework.security.userdetails.UserDetailsService;

import com.mysema.tapestry.PageMappingRule;

/**
 * AppModule is the main Tapestry module for the Edith application
 *
 * @author tiwe
 * @version $Id$
 */
@SubModule( { DataModule.class, ServiceModule.class })
public final class AppModule {

    private AppModule() {}

    public static void contributeApplicationDefaults(
            MappedConfiguration<String, String> configuration) throws IOException {
        // general config
        configuration.add(SymbolConstants.SUPPORTED_LOCALES, "fi,en,sv,de");
        configuration.add(SymbolConstants.PRODUCTION_MODE, System.getProperty("production.mode", "false"));
        configuration.add(SymbolConstants.APPLICATION_VERSION, "0.1.2");

        // Spring Security config
        configuration.add("spring-security.loginform.url", "/login");
//        configuration.add("spring-security.force.ssl.login", "true");
        configuration.add("spring-security.check.url", "/security_check");
        configuration.add("spring-security.failure.url", "/loginfailed");
    }

    public static void contributeFilterSecurityInterceptor(
            Configuration<RequestInvocationDefinition> configuration) {

        // Login and about page are the only ones allowed for anonymous users
        configuration.add(new RequestInvocationDefinition("/loginfailed", "ROLE_ANONYMOUS"));
        configuration.add(new RequestInvocationDefinition("/security_check", "ROLE_ANONYMOUS"));
        configuration.add(new RequestInvocationDefinition("/login", "ROLE_ANONYMOUS"));
        configuration.add(new RequestInvocationDefinition("/favicon.ico", "ROLE_ANONYMOUS,ROLE_USER"));
        configuration.add(new RequestInvocationDefinition("/about", "ROLE_ANONYMOUS,ROLE_USER"));
        configuration.add(new RequestInvocationDefinition("/assets/**", "ROLE_ANONYMOUS,ROLE_USER"));
        configuration.add(new RequestInvocationDefinition("/**", "ROLE_USER"));
    }

    public static void contributeURLRewriter(
            OrderedConfiguration<URLRewriterRule> configuration,
            ComponentClassResolver componentResolver) {
        // strip "page" suffix off from page names
        configuration.add("pageMapping", new PageMappingRule(componentResolver));
    }

    public static void bind(ServiceBinder binder){
        binder.bind(UserDetailsService.class, UserDetailsServiceImpl.class);
    }

    public static void contributeAlias(Configuration<AliasContribution<PasswordEncoder>> configuration ) {
        configuration.add( AliasContribution.create(PasswordEncoder.class, new ShaPasswordEncoder() ) );
    }

    public static void contributeProviderManager(
            OrderedConfiguration<AuthenticationProvider> configuration,
            @InjectService("DaoAuthenticationProvider") AuthenticationProvider daoAuthenticationProvider) {
        configuration.add("daoAuthenticationProvider", daoAuthenticationProvider);
    }

}
