package me.tgoyal.clearance.web;

import me.tgoyal.clearance.TestData;
import me.tgoyal.clearance.domain.Scan;
import me.tgoyal.clearance.parser.DependencyFileException;
import me.tgoyal.clearance.policy.PolicyStatus;
import me.tgoyal.clearance.service.NotFoundException;
import me.tgoyal.clearance.service.ReportService;
import me.tgoyal.clearance.service.ScanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the web layer only: HTTP in, JSON out. The services are Mockito mocks, so these
 * tests check status codes, validation and the error shape, not business logic.
 */
@WebMvcTest(ScanController.class)
class ScanControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ScanService scanService;

    @MockitoBean
    private ReportService reportService;

    private static MockMultipartFile file(String filename, String content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content.getBytes());
    }

    @Test
    void validUploadReturns201WithTheSummary() throws Exception {
        Scan scan = TestData.scan(7);
        TestData.component(scan, 1, "gpl-thing", PolicyStatus.DENIED);
        when(scanService.createScan(eq("demo"), eq("package-lock.json"), any(InputStream.class))).thenReturn(scan);

        mvc.perform(multipart("/api/scans")
                        .file(file("package-lock.json", "{}"))
                        .param("projectName", "demo"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/scans/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.projectName").value("demo"))
                .andExpect(jsonPath("$.componentCount").value(1))
                .andExpect(jsonPath("$.counts.DENIED").value(1))
                .andExpect(jsonPath("$.counts.APPROVED").value(0));
    }

    @Test
    void unsupportedFileReturns400WithTheStandardErrorShape() throws Exception {
        when(scanService.createScan(any(), eq("build.gradle"), any()))
                .thenThrow(new DependencyFileException("Unsupported file 'build.gradle'"));

        mvc.perform(multipart("/api/scans")
                        .file(file("build.gradle", "plugins {}"))
                        .param("projectName", "demo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Unsupported file 'build.gradle'"))
                .andExpect(jsonPath("$.path").value("/api/scans"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void blankProjectNameReturns400() throws Exception {
        mvc.perform(multipart("/api/scans")
                        .file(file("pom.xml", "<project/>"))
                        .param("projectName", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("projectName")));

        verifyNoInteractions(scanService);
    }

    @Test
    void missingFileReturns400() throws Exception {
        mvc.perform(multipart("/api/scans").param("projectName", "demo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("file")));
    }

    @Test
    void emptyFileReturns400() throws Exception {
        mvc.perform(multipart("/api/scans")
                        .file(file("pom.xml", ""))
                        .param("projectName", "demo"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(scanService);
    }

    @Test
    void listsScans() throws Exception {
        when(scanService.listScans()).thenReturn(List.of(TestData.scan(1), TestData.scan(2)));

        mvc.perform(get("/api/scans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unknownScanReturns404() throws Exception {
        when(scanService.getScan(99L)).thenThrow(new NotFoundException("Scan 99 not found"));

        mvc.perform(get("/api/scans/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Scan 99 not found"));
    }

    @Test
    void filtersComponentsByStatus() throws Exception {
        Scan scan = TestData.scan(7);
        when(scanService.getComponents(7L, PolicyStatus.DENIED))
                .thenReturn(List.of(TestData.component(scan, 3, "gpl-thing", PolicyStatus.DENIED)));

        mvc.perform(get("/api/scans/7/components").param("status", "DENIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("gpl-thing"))
                .andExpect(jsonPath("$[0].policyStatus").value("DENIED"))
                .andExpect(jsonPath("$[0].spdxExpression").value("GPL-3.0-only"))
                .andExpect(jsonPath("$[0].decision").value("NONE"));
    }

    @Test
    void noStatusFilterMeansAllComponents() throws Exception {
        when(scanService.getComponents(eq(7L), isNull())).thenReturn(List.of());

        mvc.perform(get("/api/scans/7/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidStatusFilterReturns400() throws Exception {
        mvc.perform(get("/api/scans/7/components").param("status", "MAYBE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void reportDownloadsAsCsv() throws Exception {
        when(reportService.exportCsv(7L)).thenReturn("name,version\nlodash,4.17.21\n");

        mvc.perform(get("/api/scans/7/report.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string("name,version\nlodash,4.17.21\n"));
    }
}
