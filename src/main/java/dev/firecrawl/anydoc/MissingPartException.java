package dev.firecrawl.anydoc;

/** A part required for any meaningful output is absent. {@link #part()} names it. */
public final class MissingPartException extends ConvertException {

    private final String part;

    MissingPartException(String message, String part) {
        super(message, "missingPart");
        this.part = part;
    }

    /** The part or stream that is missing. */
    public String part() {
        return part;
    }
}
