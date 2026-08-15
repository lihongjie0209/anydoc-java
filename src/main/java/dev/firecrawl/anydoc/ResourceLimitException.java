package dev.firecrawl.anydoc;

/**
 * A fixed safety limit was crossed: decompression, nesting depth, node count, repeat expansion, or
 * retained asset bytes. {@link #limit()} names it.
 */
public final class ResourceLimitException extends ConvertException {

    private final String limit;

    ResourceLimitException(String message, String limit) {
        super(message, "resourceLimit");
        this.limit = limit;
    }

    /** The limit that was crossed, e.g. {@code max_entry_bytes}. */
    public String limit() {
        return limit;
    }
}
