package me.tgoyal.clearance.parser;

import java.util.List;

/** What came out of one uploaded file: its ecosystem and its de-duplicated dependencies. */
public record ParsedFile(Ecosystem ecosystem, List<Dependency> dependencies) {
}
