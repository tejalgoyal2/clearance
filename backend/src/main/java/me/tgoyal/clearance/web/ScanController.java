package me.tgoyal.clearance.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.tgoyal.clearance.domain.Scan;
import me.tgoyal.clearance.parser.DependencyFileException;
import me.tgoyal.clearance.policy.PolicyStatus;
import me.tgoyal.clearance.service.ReportService;
import me.tgoyal.clearance.service.ScanService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * HTTP endpoints for scans. Controllers stay thin: read the request, call a service,
 * turn the result into a response record. Errors are handled in {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/scans")
public class ScanController {

    private final ScanService scanService;
    private final ReportService reportService;

    public ScanController(ScanService scanService, ReportService reportService) {
        this.scanService = scanService;
        this.reportService = reportService;
    }

    /** Upload a pom.xml or package-lock.json. Files over 2 MB are refused with 413 (see application.yml). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScanSummary> createScan(@RequestPart("file") MultipartFile file,
                                                  @RequestParam @NotBlank @Size(max = 200) String projectName)
            throws IOException {
        if (file.isEmpty()) {
            throw new DependencyFileException("The uploaded file is empty");
        }
        // Some browsers send a full path; keep only the file name.
        String filename = StringUtils.getFilename(file.getOriginalFilename());

        // try-with-resources closes the stream even if the scan throws.
        Scan scan;
        try (InputStream content = file.getInputStream()) {
            scan = scanService.createScan(projectName.trim(), filename, content);
        }
        return ResponseEntity
                .created(URI.create("/api/scans/" + scan.getId()))
                .body(ScanSummary.from(scan));
    }

    @GetMapping
    public List<ScanSummary> listScans() {
        return scanService.listScans().stream().map(ScanSummary::from).toList();
    }

    @GetMapping("/{id}")
    public ScanSummary getScan(@PathVariable Long id) {
        return ScanSummary.from(scanService.getScan(id));
    }

    /** {@code ?status=DENIED} filters; without it, all components. An invalid status is a 400. */
    @GetMapping("/{id}/components")
    public List<ComponentView> getComponents(@PathVariable Long id,
                                             @RequestParam(required = false) PolicyStatus status) {
        return scanService.getComponents(id, status).stream().map(ComponentView::from).toList();
    }

    @GetMapping("/{id}/audit")
    public List<AuditEventView> getAuditTrail(@PathVariable Long id) {
        return scanService.getAuditTrail(id).stream().map(AuditEventView::from).toList();
    }

    @GetMapping(value = "/{id}/report.csv", produces = "text/csv")
    public ResponseEntity<String> downloadReport(@PathVariable Long id) {
        String csv = reportService.exportCsv(id);
        ContentDisposition attachment = ContentDisposition.attachment()
                .filename("clearance-scan-" + id + ".csv")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment.toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
