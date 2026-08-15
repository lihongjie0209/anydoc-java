package dev.firecrawl.anydoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DocumentModelTest {

    @Test
    void headingsCarryLevelContentAndKind() throws IOException {
        Document document = Anydoc.toDocument(Fixtures.bytes("docx", "handmade-outline.docx"));
        Block.Heading heading =
                Fixtures.walk(document.blocks())
                        .filter(Block.Heading.class::isInstance)
                        .map(Block.Heading.class::cast)
                        .findFirst()
                        .orElseThrow();
        assertEquals("heading", heading.kind());
        assertTrue(heading.level() >= 1 && heading.level() <= 6);
        Inline.Text text = assertInstanceOf(Inline.Text.class, heading.content().getFirst());
        assertEquals("text", text.kind());
        assertFalse(text.text().isBlank());
        assertInstanceOf(Style.class, text.style());
    }

    @Test
    void tablesExposeOriginAndCoveredSlots() throws IOException {
        Document document = Anydoc.toDocument(Fixtures.bytes("xlsx", "handmade-merged.xlsx"));
        Block.TableBlock tableBlock =
                Fixtures.walk(document.blocks())
                        .filter(Block.TableBlock.class::isInstance)
                        .map(Block.TableBlock.class::cast)
                        .findFirst()
                        .orElseThrow();
        assertEquals("table", tableBlock.kind());
        Table table = tableBlock.table();
        assertEquals("data", table.kind().wireName());
        assertFalse(table.grid().isEmpty());
        assertTrue(
                table.grid().stream().flatMap(List::stream).anyMatch(CellSlot.Origin.class::isInstance));
        assertTrue(
                table.grid().stream().flatMap(List::stream).anyMatch(CellSlot.Covered.class::isInstance),
                "merged xlsx should keep covered slots");
        CellSlot.Covered covered =
                table.grid().stream()
                        .flatMap(List::stream)
                        .filter(CellSlot.Covered.class::isInstance)
                        .map(CellSlot.Covered.class::cast)
                        .findFirst()
                        .orElseThrow();
        assertEquals("covered", covered.kind());
        assertTrue(covered.originRow() >= 0);
        assertTrue(covered.originCol() >= 0);
    }

    @Test
    void listsPreserveMarkerAndNesting() throws IOException {
        Document document = Anydoc.toDocument(Fixtures.bytes("docx", "handmade-numbering.docx"));
        Block.ListBlock listBlock =
                Fixtures.walk(document.blocks())
                        .filter(Block.ListBlock.class::isInstance)
                        .map(Block.ListBlock.class::cast)
                        .findFirst()
                        .orElseThrow();
        assertEquals("list", listBlock.kind());
        DocList list = listBlock.list();
        assertEquals(MarkerKind.DECIMAL, list.marker());
        assertEquals("decimal", list.marker().wireName());
        assertTrue(list.start() >= 1);
        assertFalse(list.items().isEmpty());
        assertTrue(
                list.items().stream()
                        .flatMap(item -> item.blocks().stream())
                        .anyMatch(Block.ListBlock.class::isInstance),
                "numbering fixture nests a list inside an item");
    }

    @Test
    void linksDistinguishExternalAndAnchorTargets() throws IOException {
        Document document = Anydoc.toDocument(Fixtures.bytes("pptx", "handmade-links.pptx"));
        List<Inline.Link> links =
                Fixtures.allInlines(document)
                        .filter(Inline.Link.class::isInstance)
                        .map(Inline.Link.class::cast)
                        .toList();
        assertFalse(links.isEmpty());
        assertTrue(
                links.stream().anyMatch(link -> link.target() instanceof LinkTarget.External),
                links.toString());
        assertTrue(
                links.stream().anyMatch(link -> link.target() instanceof LinkTarget.Anchor),
                links.toString());
        Inline.Link external =
                links.stream()
                        .filter(link -> link.target() instanceof LinkTarget.External)
                        .findFirst()
                        .orElseThrow();
        assertEquals("link", external.kind());
        assertEquals("external", external.target().kind());
        assertTrue(external.target().value().contains("example.com"), external.target().value());
    }

    @Test
    void epubBookHasHeadingsListsAndInternalLinks() throws IOException {
        Document document = Anydoc.toDocument(Fixtures.bytes("epub", "book.epub"));
        assertTrue(Fixtures.walk(document.blocks()).anyMatch(Block.Heading.class::isInstance));
        assertTrue(Fixtures.walk(document.blocks()).anyMatch(Block.ListBlock.class::isInstance));
        assertTrue(
                Fixtures.allInlines(document)
                        .anyMatch(
                                inline ->
                                        inline instanceof Inline.Link link
                                                && link.target() instanceof LinkTarget.Anchor));
    }

    @Test
    void assetsKeepBytesAndAreDefensivelyCopied() throws IOException {
        Document document = Anydoc.toDocument(Fixtures.bytes("docx", "handmade-rich.docx"));
        Asset image =
                document.assets().stream()
                        .filter(asset -> asset.mediaType().equals("image/png"))
                        .findFirst()
                        .orElseThrow();
        assertTrue(image.data().length > 0);
        assertFalse(image.originPart().isBlank());
        assertEquals(image.id(), document.assets().indexOf(image));

        byte[] first = image.data();
        byte[] second = image.data();
        assertNotSame(first, second);
        assertTrue(Arrays.equals(first, second));
        first[0] ^= (byte) 0xff;
        assertFalse(Arrays.equals(first, image.data()));
    }

    @Test
    void pdfHasNoDocumentModel() throws IOException {
        byte[] pdf = Fixtures.bytes("pdf", "text.pdf");
        String markdown = Anydoc.toMarkdownBytes(pdf, Format.PDF);
        assertTrue(markdown.contains("# Fixture Document"), markdown);

        UnsupportedException error =
                assertThrows(UnsupportedException.class, () -> Anydoc.toDocument(pdf, Format.PDF));
        assertEquals("unsupported", error.code());
        assertInstanceOf(ConvertException.class, error);
    }

    @Test
    void toDocumentDetectsFormatWhenNoneIsNamed() throws IOException {
        Document named = Anydoc.toDocument(Fixtures.bytes("docx", "handmade-outline.docx"), Format.DOCX);
        Document detected = Anydoc.toDocument(Fixtures.bytes("docx", "handmade-outline.docx"));
        assertEquals(named.blocks().size(), detected.blocks().size());
    }
}
