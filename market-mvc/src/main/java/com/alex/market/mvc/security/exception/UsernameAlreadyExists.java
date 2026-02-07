package com.alex.market.mvc.security.exception;

public class UsernameAlreadyExists extends RuntimeException {
    private final String MESSAGE_PATTERN="Username %s already exists";

    public UsernameAlreadyExists(String username) {
        super("Username " + username + " already exists");
    }
    public UsernameAlreadyExists(String username, Throwable cause) {
        super("Username " + username + " already exists", cause);
    }
}
