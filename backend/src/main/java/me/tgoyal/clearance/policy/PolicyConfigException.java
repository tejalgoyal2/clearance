package me.tgoyal.clearance.policy;

/** Thrown when policy.yml is invalid. The application must refuse to start. */
public class PolicyConfigException extends RuntimeException {

    public PolicyConfigException(String message) {
        super(message);
    }

    public PolicyConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
