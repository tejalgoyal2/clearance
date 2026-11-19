package me.tgoyal.clearance.license;

import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.Ecosystem;

/** Uses the licence written in the dependency file itself. Free and offline, so it goes first. */
public class LockfileLicenseResolver implements LicenseResolver {

    @Override
    public LicenseLookup resolve(Ecosystem ecosystem, Dependency dependency) {
        String declared = dependency.declaredLicense();
        if (declared == null || declared.isBlank()) {
            return LicenseLookup.notFound("No licence in the dependency file");
        }
        return LicenseLookup.found(declared, "lockfile");
    }
}
