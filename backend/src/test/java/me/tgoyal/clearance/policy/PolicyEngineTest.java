package me.tgoyal.clearance.policy;

import me.tgoyal.clearance.spdx.SpdxParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against the real policy.yml, so these tests also guard the shipped configuration. */
class PolicyEngineTest {

    private static PolicyEngine engine;

    @BeforeAll
    static void loadPolicy() throws Exception {
        try (InputStream in = PolicyEngineTest.class.getResourceAsStream("/policy.yml")) {
            engine = new PolicyEngine(LicensePolicy.load(in), new SpdxParser());
        }
    }

    // The table from SPEC.md, plus the deny-first rules spelled out one by one.
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "MIT                                       | APPROVED",
            "MIT OR GPL-3.0-only                       | APPROVED",
            "Apache-2.0 AND GPL-3.0-only               | DENIED",
            "(MIT OR GPL-3.0-only) AND LGPL-2.1-only   | REVIEW",
            "MIT AND                                   | UNKNOWN",
            // OR takes the least severe side.
            "GPL-3.0-only OR MPL-2.0                   | REVIEW",
            // AND takes the most severe side.
            "MIT AND MPL-2.0                           | REVIEW",
            // An id the policy does not know is UNKNOWN...
            "WTFPL                                     | UNKNOWN",
            // ...which loses to a known licence in an OR...
            "WTFPL OR MIT                              | APPROVED",
            // ...beats APPROVED in an AND...
            "WTFPL AND MIT                             | UNKNOWN",
            // ...and is beaten by DENIED in an AND (deny-first).
            "WTFPL AND GPL-3.0-only                    | DENIED",
            // deps.dev reports this for licences it cannot classify.
            "non-standard                              | UNKNOWN",
    })
    void evaluatesExpressions(String expression, PolicyStatus expected) {
        assertThat(engine.evaluate(expression).status()).isEqualTo(expected);
    }

    @Test
    void returnsTheNormalizedExpression() {
        LicenseVerdict verdict = engine.evaluate("( mit OR gpl-3.0-only )  and LGPL-2.1-only");

        assertThat(verdict.spdxExpression()).isEqualTo("(MIT OR GPL-3.0-only) AND LGPL-2.1-only");
        assertThat(verdict.error()).isNull();
    }

    @Test
    void normalizesAWholeStringAlias() {
        LicenseVerdict verdict = engine.evaluate("The Apache Software License, Version 2.0");

        assertThat(verdict.status()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(verdict.spdxExpression()).isEqualTo("Apache-2.0");
    }

    @Test
    void normalizesAliasesInsideAnExpression() {
        // "GPL-2.0" is a deprecated SPDX id, aliased to GPL-2.0-only.
        LicenseVerdict verdict = engine.evaluate("GPL-2.0 OR MIT");

        assertThat(verdict.spdxExpression()).isEqualTo("GPL-2.0-only OR MIT");
        assertThat(verdict.status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    void withUsesTheExceptionsListFirst() {
        // GPL-2.0-only alone is DENIED, but the policy lists this exact combination as REVIEW.
        LicenseVerdict verdict = engine.evaluate("GPL-2.0-only WITH Classpath-exception-2.0");

        assertThat(verdict.status()).isEqualTo(PolicyStatus.REVIEW);
        assertThat(verdict.spdxExpression()).isEqualTo("GPL-2.0-only WITH Classpath-exception-2.0");
    }

    @Test
    void withLookupIgnoresCaseAndAliases() {
        assertThat(engine.evaluate("gpl-2.0 with classpath-exception-2.0").status())
                .isEqualTo(PolicyStatus.REVIEW);
    }

    @Test
    void withFallsBackToTheLicenceWhenTheExceptionIsNotListed() {
        assertThat(engine.evaluate("Apache-2.0 WITH LLVM-exception").status()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(engine.evaluate("GPL-3.0-only WITH GCC-exception-3.1").status()).isEqualTo(PolicyStatus.DENIED);
    }

    @Test
    void malformedExpressionIsUnknownWithTheParseError() {
        LicenseVerdict verdict = engine.evaluate("MIT AND");

        assertThat(verdict.status()).isEqualTo(PolicyStatus.UNKNOWN);
        assertThat(verdict.spdxExpression()).isNull();
        assertThat(verdict.error()).isNotBlank();
    }

    @Test
    void missingLicenceIsUnknownWithAReason() {
        assertThat(engine.evaluate(null).status()).isEqualTo(PolicyStatus.UNKNOWN);
        assertThat(engine.evaluate(null).error()).isNotBlank();
        assertThat(engine.evaluate("  ").status()).isEqualTo(PolicyStatus.UNKNOWN);
    }
}
