package dev.firecrawl.anydoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class AnydocTest {

    private static final Path OUTLINE = Fixtures.path("docx", "handmade-outline.docx");
    private static final Path RICH = Fixtures.path("docx", "handmade-rich.docx");
    private static final Path CSV = Fixtures.path("csv", "sheet.csv");
    private static final Path ENCRYPTED = Fixtures.path("malformed", "encrypted--errors.odt");
    private static final Path ZIPBOMB = Fixtures.path("abuse", "zipbomb--errors.docx");

    @Test
    void toMarkdownDetectsTheFormatFromTheFileContent() throws IOException {
        String markdown = Anydoc.toMarkdown(OUTLINE);
        boolean heading = false;
        for (String line : markdown.split("\n")) {
            if (line.startsWith("# ")) {
                heading = true;
                break;
            }
        }
        assertTrue(heading, markdown);
    }

    @Test
    void toMarkdownBytesConvertsInMemory() throws IOException {
        String markdown = Anydoc.toMarkdownBytes(Files.readAllBytes(RICH), Format.DOCX);
        assertTrue(markdown.contains("| Quarter | Widgets |"), markdown);
    }

    @Test
    void toMarkdownBytesDetectsTheFormatWhenNoneIsNamed() throws IOException {
        String markdown = Anydoc.toMarkdownBytes(Files.readAllBytes(RICH));
        assertTrue(markdown.contains("| Quarter | Widgets |"), markdown);

        UnsupportedException unrecognized =
                assertThrows(
                        UnsupportedException.class,
                        () -> Anydoc.toMarkdownBytes(Files.readAllBytes(CSV)));
        assertTrue(unrecognized.getMessage().contains("unrecognized file content"), unrecognized.getMessage());
        assertInstanceOf(ConvertException.class, unrecognized);

        assertTrue(Anydoc.toMarkdownBytes(Files.readAllBytes(CSV), Format.CSV).contains("| --- |"));
    }

    @Test
    void toDocumentExposesTheDocumentModel() throws IOException {
        Document document = Anydoc.toDocument(Files.readAllBytes(OUTLINE), Format.DOCX);
        Block.Heading heading =
                document.blocks().stream()
                        .filter(Block.Heading.class::isInstance)
                        .map(Block.Heading.class::cast)
                        .findFirst()
                        .orElseThrow(NoSuchElementException::new);
        assertTrue(heading.level() >= 1 && heading.level() <= 6);
        Inline.Text first = assertInstanceOf(Inline.Text.class, heading.content().get(0));
        assertFalse(first.text().isEmpty());
        assertEquals("text", first.kind());
        assertInstanceOf(Boolean.class, first.style().bold());
    }

    @Test
    void toDocumentCarriesEmbeddedAssetsAsBytes() throws IOException {
        Document document = Anydoc.toDocument(Files.readAllBytes(RICH), Format.DOCX);
        Asset image =
                document.assets().stream()
                        .filter(asset -> asset.mediaType().equals("image/png"))
                        .findFirst()
                        .orElseThrow(NoSuchElementException::new);
        assertTrue(image.data().length > 0);
        assertEquals(image.id(), document.assets().indexOf(image));
    }

    @Test
    void formatDetectionReadsContentExtensionAndPath() throws IOException {
        assertEquals(Optional.of(Format.DOCX), Anydoc.formatFromBytes(Files.readAllBytes(RICH)));
        assertEquals(Optional.empty(), Anydoc.formatFromBytes(Files.readAllBytes(CSV)));
        assertEquals(Optional.of(Format.PPTX), Anydoc.formatFromExtension(".pptm"));
        assertEquals(Optional.of(Format.XLSX), Anydoc.formatFromExtension("xls"));
        assertEquals(Optional.of(Format.ODT), Anydoc.formatFromPath("report.odt"));
        assertEquals(Optional.empty(), Anydoc.formatFromPath("report.unknown"));
    }

    @Test
    void conversionErrorsRaiseTheSubclassThatNamesTheFailure() throws IOException {
        MalformedException malformed =
                assertThrows(
                        MalformedException.class,
                        () -> Anydoc.toMarkdownBytes("not a document".getBytes(StandardCharsets.UTF_8), Format.DOCX));
        assertInstanceOf(ConvertException.class, malformed);
        assertEquals("malformed", malformed.code());
        assertEquals(Optional.empty(), malformed.part());

        assertThrows(UnsupportedException.class, () -> Anydoc.toMarkdownBytes(Files.readAllBytes(CSV)));

        EncryptedException encrypted =
                assertThrows(
                        EncryptedException.class,
                        () -> Anydoc.toMarkdownBytes(Files.readAllBytes(ENCRYPTED), Format.ODT));
        assertEquals("encrypted", encrypted.code());

        ResourceLimitException limit =
                assertThrows(
                        ResourceLimitException.class,
                        () -> Anydoc.toMarkdownBytes(Files.readAllBytes(ZIPBOMB), Format.DOCX));
        assertEquals("max_entry_bytes", limit.limit());
        assertEquals("resourceLimit", limit.code());

        MissingPartException missing =
                assertThrows(MissingPartException.class, () -> Anydoc.toMarkdownBytes(emptyDocxPackage(), Format.DOCX));
        assertEquals("word/document.xml", missing.part());
        assertEquals("missingPart", missing.code());
    }

    @Test
    void unreadableFilesRaiseIoException() {
        assertThrows(NoSuchFileException.class, () -> Anydoc.toMarkdown("no-such-file.docx"));
        assertThrows(NoSuchFileException.class, () -> Anydoc.toMarkdown(Paths.get("no-such-file.docx")));
    }

    @Test
    void pathAndStringOverloadsAgree() throws IOException {
        assertEquals(Anydoc.toMarkdown(OUTLINE.toString()), Anydoc.toMarkdown(OUTLINE));
    }

    @Test
    void toDocumentRejectsEncryptedFilesToo() throws IOException {
        EncryptedException error =
                assertThrows(
                        EncryptedException.class,
                        () -> Anydoc.toDocument(Files.readAllBytes(ENCRYPTED), Format.ODT));
        assertEquals("encrypted", error.code());
    }

    @Test
    void recoverableMalformedDocumentsStillConvert() throws IOException {
        String markdown = Anydoc.toMarkdown(Fixtures.path("malformed", "unclosed--recovers.docx"));
        assertTrue(markdown.contains("Unclosed paragraph text"), markdown);
    }

    @Test
    void truncatedPackagesAreMalformed() throws IOException {
        MalformedException error =
                assertThrows(
                        MalformedException.class,
                        () -> Anydoc.toMarkdown(Fixtures.path("malformed", "truncated--errors.docx")));
        assertEquals("malformed", error.code());
        assertTrue(error.getMessage().contains("malformed"), error.getMessage());
    }

    @Test
    void imageBombHitsAResourceLimit() throws IOException {
        ResourceLimitException error =
                assertThrows(
                        ResourceLimitException.class,
                        () ->
                                Anydoc.toMarkdownBytes(
                                        Fixtures.bytes("abuse", "imagebomb--errors.docx"), Format.DOCX));
        assertEquals("resourceLimit", error.code());
        assertFalse(error.limit().trim().isEmpty());
    }

    @Test
    void nullConversionArgumentsThrowNpe() {
        assertThrows(NullPointerException.class, () -> Anydoc.toMarkdown((String) null));
        assertThrows(NullPointerException.class, () -> Anydoc.toMarkdown((Path) null));
        assertThrows(NullPointerException.class, () -> Anydoc.toMarkdownBytes(null));
        assertThrows(NullPointerException.class, () -> Anydoc.toDocument((byte[]) null));
        assertThrows(NullPointerException.class, () -> Anydoc.toDocument((String) null));
        assertThrows(NullPointerException.class, () -> Anydoc.toDocument((Path) null));
    }

    private static byte[] emptyDocxPackage() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return buffer.toByteArray();
    }
}
