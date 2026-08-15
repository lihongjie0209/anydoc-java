package dev.firecrawl.anydoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Canonical table grid: every logical grid position appears exactly once. Content and spans live on
 * the origin slot, and each position a span covers holds a {@link CellSlot.Covered} pointing back
 * at that origin.
 */
public final class Table {

    private final List<List<CellSlot>> grid;
    private final int headerRows;
    private final TableKind kind;

    public Table(List<List<CellSlot>> grid, int headerRows, TableKind kind) {
        Objects.requireNonNull(grid, "grid");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.headerRows = headerRows;
        List<List<CellSlot>> copied = new ArrayList<List<CellSlot>>(grid.size());
        for (List<CellSlot> row : grid) {
            copied.add(Lists.copyOf(row));
        }
        this.grid = Collections.unmodifiableList(copied);
    }

    public List<List<CellSlot>> grid() {
        return grid;
    }

    public int headerRows() {
        return headerRows;
    }

    public TableKind kind() {
        return kind;
    }

    /** True when the table is a single origin cell (any covered padding aside). */
    public boolean isSingleCell() {
        return grid.size() == 1
                && grid.get(0).size() == 1
                && grid.get(0).get(0) instanceof CellSlot.Origin;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Table)) {
            return false;
        }
        Table table = (Table) other;
        return headerRows == table.headerRows && kind == table.kind && grid.equals(table.grid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grid, headerRows, kind);
    }

    @Override
    public String toString() {
        return "Table[grid=" + grid + ", headerRows=" + headerRows + ", kind=" + kind + "]";
    }
}
