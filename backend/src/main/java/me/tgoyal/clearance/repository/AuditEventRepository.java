package me.tgoyal.clearance.repository;

import me.tgoyal.clearance.domain.AuditEvent;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Append-only by design: this extends the bare {@link Repository} marker, not
 * JpaRepository, so it has no delete or bulk-update methods. Code that tries to
 * delete an audit event does not compile.
 */
public interface AuditEventRepository extends Repository<AuditEvent, Long> {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> findByScanIdOrderByTimestampAscIdAsc(Long scanId);
}
