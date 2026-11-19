package me.tgoyal.clearance.license;

/**
 * The outcome of looking up one dependency's licence.
 *
 * @param license the licence string as found, or {@code null} if none was found
 * @param source  where it was found ("lockfile", "deps.dev"), or {@code null}
 * @param reason  why nothing was found, or {@code null} if something was
 */
public record LicenseLookup(String license, String source, String reason) {

    public static LicenseLookup found(String license, String source) {
        return new LicenseLookup(license, source, null);
    }

    public static LicenseLookup notFound(String reason) {
        return new LicenseLookup(null, null, reason);
    }

    public boolean isFound() {
        return license != null;
    }
}
