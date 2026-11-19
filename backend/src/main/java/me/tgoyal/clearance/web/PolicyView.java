package me.tgoyal.clearance.web;

import me.tgoyal.clearance.policy.LicensePolicy;
import me.tgoyal.clearance.policy.PolicyStatus;

import java.util.List;
import java.util.Map;

public record PolicyView(List<String> approved,
                         List<String> review,
                         List<String> denied,
                         Map<String, String> aliases,
                         Map<String, PolicyStatus> exceptions) {

    public static PolicyView from(LicensePolicy policy) {
        return new PolicyView(policy.approved(), policy.review(), policy.denied(),
                policy.aliases(), policy.exceptions());
    }
}
