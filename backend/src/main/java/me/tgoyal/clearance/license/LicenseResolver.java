package me.tgoyal.clearance.license;

import me.tgoyal.clearance.parser.Dependency;
import me.tgoyal.clearance.parser.Ecosystem;

/**
 * Finds the licence of a dependency.
 *
 * <p>Implementations must never throw for a lookup that fails: a timeout, a 404 or a
 * network error becomes {@link LicenseLookup#notFound(String)} with a reason. One
 * unreachable package must not fail a whole scan.
 */
public interface LicenseResolver {

    LicenseLookup resolve(Ecosystem ecosystem, Dependency dependency);
}
