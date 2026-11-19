package me.tgoyal.clearance.policy;

/**
 * The result of checking one licence string against the policy.
 *
 * @param spdxExpression the normalized expression, or {@code null} if it could not be parsed
 * @param status         the verdict
 * @param error          why the verdict is UNKNOWN (no licence, parse error), or {@code null}
 */
public record LicenseVerdict(String spdxExpression, PolicyStatus status, String error) {
}
