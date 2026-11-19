package me.tgoyal.clearance.domain;

/** A component can be decided once. Changing a decision would rewrite the record. */
public class AlreadyDecidedException extends RuntimeException {

    public AlreadyDecidedException(String message) {
        super(message);
    }
}
