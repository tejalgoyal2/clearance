package me.tgoyal.clearance.license;

import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.Ecosystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Asks each resolver in turn and returns the first licence found. If none finds one,
 * the result keeps every reason, so a reviewer can see why the licence is UNKNOWN.
 */
public class ChainedLicenseResolver implements LicenseResolver {

    private final List<LicenseResolver> resolvers;

    public ChainedLicenseResolver(List<LicenseResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public LicenseLookup resolve(Ecosystem ecosystem, Dependency dependency) {
        List<String> reasons = new ArrayList<>();
        for (LicenseResolver resolver : resolvers) {
            LicenseLookup lookup = resolver.resolve(ecosystem, dependency);
            if (lookup.isFound()) {
                return lookup;
            }
            reasons.add(lookup.reason());
        }
        return LicenseLookup.notFound(String.join("; ", reasons));
    }
}
