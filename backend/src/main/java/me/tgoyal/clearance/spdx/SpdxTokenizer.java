package me.tgoyal.clearance.spdx;

import me.tgoyal.clearance.spdx.Token.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits an SPDX expression into tokens.
 *
 * <p>Parentheses are tokens on their own; everything else is separated by whitespace.
 * The words AND, OR and WITH are operators in any case ("and" works too); any other
 * word is a licence or exception identifier.
 */
public class SpdxTokenizer {

    public List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < expression.length()) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '(') {
                tokens.add(new Token(Type.OPEN_PAREN, "(", i));
                i++;
            } else if (c == ')') {
                tokens.add(new Token(Type.CLOSE_PAREN, ")", i));
                i++;
            } else {
                int start = i;
                while (i < expression.length() && isWordChar(expression.charAt(i))) {
                    i++;
                }
                String word = expression.substring(start, i);
                tokens.add(new Token(typeOf(word), word, start));
            }
        }
        return tokens;
    }

    private static boolean isWordChar(char c) {
        return !Character.isWhitespace(c) && c != '(' && c != ')';
    }

    private static Type typeOf(String word) {
        return switch (word.toUpperCase()) {
            case "AND" -> Type.AND;
            case "OR" -> Type.OR;
            case "WITH" -> Type.WITH;
            default -> Type.IDENTIFIER;
        };
    }
}
