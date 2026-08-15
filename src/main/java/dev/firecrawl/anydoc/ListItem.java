package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** One item of a {@link DocList}, which may hold nested blocks including further lists. */
public record ListItem(List<Block> blocks, Boolean checked, String markerLabel) {

    public ListItem {
        Objects.requireNonNull(blocks, "blocks");
        blocks = List.copyOf(blocks);
    }
}
