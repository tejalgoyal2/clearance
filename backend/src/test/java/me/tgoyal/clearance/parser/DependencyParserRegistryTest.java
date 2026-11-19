package me.tgoyal.clearance.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyParserRegistryTest {

    private final DependencyParserRegistry registry =
            new DependencyParserRegistry(List.of(new MavenPomParser(), new NpmLockfileParser()));

    private static InputStream text(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void picksTheParserByFilename() {
        assertThat(registry.parserFor("pom.xml")).get().isInstanceOf(MavenPomParser.class);
        assertThat(registry.parserFor("package-lock.json")).get().isInstanceOf(NpmLockfileParser.class);
        assertThat(registry.parserFor("build.gradle")).isEmpty();
    }

    @Test
    void unsupportedFileIsRejectedWithTheSupportedNames() {
        assertThatThrownBy(() -> registry.parse("build.gradle", text("")))
                .isInstanceOf(DependencyFileException.class)
                .hasMessageContaining("pom.xml")
                .hasMessageContaining("package-lock.json");
    }

    @Test
    void reportsTheEcosystem() {
        ParsedFile parsed = registry.parse("pom.xml", text("<project/>"));

        assertThat(parsed.ecosystem()).isEqualTo(Ecosystem.MAVEN);
    }

    @Test
    void removesDuplicateNameAndVersionPairs() {
        // npm installs the same package version at several paths when it can't hoist it.
        ParsedFile parsed = registry.parse("package-lock.json", text("""
                { "lockfileVersion": 3, "packages": {
                    "node_modules/a/node_modules/ms": { "version": "2.0.0", "license": "MIT" },
                    "node_modules/b/node_modules/ms": { "version": "2.0.0", "license": "MIT" },
                    "node_modules/ms": { "version": "2.1.3", "license": "MIT" }
                } }
                """));

        assertThat(parsed.ecosystem()).isEqualTo(Ecosystem.NPM);
        assertThat(parsed.dependencies()).containsExactly(
                new Dependency("ms", "2.0.0", "MIT"),
                new Dependency("ms", "2.1.3", "MIT"));
    }
}
