package me.tgoyal.clearance.policy;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicensePolicyTest {

    private static LicensePolicy loadYaml(String yaml) {
        return LicensePolicy.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void loadsTheBundledPolicyFile() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/policy.yml")) {
            LicensePolicy policy = LicensePolicy.load(in);

            assertThat(policy.statusOf("MIT")).contains(PolicyStatus.APPROVED);
            assertThat(policy.statusOf("MPL-2.0")).contains(PolicyStatus.REVIEW);
            assertThat(policy.statusOf("AGPL-3.0-only")).contains(PolicyStatus.DENIED);
        }
    }

    @Test
    void anIdThePolicyDoesNotListHasNoStatus() {
        LicensePolicy policy = loadYaml("approved: [MIT]");

        assertThat(policy.statusOf("WTFPL")).isEmpty();
    }

    @Test
    void matchesIdsIgnoringCase() {
        LicensePolicy policy = loadYaml("approved: [MIT]");

        assertThat(policy.statusOf("mit")).contains(PolicyStatus.APPROVED);
        assertThat(policy.canonicalId("mit")).isEqualTo("MIT");
    }

    @Test
    void mapsAliasesToTheirSpdxId() {
        LicensePolicy policy = loadYaml("""
                approved: [Apache-2.0]
                aliases:
                  "The Apache Software License, Version 2.0": Apache-2.0
                """);

        assertThat(policy.canonicalId("The Apache Software License, Version 2.0")).isEqualTo("Apache-2.0");
        assertThat(policy.canonicalId("the apache software license, version 2.0")).isEqualTo("Apache-2.0");
    }

    @Test
    void leavesAnUnknownIdAsWritten() {
        LicensePolicy policy = loadYaml("approved: [MIT]");

        assertThat(policy.canonicalId("Some-Licence")).isEqualTo("Some-Licence");
    }

    @Test
    void looksUpExceptionsIgnoringCase() {
        LicensePolicy policy = loadYaml("""
                denied: [GPL-2.0-only]
                exceptions:
                  "GPL-2.0-only WITH Classpath-exception-2.0": REVIEW
                """);

        assertThat(policy.exceptionStatus("gpl-2.0-only with classpath-exception-2.0")).contains(PolicyStatus.REVIEW);
        assertThat(policy.exceptionStatus("GPL-2.0-only WITH Other-exception")).isEmpty();
    }

    @Test
    void sameLicenceInTwoListsFailsLoudly() {
        assertThatThrownBy(() -> loadYaml("""
                approved: [MIT, Apache-2.0]
                denied: [GPL-3.0-only, MIT]
                """))
                .isInstanceOf(PolicyConfigException.class)
                .hasMessageContaining("MIT")
                .hasMessageContaining("approved")
                .hasMessageContaining("denied");
    }

    @Test
    void sameLicenceInTwoListsFailsEvenWithDifferentCase() {
        assertThatThrownBy(() -> loadYaml("""
                approved: [MIT]
                review: [mit]
                """))
                .isInstanceOf(PolicyConfigException.class);
    }

    @Test
    void misspelledSectionFails() {
        // "aproved" is a typo; silently ignoring it would approve nothing.
        assertThatThrownBy(() -> loadYaml("aproved: [MIT]"))
                .isInstanceOf(PolicyConfigException.class);
    }

    @Test
    void invalidExceptionVerdictFails() {
        assertThatThrownBy(() -> loadYaml("""
                exceptions:
                  "GPL-2.0-only WITH Classpath-exception-2.0": MAYBE
                """))
                .isInstanceOf(PolicyConfigException.class);
    }

    @Test
    void brokenYamlFails() {
        assertThatThrownBy(() -> loadYaml("approved: [MIT"))
                .isInstanceOf(PolicyConfigException.class);
    }
}
