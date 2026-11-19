package me.tgoyal.clearance;

import me.tgoyal.clearance.domain.AlreadyDecidedException;
import me.tgoyal.clearance.domain.AuditAction;
import me.tgoyal.clearance.domain.AuditEvent;
import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.domain.Decision;
import me.tgoyal.clearance.domain.Scan;
import me.tgoyal.clearance.parser.Ecosystem;
import me.tgoyal.clearance.policy.PolicyStatus;
import me.tgoyal.clearance.repository.ComponentRepository;
import me.tgoyal.clearance.service.DecisionService;
import me.tgoyal.clearance.service.NotFoundException;
import me.tgoyal.clearance.service.ReportService;
import me.tgoyal.clearance.service.ScanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The whole application against a real PostgreSQL in Docker (Testcontainers).
 *
 * <p>Why not H2: the schema uses PostgreSQL features (TIMESTAMPTZ, a PL/pgSQL trigger),
 * and an in-memory database that only imitates PostgreSQL can pass tests that production fails.
 *
 * <p>deps.dev is pointed at a closed local port, so the test never touches the internet:
 * licences come from the lockfile, and anything without one is UNKNOWN.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "clearance.deps-dev.base-url=http://127.0.0.1:1",
                "clearance.deps-dev.timeout=500ms"
        })
@Testcontainers
class ClearanceIntegrationTest {

