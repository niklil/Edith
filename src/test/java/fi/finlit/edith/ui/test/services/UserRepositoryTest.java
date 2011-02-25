/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.test.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.junit.Test;

import fi.finlit.edith.domain.User;
import fi.finlit.edith.ui.services.AuthService;
import fi.finlit.edith.ui.services.UserRepository;

public class UserRepositoryTest extends AbstractServiceTest {

    @Inject
    private UserRepository userRepo;

    @Inject
    private AuthService authService;

    @Test
    public void GetByUsername() {
        for (String username : Arrays.asList("timo", "lassi", "heli", "sakari", "ossi")) {
            assertNotNull(userRepo.getByUsername(username));
        }
    }

    @Test
    public void GetCurrentUser() {
        assertEquals(authService.getUsername(), userRepo.getCurrentUser().getUsername());
    }

    @Test
    public void GetOrderedByName() {
        List<User> users = userRepo.getOrderedByName();
        User previous = null;
        for (User user : users) {
            if (previous != null) {
                assertTrue(previous.getUsername().compareTo(user.getUsername()) <= 0);
            }
            previous = user;
        }
    }

    @Test
    public void GetUserInfoByUsername() {
        for (String username : Arrays.asList("timo", "lassi", "heli", "sakari", "ossi")) {
            assertNotNull(userRepo.getUserInfoByUsername(username));
        }
    }
}
