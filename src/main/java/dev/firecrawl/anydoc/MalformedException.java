package dev.firecrawl.anydoc;

import java.util.Optional;

/**
 * The document is structurally unusable: no meaningful content could be extracted. {@link #part()}
 * names the package part or stream at fault, and is empty when no single part is.
 */
public final class MalformedException extends ConvertException {

    private final String part;

    MalformedException(String message, String part) {
        super(message, "malformed");
        this.part = part;
    }

    /** The package part or stream at fault, if one is. */
    public Optional<String> part() {
        return Optional.ofNullable(part);
    }
}
