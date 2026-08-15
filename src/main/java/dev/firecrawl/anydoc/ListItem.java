package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** One item of a {@link DocList}, which may hold nested blocks including further lists. */
public final class ListItem {

    private final List<Block> blocks;
    private final Boolean checked;
    private final String markerLabel;

    public ListItem(List<Block> blocks, Boolean checked, String markerLabel) {
        this.blocks = Lists.copyOf(Objects.requireNonNull(blocks, "blocks"));
        this.checked = checked;
        this.markerLabel = markerLabel;
    }

    public List<Block> blocks() {
        return blocks;
    }

    public Boolean checked() {
        return checked;
    }

    public String markerLabel() {
        return markerLabel;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ListItem)) {
            return false;
        }
        ListItem item = (ListItem) other;
        return blocks.equals(item.blocks)
                && Objects.equals(checked, item.checked)
                && Objects.equals(markerLabel, item.markerLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks, checked, markerLabel);
    }

    @Override
    public String toString() {
        return "ListItem[blocks=" + blocks + ", checked=" + checked + ", markerLabel=" + markerLabel + "]";
    }
}
