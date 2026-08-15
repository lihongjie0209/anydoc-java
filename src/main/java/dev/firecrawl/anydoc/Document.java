package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** A parsed document: its body, its notes, and the bytes of everything it embedded. */
public final class Document {

    private final List<Block> blocks;
    private final List<Note> notes;
    private final List<Asset> assets;

    public Document(List<Block> blocks, List<Note> notes, List<Asset> assets) {
        this.blocks = Lists.copyOf(Objects.requireNonNull(blocks, "blocks"));
        this.notes = Lists.copyOf(Objects.requireNonNull(notes, "notes"));
        this.assets = Lists.copyOf(Objects.requireNonNull(assets, "assets"));
    }

    public List<Block> blocks() {
        return blocks;
    }

    public List<Note> notes() {
        return notes;
    }

    public List<Asset> assets() {
        return assets;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Document)) {
            return false;
        }
        Document document = (Document) other;
        return blocks.equals(document.blocks)
                && notes.equals(document.notes)
                && assets.equals(document.assets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks, notes, assets);
    }

    @Override
    public String toString() {
        return "Document[blocks=" + blocks + ", notes=" + notes + ", assets=" + assets + "]";
    }
}
