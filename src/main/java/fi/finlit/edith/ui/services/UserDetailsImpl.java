/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package fi.finlit.edith.ui.services;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;

public class UserDetailsImpl implements UserDetails{

    private static final long serialVersionUID = -3810708516049551503L;

    private final String username;

    private String password;

    private final boolean nonExpired = true, nonLocked = true, enabled = true;

    private final GrantedAuthority[] authorities;

    public UserDetailsImpl(String username, String password, GrantedAuthority... auth){
        this.username = username;
        this.password = password;
        this.authorities = auth;
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return nonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return nonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return nonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
