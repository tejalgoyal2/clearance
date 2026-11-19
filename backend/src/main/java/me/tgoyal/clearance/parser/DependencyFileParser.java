package me.tgoyal.clearance.parser;

import java.io.InputStream;
import java.util.List;

/**
 * Reads the dependencies out of one kind of dependency file.
 *
 * <p>This is the strategy pattern: each implementation knows one file format, and
 * {@link DependencyParserRegistry} picks the right one by filename. Adding Gradle
 * support means adding a class, not editing an if/else chain.
 */
public interface DependencyFileParser {

    /** True if this parser handles a file with this name, e.g. {@code pom.xml}. */
    boolean supports(String filename);

    /** The ecosystem whose files this parser reads. */
    Ecosystem ecosystem();

    /**
     * Parses the file. Does not close the stream: whoever opened it closes it.
     *
     * @throws DependencyFileException if the file is malformed
     */
    List<Dependency> parse(InputStream input);
}
