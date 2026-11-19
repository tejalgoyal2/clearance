package me.tgoyal.clearance.policy;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The licence policy loaded from policy.yml: which licences are approved, need review
 * or are denied, plus aliases and "X WITH Y" exceptions.
 *
 * <p>All lookups ignore case. Internally every map is keyed by the upper-cased text,
 * and {@link #canonicalId} turns any spelling back into the one written in the policy.
 *
 * <p>The object is immutable after construction, so it is safe to share between threads.
 */
public class LicensePolicy {

    /** The shape of policy.yml. Jackson fills it in; a missing section is {@code null}. */
    record PolicyFile(List<String> approved,
                      List<String> review,
                      List<String> denied,
                      Map<String, String> aliases,
                      Map<String, PolicyStatus> exceptions) {
    }

    // The policy as written, for display (GET /api/policy). Unmodifiable copies.
    private final List<String> approved;
    private final List<String> review;
    private final List<String> denied;
    private final Map<String, String> aliases;
    private final Map<String, PolicyStatus> exceptions;

    // Lookup tables, keyed by upper-cased text.
    private final Map<String, PolicyStatus> statusByKey = new HashMap<>();
    private final Map<String, String> canonicalByKey = new HashMap<>();
    private final Map<String, PolicyStatus> exceptionsByKey = new HashMap<>();

    /**
     * Reads a policy from YAML. Does not close the stream.
     *
     * @throws PolicyConfigException if the YAML is broken, has an unknown section, or is inconsistent
     */
    public static LicensePolicy load(InputStream yaml) {
        YAMLMapper mapper = YAMLMapper.builder()
                // A typo such as "aproved:" must fail, not be silently ignored.
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        PolicyFile file;
        try {
            file = mapper.readValue(yaml, PolicyFile.class);
        } catch (JacksonException e) {
            throw new PolicyConfigException("Cannot read policy.yml: " + e.getOriginalMessage(), e);
        }
        if (file == null) {
            throw new PolicyConfigException("policy.yml is empty");
        }
        return new LicensePolicy(file.approved(), file.review(), file.denied(), file.aliases(), file.exceptions());
    }

    /**
     * Builds a policy. Any argument may be {@code null}, meaning empty.
     *
     * @throws PolicyConfigException if a licence appears in more than one list
     */
    public LicensePolicy(List<String> approved,
                         List<String> review,
                         List<String> denied,
                         Map<String, String> aliases,
                         Map<String, PolicyStatus> exceptions) {
        this.approved = approved == null ? List.of() : List.copyOf(approved);
        this.review = review == null ? List.of() : List.copyOf(review);
        this.denied = denied == null ? List.of() : List.copyOf(denied);
        // LinkedHashMap keeps the order of policy.yml; Map.copyOf would not.
        this.aliases = aliases == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(aliases));
        this.exceptions = exceptions == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(exceptions));

        addList(approved, PolicyStatus.APPROVED);
        addList(review, PolicyStatus.REVIEW);
        addList(denied, PolicyStatus.DENIED);

        if (aliases != null) {
            aliases.forEach((alias, spdxId) -> canonicalByKey.put(key(alias), spdxId));
        }
        if (exceptions != null) {
            exceptions.forEach((expression, status) -> exceptionsByKey.put(key(expression), status));
        }
    }

    private void addList(List<String> ids, PolicyStatus status) {
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw new PolicyConfigException("Empty licence id in the " + listName(status) + " list");
            }
            PolicyStatus existing = statusByKey.get(key(id));
            if (existing != null) {
                throw new PolicyConfigException("Licence '" + id + "' is in both the "
                        + listName(existing) + " and " + listName(status) + " lists");
            }
            statusByKey.put(key(id), status);
            canonicalByKey.put(key(id), id);
        }
    }

    /**
     * The policy's own spelling of an id or alias: "mit" becomes "MIT", and
     * "Apache 2.0" becomes "Apache-2.0". Anything the policy doesn't know is returned as given.
     */
    public String canonicalId(String idOrAlias) {
        return canonicalByKey.getOrDefault(key(idOrAlias), idOrAlias);
    }

    /** The list a licence id is in, or empty if the policy doesn't list it. */
    public Optional<PolicyStatus> statusOf(String id) {
        return Optional.ofNullable(statusByKey.get(key(canonicalId(id))));
    }

    /** The verdict for a full "X WITH Y" expression, or empty if it isn't in the exceptions. */
    public Optional<PolicyStatus> exceptionStatus(String licenseWithException) {
        return Optional.ofNullable(exceptionsByKey.get(key(licenseWithException)));
    }

    public List<String> approved() {
        return approved;
    }

    public List<String> review() {
        return review;
    }

    public List<String> denied() {
        return denied;
    }

    public Map<String, String> aliases() {
        return aliases;
    }

    public Map<String, PolicyStatus> exceptions() {
        return exceptions;
    }

    /** Case-insensitive, whitespace-tolerant map key. Locale.ROOT avoids the Turkish "i" problem. */
    private static String key(String text) {
        return text.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String listName(PolicyStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }
}
