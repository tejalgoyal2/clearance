package me.tgoyal.clearance.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenPomParserTest {

    private final MavenPomParser parser = new MavenPomParser();

    private List<Dependency> parse(String xml) {
        return parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Wraps a dependencies block in a minimal, namespaced pom. */
    private static String pom(String inside) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.2.3</version>
                %s
                </project>
                """.formatted(inside);
    }

    @Test
    void supportsOnlyPomXml() {
        assertThat(parser.supports("pom.xml")).isTrue();
        assertThat(parser.supports("package-lock.json")).isFalse();
        assertThat(parser.supports("pom.xml.bak")).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void readsDirectDependencies() {
        List<Dependency> deps = parse(pom("""
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>33.0.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-api</artifactId>
                    <version>2.0.9</version>
                    <scope>runtime</scope>
                  </dependency>
                </dependencies>
                """));

        assertThat(deps).containsExactly(
                new Dependency("com.google.guava:guava", "33.0.0-jre", null),
                new Dependency("org.slf4j:slf4j-api", "2.0.9", null));
    }

    @Test
    void resolvesPropertyVersions() {
        List<Dependency> deps = parse(pom("""
                <properties>
                  <jackson.version>2.17.0</jackson.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-databind</artifactId>
                    <version>${jackson.version}</version>
                  </dependency>
                </dependencies>
                """));

        assertThat(deps).containsExactly(new Dependency("com.fasterxml.jackson.core:jackson-databind", "2.17.0", null));
    }

    @Test
    void resolvesProjectVersion() {
        List<Dependency> deps = parse(pom("""
                <dependencies>
                  <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>sibling</artifactId>
                    <version>${project.version}</version>
                  </dependency>
                </dependencies>
                """));

        assertThat(deps).containsExactly(new Dependency("com.example:sibling", "1.2.3", null));
    }

    @Test
    void leavesAnUnresolvablePropertyAsWritten() {
        // Better to show the user "${nowhere}" than to invent a version.
        List<Dependency> deps = parse(pom("""
                <dependencies>
                  <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>lib</artifactId>
                    <version>${nowhere}</version>
                  </dependency>
                </dependencies>
                """));

        assertThat(deps).containsExactly(new Dependency("com.example:lib", "${nowhere}", null));
    }

    @Test
    void skipsTestScope() {
        List<Dependency> deps = parse(pom("""
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter</artifactId>
                    <version>5.10.0</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>jakarta.servlet</groupId>
                    <artifactId>jakarta.servlet-api</artifactId>
                    <version>6.0.0</version>
                    <scope>provided</scope>
                  </dependency>
                </dependencies>
                """));

        assertThat(deps).extracting(Dependency::name).containsExactly("jakarta.servlet:jakarta.servlet-api");
    }

    @Test
    void keepsADependencyWithNoVersion() {
        // Versions managed by a parent or BOM are not in this file. Keep the dependency;
        // its licence will be UNKNOWN rather than silently missing from the report.
        List<Dependency> deps = parse(pom("""
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-web</artifactId>
                  </dependency>
                </dependencies>
                """));

        assertThat(deps).containsExactly(new Dependency("org.springframework.boot:spring-boot-starter-web", null, null));
    }

    @Test
    void ignoresDependencyManagementAndPluginDependencies() {
        List<Dependency> deps = parse(pom("""
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.example</groupId>
                      <artifactId>managed-only</artifactId>
                      <version>1.0</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>real</artifactId>
                    <version>1.0</version>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>some-plugin</artifactId>
                      <dependencies>
                        <dependency>
                          <groupId>com.example</groupId>
                          <artifactId>plugin-only</artifactId>
                          <version>1.0</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
                """));

        assertThat(deps).extracting(Dependency::name).containsExactly("com.example:real");
    }

    @Test
    void pomWithNoDependenciesGivesAnEmptyList() {
        assertThat(parse(pom(""))).isEmpty();
    }

    @Test
    void malformedXmlGivesAClearError() {
        assertThatThrownBy(() -> parse("<project><dependencies></project>"))
                .isInstanceOf(DependencyFileException.class)
                .hasMessageContaining("pom.xml");
    }

    @Test
    void xmlThatIsNotAPomIsRejected() {
        assertThatThrownBy(() -> parse("<html><body/></html>"))
                .isInstanceOf(DependencyFileException.class)
                .hasMessageContaining("<project>");
    }

    @Test
    void refusesDoctypesToPreventXxe() {
        // An XML external entity could make the server read its own files.
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE project [ <!ENTITY secret SYSTEM "file:///etc/passwd"> ]>
                <project><version>&secret;</version></project>
                """;

        assertThatThrownBy(() -> parse(xxe)).isInstanceOf(DependencyFileException.class);
    }
}
