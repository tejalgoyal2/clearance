package me.tgoyal.clearance.service;

/** The requested scan or component does not exist. Becomes HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
