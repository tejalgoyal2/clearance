package me.tgoyal.clearance.parser;

/** The uploaded file is unsupported or malformed. The message is safe to show the user. */
public class DependencyFileException extends RuntimeException {

    public DependencyFileException(String message) {
        super(message);
    }

    public DependencyFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
