package com.campushub.support.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {

    private final Long id;
    private final String token;

    public CustomUserDetails(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities, String token) {
        super(username, password, authorities);
        this.id = id;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
}
