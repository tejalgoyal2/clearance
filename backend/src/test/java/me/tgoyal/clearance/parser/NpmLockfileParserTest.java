package me.tgoyal.clearance.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NpmLockfileParserTest {

    private final NpmLockfileParser parser = new NpmLockfileParser();

    private List<Dependency> parse(String json) {
        return parser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void supportsOnlyPackageLockJson() {
        assertThat(parser.supports("package-lock.json")).isTrue();
        assertThat(parser.supports("package.json")).isFalse();
        assertThat(parser.supports("pom.xml")).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void readsV3PackagesIncludingNestedAndScopedOnes() {
        List<Dependency> deps = parse("""
                {
                  "name": "demo",
                  "lockfileVersion": 3,
                  "packages": {
                    "": { "name": "demo", "version": "1.0.0", "license": "MIT" },
                    "node_modules/express": { "version": "4.19.2", "license": "MIT" },
                    "node_modules/express/node_modules/debug": { "version": "2.6.9", "license": "MIT" },
                    "node_modules/@angular/core": { "version": "18.0.0", "license": "MIT" }
                  }
                }
                """);

        // The root entry ("") is the project itself, not a dependency.
        assertThat(deps).containsExactly(
                new Dependency("express", "4.19.2", "MIT"),
                new Dependency("debug", "2.6.9", "MIT"),
                new Dependency("@angular/core", "18.0.0", "MIT"));
    }

    @Test
    void missingLicenceFieldIsNull() {
        List<Dependency> deps = parse("""
                { "lockfileVersion": 3, "packages": {
                    "node_modules/mystery": { "version": "0.0.1" }
                } }
                """);

        assertThat(deps).containsExactly(new Dependency("mystery", "0.0.1", null));
    }

    @Test
    void readsTheOldObjectFormOfLicence() {
        List<Dependency> deps = parse("""
                { "lockfileVersion": 2, "packages": {
                    "node_modules/old": { "version": "1.0.0", "license": { "type": "BSD-3-Clause" } }
                } }
                """);

        assertThat(deps).containsExactly(new Dependency("old", "1.0.0", "BSD-3-Clause"));
    }

    @Test
    void usesTheNameFieldForAliasedPackages() {
        // "npm install foo@npm:bar" installs bar under node_modules/foo.
        List<Dependency> deps = parse("""
                { "lockfileVersion": 3, "packages": {
                    "node_modules/foo": { "name": "bar", "version": "2.0.0", "license": "ISC" }
                } }
                """);

        assertThat(deps).containsExactly(new Dependency("bar", "2.0.0", "ISC"));
    }

    @Test
    void skipsDevDependencies() {
        // Same reasoning as Maven's test scope: dev dependencies don't ship.
        List<Dependency> deps = parse("""
                { "lockfileVersion": 3, "packages": {
                    "node_modules/jest": { "version": "29.0.0", "license": "MIT", "dev": true },
                    "node_modules/lodash": { "version": "4.17.21", "license": "MIT" }
                } }
                """);

        assertThat(deps).extracting(Dependency::name).containsExactly("lodash");
    }

    @Test
    void skipsWorkspaceLinks() {
        List<Dependency> deps = parse("""
                { "lockfileVersion": 3, "packages": {
                    "node_modules/my-workspace": { "resolved": "packages/my-workspace", "link": true }
                } }
                """);

        assertThat(deps).isEmpty();
    }

    @Test
    void rejectsV1LockfilesWithAClearMessage() {
        assertThatThrownBy(() -> parse("""
                { "lockfileVersion": 1, "dependencies": { "lodash": { "version": "4.17.21" } } }
                """))
                .isInstanceOf(DependencyFileException.class)
                .hasMessageContaining("lockfileVersion");
    }

    @Test
    void malformedJsonGivesAClearError() {
        assertThatThrownBy(() -> parse("{ \"packages\": "))
                .isInstanceOf(DependencyFileException.class)
                .hasMessageContaining("package-lock.json");
    }
}
