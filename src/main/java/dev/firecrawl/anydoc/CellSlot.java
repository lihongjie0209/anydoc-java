package dev.firecrawl.anydoc;

import java.util.Objects;

/** One position in a {@link Table#grid()}: either a cell or the shadow of one. */
public sealed interface CellSlot permits CellSlot.Origin, CellSlot.Covered {

    /** {@code origin} or {@code covered}. */
    String kind();

    record Origin(Cell cell) implements CellSlot {
        public Origin {
            Objects.requireNonNull(cell, "cell");
        }

        @Override
        public String kind() {
            return "origin";
        }
    }

    record Covered(int originRow, int originCol) implements CellSlot {
        @Override
        public String kind() {
            return "covered";
        }
    }
}
