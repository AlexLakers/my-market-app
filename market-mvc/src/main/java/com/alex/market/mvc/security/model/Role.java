package com.alex.market.mvc.security.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ANONYMOUS,
    USER;

    @Override
    public String getAuthority() {
        return name();
    }
}
