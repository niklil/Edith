package fi.finlit.edith.ui.services;

import java.util.Arrays;

import nu.localhost.tapestry5.springsecurity.services.internal.SaltSourceImpl;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.providers.encoding.ShaPasswordEncoder;

import fi.finlit.edith.domain.Profile;
import fi.finlit.edith.domain.User;

/**
 * DataModule provides
 *
 * @author tiwe
 * @version $Id$
 */
public class DataModule {
    
    public static void contributeSeedEntity(OrderedConfiguration<Object> configuration) throws Exception {
        PasswordEncoder passwordEncoder = new ShaPasswordEncoder();
        
        SaltSourceImpl saltSource = new SaltSourceImpl();
        saltSource.setSystemWideSalt("DEADBEEF");
        saltSource.afterPropertiesSet();
     
        // users
        for (String email : Arrays.asList(
                "timo.westkamper@mysema.com",
                "lassi.immonen@mysema.com",
                "heli.kautonen@finlit.fi",
                "matti.anttila@finlit.fi",
                "sakari.katajamaki@finlit.fi",
                "ossi.kokko@finlit.fi")){            
            String firstName = StringUtils.capitalize(email.substring(0, email.indexOf('.')));
            String lastName = StringUtils.capitalize(email.substring(firstName.length() + 1, email.indexOf('@')));
            
            User user = new User();
            user.setUsername(firstName.toLowerCase());
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setProfile(Profile.User);
            
            UserDetailsImpl userDetails = new UserDetailsImpl(
                    user.getUsername(), user.getPassword(), 
                    user.getProfile().getAuthorities());
            String password = passwordEncoder.encodePassword(user.getUsername(),saltSource.getSalt(userDetails));
            user.setPassword(password);
            configuration.add("user-" + user.getUsername(), user);
        }     
        
        // 
        
    }  

}