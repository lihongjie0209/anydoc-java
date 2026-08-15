package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/**
 * A fully resolved list. Named {@code DocList} so it does not collide with {@link java.util.List}.
 */
public final class DocList {

    private final MarkerKind marker;
    private final long start;
    private final List<ListItem> items;

    public DocList(MarkerKind marker, long start, List<ListItem> items) {
        this.marker = Objects.requireNonNull(marker, "marker");
        this.start = start;
        this.items = Lists.copyOf(Objects.requireNonNull(items, "items"));
    }

    public MarkerKind marker() {
        return marker;
    }

    public long start() {
        return start;
    }

    public List<ListItem> items() {
        return items;
    }

    /** True when this list's marker is a numbered one. */
    public boolean ordered() {
        return marker.ordered();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DocList)) {
            return false;
        }
        DocList list = (DocList) other;
        return start == list.start && marker == list.marker && items.equals(list.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(marker, start, items);
    }

    @Override
    public String toString() {
        return "DocList[marker=" + marker + ", start=" + start + ", items=" + items + "]";
    }
}
