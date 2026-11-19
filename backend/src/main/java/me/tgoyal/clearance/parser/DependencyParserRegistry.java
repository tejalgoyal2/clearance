package me.tgoyal.clearance.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Picks the right {@link DependencyFileParser} for an uploaded file, runs it, and removes
 * duplicate name+version pairs.
 *
 * <p>The registry only knows the interface. In Spring, every parser bean is injected as
 * the list, so a new file format needs no change here.
 */
public class DependencyParserRegistry {

    private final List<DependencyFileParser> parsers;

    public DependencyParserRegistry(List<DependencyFileParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public Optional<DependencyFileParser> parserFor(String filename) {
        return parsers.stream()
                .filter(parser -> parser.supports(filename))
                .findFirst();
    }

    /**
     * @throws DependencyFileException if no parser supports the file, or the file is malformed
     */
    public ParsedFile parse(String filename, InputStream input) {
        DependencyFileParser parser = parserFor(filename)
                .orElseThrow(() -> new DependencyFileException(
                        "Unsupported file '" + filename + "'. Upload a pom.xml or a package-lock.json."));

        List<Dependency> unique = removeDuplicates(parser.parse(input));
        return new ParsedFile(parser.ecosystem(), unique);
    }

    /**
     * Keeps the first dependency for each name+version, in the original order.
     * Builds a new list rather than removing from the one being iterated.
     */
    private static List<Dependency> removeDuplicates(List<Dependency> dependencies) {
        Set<String> seen = new HashSet<>();
        List<Dependency> unique = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            String key = dependency.name() + "@" + dependency.version();
            if (seen.add(key)) {
                unique.add(dependency);
            }
        }
        return unique;
    }
}
