package me.tgoyal.clearance.license;

import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.Ecosystem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockfileLicenseResolverTest {

    private final LockfileLicenseResolver resolver = new LockfileLicenseResolver();

    @Test
    void usesTheLicenceDeclaredInTheFile() {
        LicenseLookup lookup = resolver.resolve(Ecosystem.NPM, new Dependency("lodash", "4.17.21", "MIT"));

        assertThat(lookup).isEqualTo(LicenseLookup.found("MIT", "lockfile"));
    }

    @Test
    void nothingDeclaredIsNotFound() {
        assertThat(resolver.resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", null)).isFound()).isFalse();
        assertThat(resolver.resolve(Ecosystem.NPM, new Dependency("x", "1.0.0", "  ")).isFound()).isFalse();
    }
}
