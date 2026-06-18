package com.zbor.exceptions;

public class UserNotFoundException extends ZborException {
    public UserNotFoundException(Long id) {
        super("User not found: " + id);
    }
}