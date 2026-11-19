package me.tgoyal.clearance.spdx;

import me.tgoyal.clearance.spdx.SpdxExpression.And;
import me.tgoyal.clearance.spdx.SpdxExpression.License;
import me.tgoyal.clearance.spdx.SpdxExpression.Or;
import me.tgoyal.clearance.spdx.SpdxExpression.With;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpdxParserTest {

    private final SpdxParser parser = new SpdxParser();

    @Test
    void parsesASingleIdentifier() {
        assertThat(parser.parse("MIT")).isEqualTo(new License("MIT"));
    }

    @Test
    void parsesOr() {
        assertThat(parser.parse("MIT OR GPL-3.0-only"))
                .isEqualTo(new Or(new License("MIT"), new License("GPL-3.0-only")));
    }

    @Test
    void parsesAnd() {
        assertThat(parser.parse("Apache-2.0 AND GPL-3.0-only"))
                .isEqualTo(new And(new License("Apache-2.0"), new License("GPL-3.0-only")));
    }

    @Test
    void andBindsTighterThanOr() {
        // "A OR B AND C" means "A OR (B AND C)", not "(A OR B) AND C".
        assertThat(parser.parse("MIT OR Apache-2.0 AND GPL-3.0-only"))
                .isEqualTo(new Or(
                        new License("MIT"),
                        new And(new License("Apache-2.0"), new License("GPL-3.0-only"))));
    }

    @Test
    void parenthesesOverridePrecedence() {
        assertThat(parser.parse("(MIT OR GPL-3.0-only) AND LGPL-2.1-only"))
                .isEqualTo(new And(
                        new Or(new License("MIT"), new License("GPL-3.0-only")),
                        new License("LGPL-2.1-only")));
    }

    @Test
    void parsesWith() {
        assertThat(parser.parse("GPL-2.0-only WITH Classpath-exception-2.0"))
                .isEqualTo(new With(new License("GPL-2.0-only"), "Classpath-exception-2.0"));
    }

    @Test
    void withBindsTighterThanAnd() {
        assertThat(parser.parse("MIT AND GPL-2.0-only WITH Classpath-exception-2.0"))
                .isEqualTo(new And(
                        new License("MIT"),
                        new With(new License("GPL-2.0-only"), "Classpath-exception-2.0")));
    }

    @Test
    void sameOperatorGroupsFromTheLeft() {
        assertThat(parser.parse("MIT OR ISC OR 0BSD"))
                .isEqualTo(new Or(new Or(new License("MIT"), new License("ISC")), new License("0BSD")));
    }

    @Test
    void keepsIdentifierCaseAsWritten() {
        // Matching ids case-insensitively is the policy's job, not the parser's.
        assertThat(parser.parse("mit")).isEqualTo(new License("mit"));
    }

    @Test
    void acceptsLowercaseOperators() {
        assertThat(parser.parse("mit or apache-2.0"))
                .isEqualTo(new Or(new License("mit"), new License("apache-2.0")));
    }

    @Test
    void ignoresExtraSpaces() {
        assertThat(parser.parse("   ( MIT    OR   ISC )   AND  Apache-2.0  "))
                .isEqualTo(new And(new Or(new License("MIT"), new License("ISC")), new License("Apache-2.0")));
    }

    @Test
    void acceptsAnIdentifierThePolicyHasNeverHeardOf() {
        // The parser checks shape, not meaning: an unknown id is still a valid expression.
        assertThat(parser.parse("Some-Custom-Licence-1.0")).isEqualTo(new License("Some-Custom-Licence-1.0"));
    }

    @Test
    void printsTheTreeBackAsAnExpression() {
        assertThat(parser.parse("( MIT  OR GPL-3.0-only )  AND LGPL-2.1-only").toString())
                .isEqualTo("(MIT OR GPL-3.0-only) AND LGPL-2.1-only");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                       // empty
            "   ",                    // only spaces
            "MIT AND",                // dangling operator at the end
            "OR MIT",                 // dangling operator at the start
            "MIT AND OR ISC",         // two operators in a row
            "(MIT OR ISC",            // unclosed parenthesis
            "MIT OR ISC)",            // unopened parenthesis
            "()",                     // empty parentheses
            "MIT ISC",                // two identifiers with no operator
            "MIT WITH",               // WITH with no exception
            "(MIT OR ISC) WITH Foo",  // WITH may only follow a single licence
            "MIT WITH (Foo)"          // the exception must be an identifier
    })
    void rejectsMalformedExpressions(String expression) {
        assertThatThrownBy(() -> parser.parse(expression))
                .isInstanceOf(SpdxParseException.class);
    }

    @Test
    void errorMessageSaysWhatWentWrong() {
        assertThatThrownBy(() -> parser.parse("MIT AND"))
                .isInstanceOf(SpdxParseException.class)
                .hasMessageContaining("end of expression");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(SpdxParseException.class);
    }
}