    /** @ServiceConnection points Spring's datasource at this container. Static: one container for the class. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final String LOCKFILE = """
            { "name": "demo", "lockfileVersion": 3, "packages": {
                "": { "name": "demo", "version": "1.0.0" },
                "node_modules/lodash":   { "version": "4.17.21", "license": "MIT" },
                "node_modules/axe-core": { "version": "4.9.1",   "license": "MPL-2.0" },
                "node_modules/gpl-lib":  { "version": "1.0.0",   "license": "GPL-3.0-only" },
                "node_modules/mystery":  { "version": "0.0.1" },
                "node_modules/broken":   { "version": "2.0.0",   "license": "MIT AND" }
            } }
            """;

    @Autowired
    private ScanService scanService;

    @Autowired
    private DecisionService decisionService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    private Scan createDemoScan() {
        return scanService.createScan("demo", "package-lock.json",
                new ByteArrayInputStream(LOCKFILE.getBytes(StandardCharsets.UTF_8)));
    }

    private Component componentNamed(Scan scan, String name) {
        return scanService.getComponents(scan.getId(), null).stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void scanPersistsComponentsWithTheirVerdicts() {
        Scan scan = createDemoScan();

        List<Component> components = scanService.getComponents(scan.getId(), null);
        Map<String, PolicyStatus> statusByName = components.stream()
                .collect(Collectors.toMap(Component::getName, Component::getPolicyStatus));

        assertThat(statusByName).containsExactlyInAnyOrderEntriesOf(Map.of(
                "lodash", PolicyStatus.APPROVED,
                "axe-core", PolicyStatus.REVIEW,
                "gpl-lib", PolicyStatus.DENIED,
                "mystery", PolicyStatus.UNKNOWN,
                "broken", PolicyStatus.UNKNOWN));

        Scan reloaded = scanService.getScan(scan.getId());
        assertThat(reloaded.getEcosystem()).isEqualTo(Ecosystem.NPM);
        assertThat(reloaded.getComponentCount()).isEqualTo(5);
        assertThat(reloaded.countOf(PolicyStatus.UNKNOWN)).isEqualTo(2);

        // A bad licence is stored with the reason, not dropped and not fatal.
        assertThat(componentNamed(scan, "broken").getPolicyNote()).contains("Malformed");
        assertThat(componentNamed(scan, "mystery").getPolicyNote()).contains("deps.dev");
    }

    @Test
    void scanCreationIsAudited() {
        Scan scan = createDemoScan();

        List<AuditEvent> trail = scanService.getAuditTrail(scan.getId());

        assertThat(trail).hasSize(1);
        assertThat(trail.get(0).getAction()).isEqualTo(AuditAction.SCAN_CREATED);
    }

    @Test
    void filtersComponentsByStatus() {
        Scan scan = createDemoScan();

        assertThat(scanService.getComponents(scan.getId(), PolicyStatus.DENIED))
                .extracting(Component::getName)
                .containsExactly("gpl-lib");
    }

    @Test
    void decisionIsSavedAndAppendsExactlyOneAuditEvent() {
        Scan scan = createDemoScan();
        Component gplLib = componentNamed(scan, "gpl-lib");

        decisionService.decide(gplLib.getId(), Decision.ACCEPTED, "tejal", "Internal tool, never distributed");

        Component reloaded = componentRepository.findById(gplLib.getId()).orElseThrow();
        assertThat(reloaded.getDecision()).isEqualTo(Decision.ACCEPTED);
        assertThat(reloaded.getDecidedBy()).isEqualTo("tejal");
        assertThat(reloaded.getDecisionComment()).isEqualTo("Internal tool, never distributed");
        assertThat(reloaded.getDecidedAt()).isNotNull();
        // Rule 6: a decision records an exception; it never changes the verdict.
        assertThat(reloaded.getPolicyStatus()).isEqualTo(PolicyStatus.DENIED);

        List<AuditEvent> trail = scanService.getAuditTrail(scan.getId());
        assertThat(trail).extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.SCAN_CREATED, AuditAction.DECISION_MADE);
        AuditEvent decisionEvent = trail.get(1);
        assertThat(decisionEvent.getActor()).isEqualTo("tejal");
        assertThat(decisionEvent.getComponent().getId()).isEqualTo(gplLib.getId());
    }

    @Test
    void secondDecisionIsRejectedAndLeavesNoTrace() {
        Scan scan = createDemoScan();
        Long gplLibId = componentNamed(scan, "gpl-lib").getId();
        decisionService.decide(gplLibId, Decision.ACCEPTED, "tejal", "first");

        assertThatThrownBy(() -> decisionService.decide(gplLibId, Decision.REJECTED, "someone", "second"))
                .isInstanceOf(AlreadyDecidedException.class);

        assertThat(componentRepository.findById(gplLibId).orElseThrow().getDecision()).isEqualTo(Decision.ACCEPTED);
        assertThat(scanService.getAuditTrail(scan.getId())).hasSize(2);
    }

    @Test
    void unknownIdsAreNotFound() {
        assertThatThrownBy(() -> scanService.getScan(-1L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> scanService.getComponents(-1L, null)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> decisionService.decide(-1L, Decision.ACCEPTED, "tejal", "x"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reportExportIsAudited() {
        Scan scan = createDemoScan();

        String csv = reportService.exportCsv(scan.getId());

        assertThat(csv.lines().findFirst().orElseThrow()).startsWith("name,version,");
        assertThat(csv.lines().count()).isEqualTo(1 + 5);
        assertThat(scanService.getAuditTrail(scan.getId())).extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.SCAN_CREATED, AuditAction.REPORT_EXPORTED);
    }

    @Test
    void theDatabaseRefusesToChangeAuditEvents() {
        Scan scan = createDemoScan();
        Long eventId = scanService.getAuditTrail(scan.getId()).get(0).getId();

        assertThatThrownBy(() -> jdbc.update("UPDATE audit_event SET actor = 'mallory' WHERE id = ?", eventId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_event WHERE id = ?", eventId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    // Two checks over real HTTP and Tomcat, where MockMvc can't help: the multipart
    // upload itself, and the server's size limit.

    private HttpStatusCode upload(String filename, byte[] content) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("projectName", "over-http");
        form.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return RestClient.create("http://localhost:" + port)
                .post()
                .uri("/api/scans")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { }) // read every status, don't throw
                .toBodilessEntity()
                .getStatusCode();
    }

    @Test
    void uploadOverHttpReturns201() {
        assertThat(upload("package-lock.json", LOCKFILE.getBytes(StandardCharsets.UTF_8)).value()).isEqualTo(201);
    }

    @Test
    void uploadOverTwoMegabytesReturns413() {
        byte[] tooBig = new byte[2_500_000];

        assertThat(upload("package-lock.json", tooBig).value()).isEqualTo(413);
    }
}
