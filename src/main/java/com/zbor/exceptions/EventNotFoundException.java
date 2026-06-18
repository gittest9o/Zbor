package com.zbor.exceptions;

public class EventNotFoundException extends ZborException {
    public EventNotFoundException(Long id) {
        super("Event not found: " + id);
    }
}