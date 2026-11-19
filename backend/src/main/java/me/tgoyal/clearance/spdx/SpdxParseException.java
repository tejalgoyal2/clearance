package me.tgoyal.clearance.spdx;

/** Thrown when an SPDX expression is malformed. The message says what was wrong and where. */
public class SpdxParseException extends RuntimeException {

    public SpdxParseException(String message) {
        super(message);
    }
}
