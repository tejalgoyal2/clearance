package me.tgoyal.clearance.service;

import me.tgoyal.clearance.domain.AuditAction;
import me.tgoyal.clearance.domain.AuditEvent;
import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.domain.Scan;
import me.tgoyal.clearance.license.LicenseLookup;
import me.tgoyal.clearance.license.LicenseResolver;
import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.DependencyParserRegistry;
import me.tgoyal.clearance.parser.ParsedFile;
import me.tgoyal.clearance.policy.LicenseVerdict;
import me.tgoyal.clearance.policy.PolicyEngine;
import me.tgoyal.clearance.policy.PolicyStatus;
import me.tgoyal.clearance.repository.AuditEventRepository;
import me.tgoyal.clearance.repository.ComponentRepository;
import me.tgoyal.clearance.repository.ScanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.time.Clock;
import java.util.List;

/**
 * Runs scans and reads them back.
 *
 * <p>All dependencies arrive through the constructor (constructor injection): the fields
 * can be final, and a test can build the service with {@code new} and its own fakes.
 */
@Service
public class ScanService {

    private final DependencyParserRegistry parserRegistry;
    private final LicenseResolver licenseResolver;
    private final PolicyEngine policyEngine;
    private final ScanRepository scanRepository;
    private final ComponentRepository componentRepository;
    private final AuditEventRepository auditEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ScanService(DependencyParserRegistry parserRegistry,
                       LicenseResolver licenseResolver,
                       PolicyEngine policyEngine,
                       ScanRepository scanRepository,
                       ComponentRepository componentRepository,
                       AuditEventRepository auditEventRepository,
                       TransactionTemplate transactionTemplate,
                       Clock clock) {
        this.parserRegistry = parserRegistry;
        this.licenseResolver = licenseResolver;
        this.policyEngine = policyEngine;
        this.scanRepository = scanRepository;
        this.componentRepository = componentRepository;
        this.auditEventRepository = auditEventRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Parses the file, looks up and judges every licence, then saves the scan.
     *
     * <p>Not annotated @Transactional on purpose. Licence lookups call deps.dev over HTTP
     * and can take seconds; holding a database connection open that long would starve
     * other requests. So the slow work happens first, and only the saving runs in a
     * (short) transaction, started explicitly with a TransactionTemplate.
     *
     * @throws me.tgoyal.clearance.parser.DependencyFileException if the file is unsupported or malformed
     */
    public Scan createScan(String projectName, String filename, InputStream content) {
        ParsedFile parsed = parserRegistry.parse(filename, content);
        Scan scan = new Scan(projectName, parsed.ecosystem(), clock.instant());

        for (Dependency dependency : parsed.dependencies()) {
            LicenseLookup lookup = licenseResolver.resolve(parsed.ecosystem(), dependency);
            LicenseVerdict verdict = policyEngine.evaluate(lookup.license());

            // Explain an UNKNOWN: either nothing was found (say why), or it didn't parse.
            String note = lookup.isFound() ? verdict.error() : lookup.reason();

            scan.addComponent(new Component(scan, dependency.name(), dependency.version(),
                    lookup.license(), lookup.source(), verdict.spdxExpression(), verdict.status(), note));
        }

        // Scan, components (via cascade) and audit event are saved together, or not at all.
        return transactionTemplate.execute(status -> {
            Scan saved = scanRepository.save(scan);
            auditEventRepository.save(new AuditEvent(saved, null, AuditEvent.ANONYMOUS, AuditAction.SCAN_CREATED,
                    "Scanned " + filename + ": " + saved.getComponentCount() + " components", clock.instant()));
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public List<Scan> listScans() {
        return scanRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Scan getScan(Long scanId) {
        return scanRepository.findById(scanId)
                .orElseThrow(() -> new NotFoundException("Scan " + scanId + " not found"));
    }

    /**
     * @param status only components with this verdict, or {@code null} for all
     */
    @Transactional(readOnly = true)
    public List<Component> getComponents(Long scanId, PolicyStatus status) {
        requireScanExists(scanId);
        if (status == null) {
            return componentRepository.findByScanIdOrderByNameAscVersionAsc(scanId);
        }
        return componentRepository.findByScanIdAndPolicyStatusOrderByNameAscVersionAsc(scanId, status);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getAuditTrail(Long scanId) {
        requireScanExists(scanId);
        return auditEventRepository.findByScanIdOrderByTimestampAscIdAsc(scanId);
    }

    /** Without this, an unknown scan id would return an empty list instead of a 404. */
    private void requireScanExists(Long scanId) {
        if (!scanRepository.existsById(scanId)) {
            throw new NotFoundException("Scan " + scanId + " not found");
        }
    }
}
