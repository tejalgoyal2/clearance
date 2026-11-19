package me.tgoyal.clearance.policy;

import me.tgoyal.clearance.spdx.SpdxExpression;
import me.tgoyal.clearance.spdx.SpdxExpression.And;
import me.tgoyal.clearance.spdx.SpdxExpression.License;
import me.tgoyal.clearance.spdx.SpdxExpression.Or;
import me.tgoyal.clearance.spdx.SpdxExpression.With;
import me.tgoyal.clearance.spdx.SpdxParseException;
import me.tgoyal.clearance.spdx.SpdxParser;

/**
 * Turns a licence string into a verdict: normalize, parse into a tree, then evaluate the tree.
 *
 * <p>This method never throws for bad input. A missing or malformed licence is UNKNOWN,
 * with the reason in {@link LicenseVerdict#error()}, so one bad licence cannot fail a scan.
 */
public class PolicyEngine {

    private final LicensePolicy policy;
    private final SpdxParser parser;

    public PolicyEngine(LicensePolicy policy, SpdxParser parser) {
        this.policy = policy;
        this.parser = parser;
    }

    public LicenseVerdict evaluate(String rawLicense) {
        if (rawLicense == null || rawLicense.isBlank()) {
            return new LicenseVerdict(null, PolicyStatus.UNKNOWN, "No licence declared");
        }

        // Aliases such as "The Apache Software License, Version 2.0" contain spaces and
        // commas, so they must be replaced before the string is parsed.
        String expression = policy.canonicalId(rawLicense.trim());

        SpdxExpression tree;
        try {
            tree = normalize(parser.parse(expression));
        } catch (SpdxParseException e) {
            return new LicenseVerdict(null, PolicyStatus.UNKNOWN, "Malformed licence expression: " + e.getMessage());
        }
        return new LicenseVerdict(tree.toString(), statusOf(tree), null);
    }

    /** Rebuilds the tree with every id in the policy's own spelling, e.g. "mit" becomes "MIT". */
    private SpdxExpression normalize(SpdxExpression node) {
        return switch (node) {
            case License license -> new License(policy.canonicalId(license.id()));
            case With with -> new With(new License(policy.canonicalId(with.license().id())), with.exception());
            case And and -> new And(normalize(and.left()), normalize(and.right()));
            case Or or -> new Or(normalize(or.left()), normalize(or.right()));
        };
    }

    /**
     * Evaluates the tree bottom-up. The switch has no default branch: SpdxExpression is
     * sealed, so the compiler fails the build if a new node type is added and not handled here.
     */
    private PolicyStatus statusOf(SpdxExpression node) {
        return switch (node) {
            case License license -> policy.statusOf(license.id()).orElse(PolicyStatus.UNKNOWN);
            case With with -> policy.exceptionStatus(with.toString())
                    .orElseGet(() -> statusOf(with.license()));
            case And and -> PolicyStatus.mostSevere(statusOf(and.left()), statusOf(and.right()));
            case Or or -> PolicyStatus.leastSevere(statusOf(or.left()), statusOf(or.right()));
        };
    }
}
