package dev.firecrawl.anydoc;

/** The marker family a list uses in the source document. */
public enum MarkerKind {
    BULLET("bullet"),
    DECIMAL("decimal"),
    LOWER_ALPHA("lower_alpha"),
    UPPER_ALPHA("upper_alpha"),
    LOWER_ROMAN("lower_roman"),
    UPPER_ROMAN("upper_roman");

    private final String wireName;

    MarkerKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    /** True for every kind but {@link #BULLET}. */
    public boolean ordered() {
        return this != BULLET;
    }

    /**
     * The marker text for ordinal {@code n} (1-based), without trailing space: {@code 3.}, {@code
     * c.}, {@code iv.}; bullets have no ordinal text.
     */
    public String label(long n) {
        if (this == BULLET) {
            return "-";
        }
        return ordinal(n) + ".";
    }

    /** The bare ordinal text for {@code n} without punctuation: {@code 3}, {@code c}, {@code iv}. */
    public String ordinal(long n) {
        return switch (this) {
            case BULLET -> "-";
            case DECIMAL -> Long.toString(n);
            case LOWER_ALPHA -> alpha(n);
            case UPPER_ALPHA -> alpha(n).toUpperCase();
            case LOWER_ROMAN -> roman(n);
            case UPPER_ROMAN -> roman(n).toUpperCase();
        };
    }

    /** 1 → {@code a}, 26 → {@code z}, 27 → {@code aa} (bijective base 26). */
    static String alpha(long n) {
        if (n == 0) {
            return "0";
        }
        StringBuilder out = new StringBuilder();
        while (n > 0) {
            n -= 1;
            out.append((char) ('a' + (n % 26)));
            n /= 26;
        }
        return out.reverse().toString();
    }

    static String roman(long n) {
        if (n == 0 || n > 3999) {
            return Long.toString(n);
        }
        long remaining = n;
        StringBuilder out = new StringBuilder();
        long[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                out.append(numerals[i]);
                remaining -= values[i];
            }
        }
        return out.toString();
    }
}
