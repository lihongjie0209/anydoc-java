package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** A table cell and the extent it spans. */
public record Cell(List<Block> blocks, int colSpan, int rowSpan) {

    public Cell {
        Objects.requireNonNull(blocks, "blocks");
        blocks = List.copyOf(blocks);
    }

    /**
     * True when the cell holds nothing that would render: only paragraphs count toward emptiness,
     * so a cell with a table or list in it is not empty even if that content is blank.
     */
    public boolean isEmpty() {
        return blocks.stream()
                .allMatch(
                        block ->
                                block instanceof Block.Paragraph paragraph
                                        && Inlines.areEmpty(paragraph.content()));
    }
}
