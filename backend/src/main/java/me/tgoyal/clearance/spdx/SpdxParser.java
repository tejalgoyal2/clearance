package me.tgoyal.clearance.spdx;

import me.tgoyal.clearance.spdx.SpdxExpression.And;
import me.tgoyal.clearance.spdx.SpdxExpression.License;
import me.tgoyal.clearance.spdx.SpdxExpression.Or;
import me.tgoyal.clearance.spdx.SpdxExpression.With;
import me.tgoyal.clearance.spdx.Token.Type;

import java.util.List;

/**
 * Parses an SPDX licence expression into a tree, using recursive descent.
 *
 * <p>Grammar, from loosest to tightest binding. Each rule is one method below:
 *
 * <pre>
 *   orExpression   = andExpression { "OR" andExpression }
 *   andExpression  = withExpression { "AND" withExpression }
 *   withExpression = IDENTIFIER [ "WITH" IDENTIFIER ]
 *                  | "(" orExpression ")"
 * </pre>
 *
 * <p>Because {@code orExpression} calls {@code andExpression}, an AND is always grouped
 * before the OR around it is: "A OR B AND C" becomes Or(A, And(B, C)).
 *
 * <p>A new {@link Cursor} is created for every call, so one parser instance is safe to
 * share between threads.
 */
public class SpdxParser {

    private final SpdxTokenizer tokenizer = new SpdxTokenizer();

    /**
     * @throws SpdxParseException if the expression is empty or malformed
     */
    public SpdxExpression parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new SpdxParseException("Licence expression is empty");
        }

        Cursor cursor = new Cursor(tokenizer.tokenize(expression));
        SpdxExpression tree = orExpression(cursor);

        if (cursor.hasMore()) {
            Token extra = cursor.peek();
            throw new SpdxParseException(
                    "Unexpected '" + extra.text() + "' at position " + extra.position());
        }
        return tree;
    }

    private SpdxExpression orExpression(Cursor cursor) {
        SpdxExpression left = andExpression(cursor);
        while (cursor.nextIs(Type.OR)) {
            cursor.advance();
            SpdxExpression right = andExpression(cursor);
            left = new Or(left, right);
        }
        return left;
    }

    private SpdxExpression andExpression(Cursor cursor) {
        SpdxExpression left = withExpression(cursor);
        while (cursor.nextIs(Type.AND)) {
            cursor.advance();
            SpdxExpression right = withExpression(cursor);
            left = new And(left, right);
        }
        return left;
    }

    private SpdxExpression withExpression(Cursor cursor) {
        if (cursor.nextIs(Type.OPEN_PAREN)) {
            cursor.advance();
            SpdxExpression inner = orExpression(cursor);
            cursor.expect(Type.CLOSE_PAREN, "')'");
            return inner;
        }

        License license = new License(cursor.expect(Type.IDENTIFIER, "a licence identifier").text());

        if (cursor.nextIs(Type.WITH)) {
            cursor.advance();
            String exception = cursor.expect(Type.IDENTIFIER, "an exception identifier").text();
            return new With(license, exception);
        }
        return license;
    }

    /** Walks through the token list one token at a time. */
    private static class Cursor {

        private final List<Token> tokens;
        private int index = 0;

        Cursor(List<Token> tokens) {
            this.tokens = tokens;
        }

        boolean hasMore() {
            return index < tokens.size();
        }

        Token peek() {
            return tokens.get(index);
        }

        boolean nextIs(Type type) {
            return hasMore() && peek().type() == type;
        }

        void advance() {
            index++;
        }

        /** Consumes the next token if it has the given type, otherwise fails with a clear message. */
        Token expect(Type type, String description) {
            if (!hasMore()) {
                throw new SpdxParseException("Expected " + description + " but reached the end of expression");
            }
            Token token = peek();
            if (token.type() != type) {
                throw new SpdxParseException("Expected " + description + " but found '"
                        + token.text() + "' at position " + token.position());
            }
            advance();
            return token;
        }
    }
}
