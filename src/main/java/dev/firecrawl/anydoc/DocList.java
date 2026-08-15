package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/**
 * A fully resolved list. Named {@code DocList} so it does not collide with {@link java.util.List}.
 */
public record DocList(MarkerKind marker, long start, List<ListItem> items) {

    public DocList {
        Objects.requireNonNull(marker, "marker");
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    /** True when this list's marker is a numbered one. */
    public boolean ordered() {
        return marker.ordered();
    }
}
