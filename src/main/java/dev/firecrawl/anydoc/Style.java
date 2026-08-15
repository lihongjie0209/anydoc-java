package dev.firecrawl.anydoc;

import java.util.Objects;

/** Fully resolved character style. */
public final class Style {

    /** No toggle set. */
    public static final Style PLAIN = new Style(false, false, false, false);

    private final boolean bold;
    private final boolean italic;
    private final boolean strike;
    private final boolean code;

    public Style(boolean bold, boolean italic, boolean strike, boolean code) {
        this.bold = bold;
        this.italic = italic;
        this.strike = strike;
        this.code = code;
    }

    public boolean bold() {
        return bold;
    }

    public boolean italic() {
        return italic;
    }

    public boolean strike() {
        return strike;
    }

    public boolean code() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Style)) {
            return false;
        }
        Style style = (Style) other;
        return bold == style.bold
                && italic == style.italic
                && strike == style.strike
                && code == style.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, strike, code);
    }

    @Override
    public String toString() {
        return "Style[bold=" + bold + ", italic=" + italic + ", strike=" + strike + ", code=" + code + "]";
    }
}
