package me.tgoyal.clearance.parser;

/**
 * One dependency found in a dependency file.
 *
 * <p>A record, so {@code equals} and {@code hashCode} compare all three fields.
 *
 * @param name            {@code groupId:artifactId} for Maven, the package name for npm
 * @param version         the version as declared, or {@code null} if the file does not say
 * @param declaredLicense the licence written in the file itself (npm lockfiles only), or {@code null}
 */
public record Dependency(String name, String version, String declaredLicense) {
}
