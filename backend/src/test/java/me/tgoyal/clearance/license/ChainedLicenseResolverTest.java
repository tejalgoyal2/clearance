package me.tgoyal.clearance.license;

import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.Ecosystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChainedLicenseResolverTest {

    private final LicenseResolver first = mock(LicenseResolver.class);
    private final LicenseResolver second = mock(LicenseResolver.class);
    private final ChainedLicenseResolver chain = new ChainedLicenseResolver(List.of(first, second));

    private final Dependency dependency = new Dependency("x", "1.0.0", null);

    @Test
    void stopsAtTheFirstResolverThatFindsALicence() {
        when(first.resolve(any(), any())).thenReturn(LicenseLookup.found("MIT", "lockfile"));

        LicenseLookup lookup = chain.resolve(Ecosystem.NPM, dependency);

        assertThat(lookup).isEqualTo(LicenseLookup.found("MIT", "lockfile"));
        verifyNoInteractions(second);
    }

    @Test
    void triesTheNextResolverWhenTheFirstFindsNothing() {
        when(first.resolve(any(), any())).thenReturn(LicenseLookup.notFound("no licence in lockfile"));
        when(second.resolve(any(), any())).thenReturn(LicenseLookup.found("Apache-2.0", "deps.dev"));

        assertThat(chain.resolve(Ecosystem.NPM, dependency)).isEqualTo(LicenseLookup.found("Apache-2.0", "deps.dev"));
    }

    @Test
    void whenNothingFindsALicenceEveryReasonIsKept() {
        when(first.resolve(any(), any())).thenReturn(LicenseLookup.notFound("no licence in lockfile"));
        when(second.resolve(any(), any())).thenReturn(LicenseLookup.notFound("deps.dev timed out"));

        LicenseLookup lookup = chain.resolve(Ecosystem.NPM, dependency);

        assertThat(lookup.isFound()).isFalse();
        assertThat(lookup.reason()).isEqualTo("no licence in lockfile; deps.dev timed out");
    }
}
