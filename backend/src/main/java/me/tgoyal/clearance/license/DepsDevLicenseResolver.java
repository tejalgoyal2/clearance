package me.tgoyal.clearance.license;

import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.Ecosystem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up a licence on deps.dev (Open Source Insights):
 * {@code GET /v3/systems/{system}/packages/{name}/versions/{version}}.
 *
 * <p>Answers are cached in memory, keyed by ecosystem, name and version. A published
 * package version never changes, so a definitive answer (200 or 404) can be kept forever.
 * Timeouts and server errors may be temporary, so they are not cached.
 *
 * <p>The cache is a {@link ConcurrentHashMap} because one resolver is shared by every
 * request thread. A plain HashMap can corrupt itself under concurrent writes.
 */
public class DepsDevLicenseResolver implements LicenseResolver {

    public static final String DEFAULT_BASE_URL = "https://api.deps.dev";

    private static final String SOURCE = "deps.dev";

    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final Map<String, LicenseLookup> cache = new ConcurrentHashMap<>();

    public DepsDevLicenseResolver(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public LicenseLookup resolve(Ecosystem ecosystem, Dependency dependency) {
        if (dependency.version() == null || dependency.version().isBlank()) {
            return LicenseLookup.notFound("No version in the dependency file, so deps.dev cannot be asked");
        }

        String cacheKey = ecosystem + ":" + dependency.name() + "@" + dependency.version();
        LicenseLookup cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // Two threads may both miss and both fetch the same package. That costs one extra
        // HTTP call; it is simpler than computeIfAbsent, which would hold a lock during the call.
        return fetch(ecosystem, dependency, cacheKey);
    }

    private LicenseLookup fetch(Ecosystem ecosystem, Dependency dependency, String cacheKey) {
        HttpRequest request = HttpRequest.newBuilder(urlFor(ecosystem, dependency))
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            // Must come before IOException, because it is a subclass of it.
            return LicenseLookup.notFound("deps.dev timed out after " + timeout.toMillis() + " ms");
        } catch (IOException e) {
            return LicenseLookup.notFound("Could not reach deps.dev: " + e);
        } catch (InterruptedException e) {
            // Restore the flag so the code that interrupted this thread can see it.
            Thread.currentThread().interrupt();
            return LicenseLookup.notFound("Interrupted while calling deps.dev");
        }

        if (response.statusCode() == 404) {
            LicenseLookup result = LicenseLookup.notFound("deps.dev does not know this package version (404)");
            cache.put(cacheKey, result);
            return result;
        }
        if (response.statusCode() != 200) {
            return LicenseLookup.notFound("deps.dev returned HTTP " + response.statusCode());
        }

        List<String> licenses;
        try {
            licenses = readLicenses(response.body());
        } catch (JacksonException e) {
            return LicenseLookup.notFound("deps.dev returned a response that is not JSON");
        }

        LicenseLookup result = licenses.isEmpty()
                ? LicenseLookup.notFound("deps.dev lists no licence for this version")
                : LicenseLookup.found(joinWithAnd(licenses), SOURCE);
        cache.put(cacheKey, result);
        return result;
    }

    /**
     * Builds the request URL. Names are percent-encoded because Maven names contain ':'
     * and scoped npm names contain '@' and '/', which would otherwise break the path.
     */
    private URI urlFor(Ecosystem ecosystem, Dependency dependency) {
        return URI.create(baseUrl
                + "/v3/systems/" + ecosystem.name().toLowerCase(Locale.ROOT)
                + "/packages/" + encode(dependency.name())
                + "/versions/" + encode(dependency.version()));
    }

    private static String encode(String pathSegment) {
        return URLEncoder.encode(pathSegment, StandardCharsets.UTF_8);
    }

    private List<String> readLicenses(String body) {
        List<String> licenses = new ArrayList<>();
        for (JsonNode license : mapper.readTree(body).path("licenses")) {
            if (license.isString() && !license.asString().isBlank()) {
                licenses.add(license.asString().trim());
            }
        }
        return licenses;
    }

    /**
     * deps.dev may list several licences. Being conservative, all of them apply, so they
     * are joined with AND. Each one is wrapped in parentheses if it is itself compound.
     */
    private static String joinWithAnd(List<String> licenses) {
        if (licenses.size() == 1) {
            return licenses.get(0);
        }
        List<String> parts = new ArrayList<>();
        for (String license : licenses) {
            parts.add(license.contains(" ") ? "(" + license + ")" : license);
        }
        return String.join(" AND ", parts);
    }
}
