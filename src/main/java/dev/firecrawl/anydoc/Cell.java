package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** A table cell and the extent it spans. */
public final class Cell {

    private final List<Block> blocks;
    private final int colSpan;
    private final int rowSpan;

    public Cell(List<Block> blocks, int colSpan, int rowSpan) {
        this.blocks = Lists.copyOf(Objects.requireNonNull(blocks, "blocks"));
        this.colSpan = colSpan;
        this.rowSpan = rowSpan;
    }

    public List<Block> blocks() {
        return blocks;
    }

    public int colSpan() {
        return colSpan;
    }

    public int rowSpan() {
        return rowSpan;
    }

    /**
     * True when the cell holds nothing that would render: only paragraphs count toward emptiness,
     * so a cell with a table or list in it is not empty even if that content is blank.
     */
    public boolean isEmpty() {
        for (Block block : blocks) {
            if (!(block instanceof Block.Paragraph)) {
                return false;
            }
            if (!Inlines.areEmpty(((Block.Paragraph) block).content())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Cell)) {
            return false;
        }
        Cell cell = (Cell) other;
        return colSpan == cell.colSpan && rowSpan == cell.rowSpan && blocks.equals(cell.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks, colSpan, rowSpan);
    }

    @Override
    public String toString() {
        return "Cell[blocks=" + blocks + ", colSpan=" + colSpan + ", rowSpan=" + rowSpan + "]";
    }
}
