package me.tgoyal.clearance.config;

import me.tgoyal.clearance.license.ChainedLicenseResolver;
import me.tgoyal.clearance.license.DepsDevLicenseResolver;
import me.tgoyal.clearance.license.LicenseResolver;
import me.tgoyal.clearance.license.LockfileLicenseResolver;
import me.tgoyal.clearance.parser.DependencyFileParser;
import me.tgoyal.clearance.parser.DependencyParserRegistry;
import me.tgoyal.clearance.parser.MavenPomParser;
import me.tgoyal.clearance.parser.NpmLockfileParser;
import me.tgoyal.clearance.policy.LicensePolicy;
import me.tgoyal.clearance.policy.PolicyEngine;
import me.tgoyal.clearance.spdx.SpdxParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;

/**
 * Wires the plain-Java engine classes from phase 1 into Spring.
 *
 * <p>They are created here with @Bean methods instead of being annotated, so the engine
 * has no dependency on Spring and stays easy to unit-test with {@code new}.
 */
@Configuration
@EnableConfigurationProperties(ClearanceProperties.class)
public class ClearanceConfig {

    /**
     * Loaded once at startup. If policy.yml is invalid (for example, a licence in two
     * lists) this throws, and the application refuses to start.
     */
    @Bean
    public LicensePolicy licensePolicy(ClearanceProperties properties) throws IOException {
        try (InputStream in = properties.policyFile().getInputStream()) {
            return LicensePolicy.load(in);
        }
    }

    @Bean
    public PolicyEngine policyEngine(LicensePolicy policy) {
        return new PolicyEngine(policy, new SpdxParser());
    }

    @Bean
    public MavenPomParser mavenPomParser() {
        return new MavenPomParser();
    }

    @Bean
    public NpmLockfileParser npmLockfileParser() {
        return new NpmLockfileParser();
    }

    /** Spring passes in every DependencyFileParser bean: add a parser bean and it is picked up. */
    @Bean
    public DependencyParserRegistry dependencyParserRegistry(List<DependencyFileParser> parsers) {
        return new DependencyParserRegistry(parsers);
    }

    /** The lockfile first (free, offline), then deps.dev. Only the chain is a bean, so there is one LicenseResolver to inject. */
    @Bean
    public LicenseResolver licenseResolver(ClearanceProperties properties) {
        ClearanceProperties.DepsDev depsDev = properties.depsDev();
        return new ChainedLicenseResolver(List.of(
                new LockfileLicenseResolver(),
                new DepsDevLicenseResolver(depsDev.baseUrl(), depsDev.timeout())));
    }

    /** Injected instead of calling Instant.now() directly, so tests can control time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
