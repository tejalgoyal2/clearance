package me.tgoyal.clearance.license;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.Ecosystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs a tiny real HTTP server in-process instead of mocking the HTTP client, so
 * timeouts, status codes and URL encoding are tested for real.
 */
class DepsDevLicenseResolverTest {

    private HttpServer server;
    private String baseUrl;

    /** Every request path the fake deps.dev received, in order. */
    private final List<String> requests = new CopyOnWriteArrayList<>();

    private int status = 200;
    private String body = "{}";
    private long delayMillis = 0;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        // A thread per request, so a deliberately slow request doesn't block the next one.
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestURI().getRawPath());
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private DepsDevLicenseResolver resolver(Duration timeout) {
        return new DepsDevLicenseResolver(baseUrl, timeout);
    }

    private DepsDevLicenseResolver resolver() {
        return resolver(Duration.ofSeconds(2));
    }

    @Test
    void readsTheLicenceFromTheResponse() {
        body = """
                { "versionKey": { "system": "NPM", "name": "lodash", "version": "4.17.21" },
                  "licenses": ["MIT"] }
                """;

        LicenseLookup lookup = resolver().resolve(Ecosystem.NPM, new Dependency("lodash", "4.17.21", null));

        assertThat(lookup).isEqualTo(LicenseLookup.found("MIT", "deps.dev"));
        assertThat(requests).containsExactly("/v3/systems/npm/packages/lodash/versions/4.17.21");
    }

    @Test
    void encodesMavenAndScopedNames() {
        body = "{ \"licenses\": [\"Apache-2.0\"] }";

        resolver().resolve(Ecosystem.MAVEN, new Dependency("com.google.guava:guava", "33.0.0-jre", null));
        resolver().resolve(Ecosystem.NPM, new Dependency("@angular/core", "18.0.0", null));

        assertThat(requests).containsExactly(
                "/v3/systems/maven/packages/com.google.guava%3Aguava/versions/33.0.0-jre",
                "/v3/systems/npm/packages/%40angular%2Fcore/versions/18.0.0");
    }

    @Test
    void joinsSeveralLicencesWithAnd() {
        // deps.dev lists every licence it found; being conservative, all of them apply.
        body = "{ \"licenses\": [\"MIT\", \"GPL-2.0-only OR Apache-2.0\"] }";

        LicenseLookup lookup = resolver().resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", null));

        assertThat(lookup.license()).isEqualTo("MIT AND (GPL-2.0-only OR Apache-2.0)");
    }

    @Test
    void anEmptyLicenceListIsNotFound() {
        body = "{ \"licenses\": [] }";

        LicenseLookup lookup = resolver().resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", null));

        assertThat(lookup.isFound()).isFalse();
        assertThat(lookup.reason()).isNotBlank();
    }

    @Test
    void aCacheHitMakesNoSecondCall() {
        body = "{ \"licenses\": [\"MIT\"] }";
        DepsDevLicenseResolver resolver = resolver();
        Dependency lodash = new Dependency("lodash", "4.17.21", null);

        LicenseLookup first = resolver.resolve(Ecosystem.NPM, lodash);
        LicenseLookup second = resolver.resolve(Ecosystem.NPM, lodash);

        assertThat(second).isEqualTo(first);
        assertThat(requests).hasSize(1);
    }

    @Test
    void theCacheKeyIncludesEcosystemAndVersion() {
        body = "{ \"licenses\": [\"MIT\"] }";
        DepsDevLicenseResolver resolver = resolver();

        resolver.resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", null));
        resolver.resolve(Ecosystem.NPM, new Dependency("x", "2.0.0", null));
        resolver.resolve(Ecosystem.MAVEN, new Dependency("x", "1.0.0", null));

        assertThat(requests).hasSize(3);
    }

    @Test
    void aTimeoutIsNotFoundAndIsNotCached() {
        delayMillis = 1_000;
        DepsDevLicenseResolver resolver = resolver(Duration.ofMillis(100));
        Dependency slow = new Dependency("slow", "1.0.0", null);

        LicenseLookup lookup = resolver.resolve(Ecosystem.NPM, slow);

        assertThat(lookup.isFound()).isFalse();
        assertThat(lookup.reason()).containsIgnoringCase("timed out");

        // A timeout may be temporary, so the next scan should try again.
        delayMillis = 0;
        body = "{ \"licenses\": [\"MIT\"] }";
        assertThat(resolver.resolve(Ecosystem.NPM, slow).isFound()).isTrue();
    }

    @Test
    void a404IsNotFound() {
        status = 404;
        body = "{ \"error\": \"not found\" }";

        LicenseLookup lookup = resolver().resolve(Ecosystem.NPM, new Dependency("nope", "1.0.0", null));

        assertThat(lookup.isFound()).isFalse();
        assertThat(lookup.reason()).contains("404");
    }

    @Test
    void aServerErrorIsNotFound() {
        status = 500;

        LicenseLookup lookup = resolver().resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", null));

        assertThat(lookup.isFound()).isFalse();
        assertThat(lookup.reason()).contains("500");
    }

    @Test
    void anUnreadableResponseIsNotFound() {
        body = "<html>oops</html>";

        LicenseLookup lookup = resolver().resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", null));

        assertThat(lookup.isFound()).isFalse();
    }

    @Test
    void anUnreachableServerIsNotFound() {
        server.stop(0);

        LicenseLookup lookup = resolver().resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", null));

        assertThat(lookup.isFound()).isFalse();
        assertThat(lookup.reason()).isNotBlank();
    }

    @Test
    void aMissingVersionIsNotFoundWithoutCallingTheServer() {
        LicenseLookup lookup = resolver().resolve(Ecosystem.MAVEN, new Dependency("com.example:lib", null, null));

        assertThat(lookup.isFound()).isFalse();
        assertThat(requests).isEmpty();
    }
}
