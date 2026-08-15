package dev.firecrawl.anydoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ModelHelpersTest {

    @Test
    void toPlainTextKeepsLinkTextAndImageAltAndDropsMarkers() {
        assertEquals("hi", Inlines.toPlainText(List.of(new Inline.Text("hi", Style.PLAIN))));
        assertEquals(
                "click here",
                Inlines.toPlainText(
                        List.of(
                                new Inline.Link(
                                        List.of(new Inline.Text("click here", Style.PLAIN)),
                                        new LinkTarget.External("https://example.com")))));
        assertEquals("logo", Inlines.toPlainText(List.of(new Inline.Image("logo", new ImageSource.Unavailable()))));
        assertEquals("\n", Inlines.toPlainText(List.of(new Inline.LineBreak())));
        assertEquals("", Inlines.toPlainText(List.of(new Inline.Anchor("mark"))));
        assertEquals("", Inlines.toPlainText(List.of(new Inline.NoteRef("n1"))));
        assertEquals(
                "ab\nc",
                Inlines.toPlainText(
                        List.of(
                                new Inline.Text("a", Style.PLAIN),
                                new Inline.Link(
                                        List.of(new Inline.Text("b", Style.PLAIN)),
                                        new LinkTarget.Relative("next.md")),
                                new Inline.LineBreak(),
                                new Inline.Anchor("x"),
                                new Inline.NoteRef("n"),
                                new Inline.Text("c", Style.PLAIN))));
    }

    @Test
    void areEmptyTreatsWhitespaceAnchorsAndEmptyLinksAsEmpty() {
        assertTrue(
                Inlines.areEmpty(
                        List.of(
                                new Inline.Text("  \t", Style.PLAIN),
                                new Inline.Link(List.of(), new LinkTarget.External("")),
                                new Inline.Anchor("id"),
                                new Inline.LineBreak())));
        assertFalse(Inlines.areEmpty(List.of(new Inline.Image("", new ImageSource.Unavailable()))));
        assertFalse(Inlines.areEmpty(List.of(new Inline.NoteRef("n1"))));
        assertFalse(Inlines.areEmpty(List.of(new Inline.Text("x", Style.PLAIN))));
    }

    @Test
    void markerKindMatchesCrateLabelAndOrdinalCases() {
        assertEquals("7.", MarkerKind.DECIMAL.label(7));
        assertEquals("7", MarkerKind.DECIMAL.ordinal(7));
        assertEquals("c.", MarkerKind.LOWER_ALPHA.label(3));
        assertEquals("c", MarkerKind.LOWER_ALPHA.ordinal(3));
        assertEquals("aa.", MarkerKind.LOWER_ALPHA.label(27));
        assertEquals("iv.", MarkerKind.LOWER_ROMAN.label(4));
        assertEquals("iv", MarkerKind.LOWER_ROMAN.ordinal(4));
        assertFalse(MarkerKind.BULLET.ordered());
        assertEquals("-", MarkerKind.BULLET.label(1));
        assertEquals("-", MarkerKind.BULLET.ordinal(1));
        assertTrue(MarkerKind.DECIMAL.ordered());
        assertTrue(MarkerKind.LOWER_ALPHA.ordered());
        assertEquals("B.", MarkerKind.UPPER_ALPHA.label(2));
        assertEquals("IX.", MarkerKind.UPPER_ROMAN.label(9));
    }

    @Test
    void listOrderedFollowsTheMarker() {
        assertTrue(new DocList(MarkerKind.DECIMAL, 1, List.of()).ordered());
        assertFalse(new DocList(MarkerKind.BULLET, 1, List.of()).ordered());
    }

    @Test
    void cellEmptinessOnlyLooksAtParagraphs() {
        Cell blank = new Cell(List.of(new Block.Paragraph(List.of(new Inline.Text("  ", Style.PLAIN)))), 1, 1);
        assertTrue(blank.isEmpty());
        Cell withList =
                new Cell(
                        List.of(
                                new Block.ListBlock(
                                        new DocList(MarkerKind.BULLET, 1, List.of()))),
                        1,
                        1);
        assertFalse(withList.isEmpty());
    }

    @Test
    void tableIsSingleCellOnlyForALoneOrigin() {
        Cell cell = new Cell(List.of(), 1, 1);
        Table single = new Table(List.of(List.of(new CellSlot.Origin(cell))), 0, TableKind.DATA);
        assertTrue(single.isSingleCell());
        Table coveredPad =
                new Table(
                        List.of(List.of(new CellSlot.Origin(cell), new CellSlot.Covered(0, 0))),
                        0,
                        TableKind.DATA);
        assertFalse(coveredPad.isSingleCell());
    }

    @Test
    void linkTargetIsEmptyWhenTheValueIsEmpty() {
        assertTrue(new LinkTarget.External("").isEmpty());
        assertTrue(new LinkTarget.Relative("").isEmpty());
        assertTrue(new LinkTarget.Anchor("").isEmpty());
        assertFalse(new LinkTarget.External("https://example.com").isEmpty());
    }
}
