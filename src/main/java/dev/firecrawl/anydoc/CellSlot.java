package dev.firecrawl.anydoc;

import java.util.Objects;

/** One position in a {@link Table#grid()}: either a cell or the shadow of one. */
public interface CellSlot {

    /** {@code origin} or {@code covered}. */
    String kind();

    final class Origin implements CellSlot {
        private final Cell cell;

        public Origin(Cell cell) {
            this.cell = Objects.requireNonNull(cell, "cell");
        }

        public Cell cell() {
            return cell;
        }

        @Override
        public String kind() {
            return "origin";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Origin)) {
                return false;
            }
            return cell.equals(((Origin) other).cell);
        }

        @Override
        public int hashCode() {
            return cell.hashCode();
        }

        @Override
        public String toString() {
            return "Origin[cell=" + cell + "]";
        }
    }

    final class Covered implements CellSlot {
        private final int originRow;
        private final int originCol;

        public Covered(int originRow, int originCol) {
            this.originRow = originRow;
            this.originCol = originCol;
        }

        public int originRow() {
            return originRow;
        }

        public int originCol() {
            return originCol;
        }

        @Override
        public String kind() {
            return "covered";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Covered)) {
                return false;
            }
            Covered covered = (Covered) other;
            return originRow == covered.originRow && originCol == covered.originCol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(originRow, originCol);
        }

        @Override
        public String toString() {
            return "Covered[originRow=" + originRow + ", originCol=" + originCol + "]";
        }
    }
}
