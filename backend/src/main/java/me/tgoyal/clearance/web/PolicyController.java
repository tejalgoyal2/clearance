package me.tgoyal.clearance.web;

import me.tgoyal.clearance.policy.LicensePolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PolicyController {

    private final LicensePolicy policy;

    public PolicyController(LicensePolicy policy) {
        this.policy = policy;
    }

    @GetMapping("/api/policy")
    public PolicyView getPolicy() {
        return PolicyView.from(policy);
    }
}
