package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** A parsed document: its body, its notes, and the bytes of everything it embedded. */
public record Document(List<Block> blocks, List<Note> notes, List<Asset> assets) {

    public Document {
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(assets, "assets");
        blocks = List.copyOf(blocks);
        notes = List.copyOf(notes);
        assets = List.copyOf(assets);
    }
}
