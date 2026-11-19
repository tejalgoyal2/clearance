package me.tgoyal.clearance.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import me.tgoyal.clearance.domain.Decision;

/** Body of POST /api/components/{id}/decision. Checked by Bean Validation before the controller runs. */
public record DecisionRequest(
        @NotNull Decision decision,
        @NotBlank @Size(max = 100) String reviewer,
        @NotBlank @Size(max = 2000) String comment) {

    @AssertTrue(message = "must be ACCEPTED or REJECTED")
    public boolean isDecisionMade() {
        // A null decision is reported by @NotNull; don't report it twice.
        return decision != Decision.NONE;
    }
}
