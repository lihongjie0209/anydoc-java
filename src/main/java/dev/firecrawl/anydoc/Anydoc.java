package dev.firecrawl.anydoc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Convert documents to GitHub-Flavored Markdown.
 *
 * <pre>{@code
 * String markdown = Anydoc.toMarkdown(Path.of("report.docx"));
 * String fromBytes = Anydoc.toMarkdownBytes(bytes);
 * String fromCsv = Anydoc.toMarkdownBytes(bytes, Format.CSV);
 * Document document = Anydoc.toDocument(bytes);
 * }</pre>
 */
public final class Anydoc {

    static {
        NativeLoader.load();
    }

    private Anydoc() {}

    /**
     * Convert a document file to Markdown. The format is detected from the file content; the
     * extension is the fallback for signature-less formats (CSV) and unrecognizable containers.
     */
    public static String toMarkdown(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        return toMarkdown(path.toString());
    }

    /**
     * Convert a document file to Markdown. The format is detected from the file content; the
     * extension is the fallback for signature-less formats (CSV) and unrecognizable containers.
     */
    public static String toMarkdown(String path) throws IOException {
        Objects.requireNonNull(path, "path");
        return nativeToMarkdown(path);
    }

    /**
     * Convert an in-memory document to Markdown. The format is detected from the content, which
     * signature-less formats (CSV) have to name explicitly via {@link #toMarkdownBytes(byte[],
     * Format)}.
     */
    public static String toMarkdownBytes(byte[] data) {
        return toMarkdownBytes(data, null);
    }

    /**
     * Convert an in-memory document to Markdown. Without a format, it is detected from the
     * content, which signature-less formats (CSV) have to name explicitly.
     */
    public static String toMarkdownBytes(byte[] data, Format format) {
        Objects.requireNonNull(data, "data");
        return nativeToMarkdownBytes(data, format);
    }

    /**
     * Parse an in-memory document into the document model, which also carries the embedded assets.
     * The format is detected from the content.
     *
     * <p>Unsupported for {@link Format#PDF}: PDF conversion produces Markdown directly and has no
     * document-model form; use {@link #toMarkdownBytes(byte[])}.
     */
    public static Document toDocument(byte[] data) {
        return toDocument(data, null);
    }

    /**
     * Parse an in-memory document into the document model, which also carries the embedded assets.
     * Without a format, it is detected from the content.
     *
     * <p>Unsupported for {@link Format#PDF}: PDF conversion produces Markdown directly and has no
     * document-model form; use {@link #toMarkdownBytes(byte[], Format)}.
     */
    public static Document toDocument(byte[] data, Format format) {
        Objects.requireNonNull(data, "data");
        return nativeToDocument(data, format);
    }

    /**
     * Detect the format from the content itself: the signature and identity each container
     * specification designates (PDF header, RTF open group, OLE stream names, ZIP package
     * mimetype/content types). Plain-text formats (CSV) carry no signature and return empty; so
     * does anything unrecognized.
     */
    public static Optional<Format> formatFromBytes(byte[] data) {
        Objects.requireNonNull(data, "data");
        return Optional.ofNullable(nativeFormatFromBytes(data));
    }

    /** The format an extension names, with or without a leading dot. */
    public static Optional<Format> formatFromExtension(String extension) {
        Objects.requireNonNull(extension, "extension");
        return Optional.ofNullable(nativeFormatFromExtension(extension));
    }

    /** The format a path's extension names. */
    public static Optional<Format> formatFromPath(Path path) {
        Objects.requireNonNull(path, "path");
        return formatFromPath(path.toString());
    }

    /** The format a path's extension names. */
    public static Optional<Format> formatFromPath(String path) {
        Objects.requireNonNull(path, "path");
        return Optional.ofNullable(nativeFormatFromPath(path));
    }

    private static native String nativeToMarkdown(String path) throws IOException;

    private static native String nativeToMarkdownBytes(byte[] data, Format format);

    private static native Document nativeToDocument(byte[] data, Format format);

    private static native Format nativeFormatFromBytes(byte[] data);

    private static native Format nativeFormatFromExtension(String extension);

    private static native Format nativeFormatFromPath(String path);
}
