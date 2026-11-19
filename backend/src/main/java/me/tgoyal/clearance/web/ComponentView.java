package me.tgoyal.clearance.web;

import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.domain.Decision;
import me.tgoyal.clearance.policy.PolicyStatus;

import java.time.Instant;

public record ComponentView(Long id,
                            String name,
                            String version,
                            String rawLicense,
                            String licenseSource,
                            String spdxExpression,
                            PolicyStatus policyStatus,
                            String policyNote,
                            Decision decision,
                            String decidedBy,
                            Instant decidedAt,
                            String decisionComment) {

    public static ComponentView from(Component c) {
        return new ComponentView(c.getId(), c.getName(), c.getVersion(), c.getRawLicense(), c.getLicenseSource(),
                c.getSpdxExpression(), c.getPolicyStatus(), c.getPolicyNote(), c.getDecision(),
                c.getDecidedBy(), c.getDecidedAt(), c.getDecisionComment());
    }
}
