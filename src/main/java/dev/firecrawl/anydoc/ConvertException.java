package dev.firecrawl.anydoc;

/**
 * Meaningful conversion was impossible. Catch this to handle every kind of failure, or one of the
 * subclasses to single one out. An unreadable file from {@link Anydoc#toMarkdown} throws {@link
 * java.io.IOException} instead.
 */
public class ConvertException extends RuntimeException {

    private final String code;

    ConvertException(String message) {
        this(message, "convert");
    }

    ConvertException(String message, String code) {
        super(message);
        this.code = code;
    }

    /**
     * Stable, machine-readable name for the failure, the same strings Node and wasm publish as
     * {@code error.code}: {@code unsupported}, {@code malformed}, {@code encrypted}, {@code
     * resourceLimit}, {@code missingPart}.
     */
    public String code() {
        return code;
    }
}
