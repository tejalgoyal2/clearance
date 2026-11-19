package me.tgoyal.clearance.spdx;

/**
 * One piece of an SPDX expression, produced by {@link SpdxTokenizer}.
 *
 * @param type     what kind of token this is
 * @param text     the text exactly as written
 * @param position zero-based character offset, used in error messages
 */
public record Token(Type type, String text, int position) {

    public enum Type {
        IDENTIFIER,
        AND,
        OR,
        WITH,
        OPEN_PAREN,
        CLOSE_PAREN
    }
}
