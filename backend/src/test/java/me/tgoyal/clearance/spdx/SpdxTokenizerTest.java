package me.tgoyal.clearance.spdx;

import me.tgoyal.clearance.spdx.Token.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpdxTokenizerTest {

    private final SpdxTokenizer tokenizer = new SpdxTokenizer();

    @Test
    void splitsOnSpacesAndParentheses() {
        List<Token> tokens = tokenizer.tokenize("(MIT OR ISC) AND Apache-2.0");

        assertThat(tokens).extracting(Token::type).containsExactly(
                Type.OPEN_PAREN, Type.IDENTIFIER, Type.OR, Type.IDENTIFIER, Type.CLOSE_PAREN,
                Type.AND, Type.IDENTIFIER);
        assertThat(tokens).extracting(Token::text).containsExactly(
                "(", "MIT", "OR", "ISC", ")", "AND", "Apache-2.0");
    }

    @Test
    void recordsWhereEachTokenStarts() {
        assertThat(tokenizer.tokenize("MIT  OR ISC")).extracting(Token::position).containsExactly(0, 5, 8);
    }

    @Test
    void recognisesOperatorsInAnyCase() {
        assertThat(tokenizer.tokenize("a and b or c with d")).extracting(Token::type).containsExactly(
                Type.IDENTIFIER, Type.AND, Type.IDENTIFIER, Type.OR, Type.IDENTIFIER, Type.WITH, Type.IDENTIFIER);
    }

    @Test
    void keepsPlusAndDotsInsideIdentifiers() {
        assertThat(tokenizer.tokenize("GPL-2.0+")).extracting(Token::text).containsExactly("GPL-2.0+");
    }

    @Test
    void returnsNothingForBlankInput() {
        assertThat(tokenizer.tokenize("   ")).isEmpty();
    }
}
