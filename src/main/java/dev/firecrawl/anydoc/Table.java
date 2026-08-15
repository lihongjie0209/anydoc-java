package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/**
 * Canonical table grid: every logical grid position appears exactly once. Content and spans live on
 * the origin slot, and each position a span covers holds a {@link CellSlot.Covered} pointing back
 * at that origin.
 */
public record Table(List<List<CellSlot>> grid, int headerRows, TableKind kind) {

    public Table {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(kind, "kind");
        grid = grid.stream().map(List::copyOf).toList();
    }
}
