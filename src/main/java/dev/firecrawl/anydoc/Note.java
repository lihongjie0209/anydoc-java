package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** Footnote or endnote body, referenced from text by an {@link Inline.NoteRef}. */
public record Note(String id, NoteKind kind, List<Block> blocks) {

    public Note {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(blocks, "blocks");
        blocks = List.copyOf(blocks);
    }
}
