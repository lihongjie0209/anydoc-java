package dev.firecrawl.anydoc;

/** Where the source document places a note. */
public enum NoteKind {
    FOOTNOTE("footnote"),
    ENDNOTE("endnote");

    private final String wireName;

    NoteKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
