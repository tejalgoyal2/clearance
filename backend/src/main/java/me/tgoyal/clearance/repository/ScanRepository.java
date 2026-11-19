package me.tgoyal.clearance.repository;

import me.tgoyal.clearance.domain.Scan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data writes the implementation at startup, from the method names. */
public interface ScanRepository extends JpaRepository<Scan, Long> {

    List<Scan> findAllByOrderByCreatedAtDesc();
}
