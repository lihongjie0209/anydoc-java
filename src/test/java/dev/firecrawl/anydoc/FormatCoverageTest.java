package dev.firecrawl.anydoc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FormatCoverageTest {

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "DOC,doc/text.doc,# Fixture Document",
        "DOCX,docx/text.docx,# Fixture Document",
        "ODT,odt/text.odt,# Fixture Document",
        "PDF,pdf/text.pdf,# Fixture Document",
        "PPT,ppt/pres.ppt,",
        "PPTX,pptx/pres.pptx,",
        "RTF,rtf/text.rtf,# Fixture Document",
        "EPUB,epub/book.epub,# Fixture Book",
        "XLSX,xlsx/sheet.xlsx,| Kind | Value | Note |",
        "ODS,ods/sheet.ods,",
        "ODP,odp/pres.odp,",
        "CSV,csv/sheet.csv,| --- |"
    })
    void everySupportedFormatConvertsToMarkdown(Format format, String fixture, String expected)
            throws IOException {
        Path path = Fixtures.path(fixture.split("/", 2)[0], fixture.split("/", 2)[1]);
        byte[] data = Files.readAllBytes(path);

        if (format != Format.CSV) {
            assertTrue(Anydoc.formatFromBytes(data).isPresent(), "should detect " + format);
        }

        String fromPath = Anydoc.toMarkdown(path);
        String fromBytes = Anydoc.toMarkdownBytes(data, format);
        assertFalse(fromPath.isBlank(), fromPath);
        assertFalse(fromBytes.isBlank(), fromBytes);
        if (expected != null && !expected.isBlank()) {
            assertTrue(fromPath.contains(expected), fromPath);
            assertTrue(fromBytes.contains(expected), fromBytes);
        }

        if (format != Format.PDF) {
            Document document = Anydoc.toDocument(data, format);
            assertFalse(document.blocks().isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "csv/handmade-quoted.csv",
        "csv/handmade-semicolon.csv",
        "csv/handmade-utf16.csv"
    })
    void csvVariantsNeedAnExplicitFormat(String fixture) throws IOException {
        String[] parts = fixture.split("/", 2);
        byte[] data = Fixtures.bytes(parts[0], parts[1]);
        assertTrue(Anydoc.formatFromBytes(data).isEmpty());
        String markdown = Anydoc.toMarkdownBytes(data, Format.CSV);
        assertTrue(markdown.contains("|"), markdown);
    }
}
