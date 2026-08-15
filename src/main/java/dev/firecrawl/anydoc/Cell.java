package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** A table cell and the extent it spans. */
public record Cell(List<Block> blocks, int colSpan, int rowSpan) {

    public Cell {
        Objects.requireNonNull(blocks, "blocks");
        blocks = List.copyOf(blocks);
    }
}
