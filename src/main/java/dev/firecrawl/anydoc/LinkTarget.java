package dev.firecrawl.anydoc;

import java.util.Objects;

/** Where a link points. */
public sealed interface LinkTarget permits LinkTarget.External, LinkTarget.Relative, LinkTarget.Anchor {

    /** {@code external}, {@code relative}, or {@code anchor}. */
    String kind();

    /** The URL, relative reference, or anchor id. */
    String value();

    /** Absolute URL with a scheme. */
    record External(String value) implements LinkTarget {
        public External {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return "external";
        }
    }

    /** Scheme-less relative reference, preserved as written. */
    record Relative(String value) implements LinkTarget {
        public Relative {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return "relative";
        }
    }

    /** Internal target: a heading anchor or an {@link Inline.Anchor}. */
    record Anchor(String value) implements LinkTarget {
        public Anchor {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public String kind() {
            return "anchor";
        }
    }
}
