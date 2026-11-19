package me.tgoyal.clearance.repository;

import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.policy.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComponentRepository extends JpaRepository<Component, Long> {

    List<Component> findByScanIdOrderByNameAscVersionAsc(Long scanId);

    /** Served by the (scan_id, policy_status) index. */
    List<Component> findByScanIdAndPolicyStatusOrderByNameAscVersionAsc(Long scanId, PolicyStatus status);
}
