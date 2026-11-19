package me.tgoyal.clearance.policy;

import org.junit.jupiter.api.Test;

import static me.tgoyal.clearance.policy.PolicyStatus.APPROVED;
import static me.tgoyal.clearance.policy.PolicyStatus.DENIED;
import static me.tgoyal.clearance.policy.PolicyStatus.REVIEW;
import static me.tgoyal.clearance.policy.PolicyStatus.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyStatusTest {

    @Test
    void severityGoesApprovedReviewUnknownDenied() {
        assertThat(APPROVED.severity()).isLessThan(REVIEW.severity());
        assertThat(REVIEW.severity()).isLessThan(UNKNOWN.severity());
        assertThat(UNKNOWN.severity()).isLessThan(DENIED.severity());
    }

    @Test
    void mostSeverePicksTheWorse() {
        assertThat(PolicyStatus.mostSevere(APPROVED, DENIED)).isEqualTo(DENIED);
        assertThat(PolicyStatus.mostSevere(UNKNOWN, REVIEW)).isEqualTo(UNKNOWN);
    }

    @Test
    void leastSeverePicksTheBetter() {
        assertThat(PolicyStatus.leastSevere(APPROVED, DENIED)).isEqualTo(APPROVED);
        assertThat(PolicyStatus.leastSevere(UNKNOWN, REVIEW)).isEqualTo(REVIEW);
    }
}
