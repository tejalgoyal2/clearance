package me.tgoyal.clearance.service;

import java.util.ArrayList;
import java.util.List;

/** Minimal CSV writing (RFC 4180), so the report has no extra library dependency. */
final class Csv {

    private Csv() {
    }

    static String row(List<String> cells) {
        List<String> escaped = new ArrayList<>();
        for (String cell : cells) {
            escaped.add(cell(cell));
        }
        return String.join(",", escaped);
    }

    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        // CSV injection: spreadsheet apps run a cell starting with = + - @ as a formula.
        // A leading apostrophe makes them show it as text instead.
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        // A cell with a comma, quote or line break is wrapped in quotes, with quotes doubled.
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
