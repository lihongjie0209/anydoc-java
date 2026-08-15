package dev.firecrawl.anydoc;

/** What a table is for. */
public enum TableKind {
    /** A real data table. */
    DATA("data"),
    /** Layout scaffolding (text boxes, positioning tables). */
    LAYOUT("layout");

    private final String wireName;

    TableKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
