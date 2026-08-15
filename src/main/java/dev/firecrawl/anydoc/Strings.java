package dev.firecrawl.anydoc;

/** Java 8 stand-in for {@code String.isBlank()}. */
final class Strings {

    private Strings() {}

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
