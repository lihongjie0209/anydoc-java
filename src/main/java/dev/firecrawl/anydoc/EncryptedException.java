package dev.firecrawl.anydoc;

/** The document is encrypted or password-protected. */
public final class EncryptedException extends ConvertException {

    EncryptedException(String message) {
        super(message, "encrypted");
    }
}
