package dev.firecrawl.anydoc;

/** Fully resolved character style. */
public record Style(boolean bold, boolean italic, boolean strike, boolean code) {

    /** No toggle set. */
    public static final Style PLAIN = new Style(false, false, false, false);
}
