package dev.firecrawl.anydoc;

import java.util.Objects;

/** Where a link points. */
public interface LinkTarget {

    /** {@code external}, {@code relative}, or {@code anchor}. */
    String kind();

    /** The URL, relative reference, or anchor id. */
    String value();

    /** True when the target string is empty, whichever kind it is. */
    default boolean isEmpty() {
        return value().isEmpty();
    }

    /** Absolute URL with a scheme. */
    final class External implements LinkTarget {
        private final String value;

        public External(String value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return "external";
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof External)) {
                return false;
            }
            return value.equals(((External) other).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "External[value=" + value + "]";
        }
    }

    /** Scheme-less relative reference, preserved as written. */
    final class Relative implements LinkTarget {
        private final String value;

        public Relative(String value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return "relative";
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Relative)) {
                return false;
            }
            return value.equals(((Relative) other).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "Relative[value=" + value + "]";
        }
    }

    /** Internal target: a heading anchor or an {@link Inline.Anchor}. */
    final class Anchor implements LinkTarget {
        private final String value;

        public Anchor(String value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return "anchor";
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Anchor)) {
                return false;
            }
            return value.equals(((Anchor) other).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "Anchor[value=" + value + "]";
        }
    }
}
