package me.tgoyal.clearance.web;

import me.tgoyal.clearance.domain.Scan;
import me.tgoyal.clearance.parser.Ecosystem;
import me.tgoyal.clearance.policy.PolicyStatus;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * A scan as the API returns it. Controllers return records like this, never entities:
 * the JSON shape stays stable when the database changes, and lazy JPA fields are never
 * touched outside a transaction.
 */
public record ScanSummary(Long id,
                          String projectName,
                          Ecosystem ecosystem,
                          Instant createdAt,
                          int componentCount,
                          Map<PolicyStatus, Integer> counts) {

    public static ScanSummary from(Scan scan) {
        // EnumMap iterates in declaration order, so the JSON lists APPROVED..DENIED.
        Map<PolicyStatus, Integer> counts = new EnumMap<>(PolicyStatus.class);
        for (PolicyStatus status : PolicyStatus.values()) {
            counts.put(status, scan.countOf(status));
        }
        return new ScanSummary(scan.getId(), scan.getProjectName(), scan.getEcosystem(),
                scan.getCreatedAt(), scan.getComponentCount(), counts);
    }
}
