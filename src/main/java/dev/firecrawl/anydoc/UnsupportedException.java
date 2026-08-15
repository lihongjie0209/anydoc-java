package dev.firecrawl.anydoc;

/**
 * The format is unknown, or cannot be converted at all: a scanned or image-only PDF needs OCR,
 * which anydoc does not do.
 */
public final class UnsupportedException extends ConvertException {

    UnsupportedException(String message) {
        super(message, "unsupported");
    }
}
