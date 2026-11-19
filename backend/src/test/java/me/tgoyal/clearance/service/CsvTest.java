package me.tgoyal.clearance.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CsvTest {

    @Test
    void joinsPlainCellsWithCommas() {
        assertThat(Csv.row(Arrays.asList("lodash", "4.17.21", "MIT"))).isEqualTo("lodash,4.17.21,MIT");
    }

    @Test
    void nullIsAnEmptyCell() {
        assertThat(Csv.row(Arrays.asList("a", null, "c"))).isEqualTo("a,,c");
    }

    @Test
    void quotesCellsContainingCommasQuotesOrNewlines() {
        assertThat(Csv.row(Arrays.asList("x, y", "say \"hi\"", "two\nlines")))
                .isEqualTo("\"x, y\",\"say \"\"hi\"\"\",\"two\nlines\"");
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        // A reviewer comment such as "=HYPERLINK(...)" must not run when the CSV is opened in Excel.
        assertThat(Csv.row(Arrays.asList("=1+1", "+cmd", "-x", "@sum"))).isEqualTo("'=1+1,'+cmd,'-x,'@sum");
    }
}
