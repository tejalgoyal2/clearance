package me.tgoyal.clearance.parser;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads every installed package from an npm {@code package-lock.json} (lockfileVersion 2 or 3).
 *
 * <p>The {@code packages} object maps install paths to package details:
 * <pre>
 *   ""                                     the project itself (skipped)
 *   "node_modules/express"                 a dependency
 *   "node_modules/express/node_modules/ms" a transitive dependency npm could not hoist
 *   "node_modules/@angular/core"           a scoped package
 * </pre>
 * Packages marked {@code "dev": true} are skipped, for the same reason as Maven's test scope.
 */
public class NpmLockfileParser implements DependencyFileParser {

    private static final String NODE_MODULES = "node_modules/";

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public boolean supports(String filename) {
        return "package-lock.json".equals(filename);
    }

    @Override
    public Ecosystem ecosystem() {
        return Ecosystem.NPM;
    }

    @Override
    public List<Dependency> parse(InputStream input) {
        JsonNode root;
        try {
            root = mapper.readTree(input);
        } catch (JacksonException e) {
            throw new DependencyFileException("package-lock.json is not valid JSON: " + e.getOriginalMessage(), e);
        }

        JsonNode packages = root.path("packages");
        if (!packages.isObject()) {
            throw new DependencyFileException("package-lock.json has no \"packages\" section. Only lockfileVersion 2 "
                    + "and 3 are supported; run npm 7 or later to upgrade the lockfile.");
        }

        List<Dependency> dependencies = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry : packages.properties()) {
            String path = entry.getKey();
            JsonNode details = entry.getValue();

            boolean isProjectItself = path.isEmpty();
            boolean isWorkspaceLink = details.path("link").asBoolean(false);
            boolean isDevOnly = details.path("dev").asBoolean(false);
            if (isProjectItself || isWorkspaceLink || isDevOnly) {
                continue;
            }

            dependencies.add(new Dependency(nameOf(path, details), textOrNull(details.get("version")), licenseOf(details)));
        }
        return dependencies;
    }

    /**
     * The package name is the part of the path after the last "node_modules/",
     * unless the entry says otherwise (an aliased install such as {@code foo@npm:bar}).
     */
    private static String nameOf(String path, JsonNode details) {
        String declaredName = textOrNull(details.get("name"));
        if (declaredName != null) {
            return declaredName;
        }
        int start = path.lastIndexOf(NODE_MODULES);
        return start < 0 ? path : path.substring(start + NODE_MODULES.length());
    }

    /** Modern packages use {@code "license": "MIT"}; some old ones use {@code "license": {"type": "MIT"}}. */
    private static String licenseOf(JsonNode details) {
        JsonNode license = details.get("license");
        if (license == null) {
            return null;
        }
        if (license.isObject()) {
            return textOrNull(license.get("type"));
        }
        return textOrNull(license);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || !node.isString()) {
            return null;
        }
        String text = node.asString().trim();
        return text.isEmpty() ? null : text;
    }
}
