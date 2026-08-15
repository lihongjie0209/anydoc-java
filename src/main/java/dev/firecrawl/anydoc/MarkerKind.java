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
}
