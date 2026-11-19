package me.tgoyal.clearance.spdx;

/**
 * A parsed SPDX licence expression, as a tree.
 *
 * <p>The interface is sealed: these four records are the only node types, so a
 * {@code switch} over an {@code SpdxExpression} can be checked for completeness
 * by the compiler.
 *
 * <pre>
 *   "(MIT OR GPL-3.0-only) AND LGPL-2.1-only"
 *
 *              And
 *             /   \
 *           Or     License(LGPL-2.1-only)
 *          /  \
 *   License   License
 *    (MIT)  (GPL-3.0-only)
 * </pre>
 */
public sealed interface SpdxExpression {

    /** A single licence identifier, such as {@code MIT}. */
    record License(String id) implements SpdxExpression {
        @Override
        public String toString() {
            return id;
        }
    }

    /** A licence with an exception, such as {@code GPL-2.0-only WITH Classpath-exception-2.0}. */
    record With(License license, String exception) implements SpdxExpression {
        @Override
        public String toString() {
            return license + " WITH " + exception;
        }
    }

    /** Both sides apply: you must meet both licences. */
    record And(SpdxExpression left, SpdxExpression right) implements SpdxExpression {
        @Override
        public String toString() {
            return wrap(left) + " AND " + wrap(right);
        }

        /** An OR inside an AND needs parentheses to keep its meaning. */
        private static String wrap(SpdxExpression side) {
            return side instanceof Or ? "(" + side + ")" : side.toString();
        }
    }

    /** Either side applies: you may choose either licence. */
    record Or(SpdxExpression left, SpdxExpression right) implements SpdxExpression {
        @Override
        public String toString() {
            return left + " OR " + right;
        }
    }
}
