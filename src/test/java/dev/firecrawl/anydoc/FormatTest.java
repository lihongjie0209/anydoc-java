package dev.firecrawl.anydoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FormatTest {

    @Test
    void everyVariantHasAUniqueWireName() {
        Set<String> names = Arrays.stream(Format.values()).map(Format::wireName).collect(Collectors.toSet());
        assertEquals(Format.values().length, names.size());
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void fromWireNameRoundTrips(Format format) {
        assertEquals(Optional.of(format), Format.fromWireName(format.wireName()));
        assertEquals(Optional.of(format), Format.fromWireName(format.wireName().toUpperCase()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"wat", "docm", "image/png"})
    void fromWireNameRejectsUnknownNames(String name) {
        assertEquals(Optional.empty(), Format.fromWireName(name));
    }

    @ParameterizedTest
    @CsvSource({
        "doc,DOC",
        "DOCX,DOCX",
        ".docm,DOCX",
        "odt,ODT",
        "pdf,PDF",
        "ppt,PPT",
        ".pps,PPT",
        "pot,PPT",
        "pptx,PPTX",
        ".pptm,PPTX",
        "ppsx,PPTX",
        "ppsm,PPTX",
        "rtf,RTF",
        "epub,EPUB",
        "xlsx,XLSX",
        "xls,XLSX",
        "xlsm,XLSX",
        "xlsb,XLSX",
        "ods,ODS",
        "odp,ODP",
        "csv,CSV",
        ".CSV,CSV"
    })
    void formatFromExtensionMapsContainerVariants(String extension, Format expected) {
        assertEquals(Optional.of(expected), Anydoc.formatFromExtension(extension));
        assertEquals(Optional.of(expected), Anydoc.formatFromPath("file." + extension.replace(".", "")));
        assertEquals(Optional.of(expected), Anydoc.formatFromPath(java.nio.file.Path.of("dir", "file." + extension.replace(".", ""))));
    }

    @Test
    void formatFromPathNeedsARecognizedExtension() {
        assertEquals(Optional.empty(), Anydoc.formatFromPath("report"));
        assertEquals(Optional.empty(), Anydoc.formatFromPath("report.unknown"));
        assertEquals(Optional.empty(), Anydoc.formatFromExtension("unknown"));
    }

    @Test
    void nullArgumentsThrowNpe() {
        assertTrue(throwsNpe(() -> Anydoc.formatFromBytes(null)));
        assertTrue(throwsNpe(() -> Anydoc.formatFromExtension(null)));
        assertTrue(throwsNpe(() -> Anydoc.formatFromPath((String) null)));
        assertTrue(throwsNpe(() -> Anydoc.formatFromPath((java.nio.file.Path) null)));
    }

    private static boolean throwsNpe(Runnable action) {
        try {
            action.run();
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }
}
