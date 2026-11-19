package me.tgoyal.clearance.policy;

/**
 * The verdict for a licence, ordered from least to most severe.
 *
 * <p>Severity is an explicit field rather than {@code ordinal()}, so reordering the
 * constants in this file can never silently change the policy.
 */
public enum PolicyStatus {
    APPROVED(0),
    REVIEW(1),
    UNKNOWN(2),
    DENIED(3);

    private final int severity;

    PolicyStatus(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }

    /** For {@code A AND B}: you must meet both, so the worse one wins. */
    public static PolicyStatus mostSevere(PolicyStatus a, PolicyStatus b) {
        return a.severity >= b.severity ? a : b;
    }

    /** For {@code A OR B}: you may pick either, so the better one wins. */
    public static PolicyStatus leastSevere(PolicyStatus a, PolicyStatus b) {
        return a.severity <= b.severity ? a : b;
    }
}
