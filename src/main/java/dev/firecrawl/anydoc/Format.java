package dev.firecrawl.anydoc;

import java.util.Locale;
import java.util.Optional;

/**
 * Input format, named after the extension that identifies it. Container variants that share a
 * parser ({@code .docm}, {@code .xlsm}, {@code .ppsx}, ...) map onto these via {@link
 * Anydoc#formatFromBytes} or {@link Anydoc#formatFromExtension}.
 *
 * <p>PDF conversion produces Markdown directly: {@link Anydoc#toDocument} is unsupported for PDFs.
 * Scanned or image-only PDFs (needing OCR) error as unsupported.
 */
public enum Format {
    DOC("doc"),
    DOCX("docx"),
    ODT("odt"),
    PDF("pdf"),
    PPT("ppt"),
    PPTX("pptx"),
    RTF("rtf"),
    EPUB("epub"),
    XLSX("xlsx"),
    ODS("ods"),
    ODP("odp"),
    CSV("csv");

    private final String wireName;

    Format(String wireName) {
        this.wireName = wireName;
    }

    /** The lowercase name other language bindings use ({@code "docx"}, {@code "xlsx"}, ...). */
    public String wireName() {
        return wireName;
    }

    /** Parse a lowercase wire name such as {@code "docx"} or {@code "xlsx"}. */
    public static Optional<Format> fromWireName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String key = name.toLowerCase(Locale.ROOT);
        for (Format format : values()) {
            if (format.wireName.equals(key)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
