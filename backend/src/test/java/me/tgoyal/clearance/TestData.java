package me.tgoyal.clearance;

import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.domain.Scan;
import me.tgoyal.clearance.parser.Ecosystem;
import me.tgoyal.clearance.policy.PolicyStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/** Builds entities for tests that run without a database. */
public final class TestData {

    public static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");

    private TestData() {
    }

    /** A saved-looking scan: the database normally assigns the id, so the test sets it directly. */
    public static Scan scan(long id) {
        Scan scan = new Scan("demo", Ecosystem.NPM, NOW);
        ReflectionTestUtils.setField(scan, "id", id);
        return scan;
    }

    public static Component component(Scan scan, long id, String name, PolicyStatus status) {
        Component component = new Component(scan, name, "1.0.0", "GPL-3.0-only", "lockfile", "GPL-3.0-only", status, null);
        ReflectionTestUtils.setField(component, "id", id);
        scan.addComponent(component);
        return component;
    }
}
