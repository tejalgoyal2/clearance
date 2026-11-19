package me.tgoyal.clearance.service;

import me.tgoyal.clearance.domain.AuditAction;
import me.tgoyal.clearance.domain.AuditEvent;
import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.domain.Scan;
import me.tgoyal.clearance.repository.AuditEventRepository;
import me.tgoyal.clearance.repository.ComponentRepository;
import me.tgoyal.clearance.repository.ScanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Builds the CSV report for a scan. Every export is recorded in the audit trail. */
@Service
public class ReportService {

    private static final List<String> HEADER = List.of(
            "name", "version", "raw_license", "license_source", "spdx_expression", "policy_status", "policy_note",
            "decision", "decided_by", "decided_at", "decision_comment");

    private final ScanRepository scanRepository;
    private final ComponentRepository componentRepository;
    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    public ReportService(ScanRepository scanRepository,
                         ComponentRepository componentRepository,
                         AuditEventRepository auditEventRepository,
                         Clock clock) {
        this.scanRepository = scanRepository;
        this.componentRepository = componentRepository;
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    @Transactional
    public String exportCsv(Long scanId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new NotFoundException("Scan " + scanId + " not found"));
        List<Component> components = componentRepository.findByScanIdOrderByNameAscVersionAsc(scanId);

        StringBuilder csv = new StringBuilder();
        csv.append(Csv.row(HEADER)).append('\n');
        for (Component c : components) {
            // Arrays.asList, not List.of: List.of rejects nulls, and many of these fields can be null.
            csv.append(Csv.row(Arrays.asList(
                    c.getName(), c.getVersion(), c.getRawLicense(), c.getLicenseSource(), c.getSpdxExpression(),
                    c.getPolicyStatus().name(), c.getPolicyNote(), c.getDecision().name(), c.getDecidedBy(),
                    Objects.toString(c.getDecidedAt(), null), c.getDecisionComment())))
                    .append('\n');
        }

        auditEventRepository.save(new AuditEvent(scan, null, AuditEvent.ANONYMOUS, AuditAction.REPORT_EXPORTED,
                "CSV report with " + components.size() + " components", clock.instant()));
        return csv.toString();
    }
}
