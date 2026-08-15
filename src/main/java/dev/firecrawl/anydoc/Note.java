package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** Footnote or endnote body, referenced from text by an {@link Inline.NoteRef}. */
public final class Note {

    private final String id;
    private final NoteKind kind;
    private final List<Block> blocks;

    public Note(String id, NoteKind kind, List<Block> blocks) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.blocks = Lists.copyOf(Objects.requireNonNull(blocks, "blocks"));
    }

    public String id() {
        return id;
    }

    public NoteKind kind() {
        return kind;
    }

    public List<Block> blocks() {
        return blocks;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Note)) {
            return false;
        }
        Note note = (Note) other;
        return id.equals(note.id) && kind == note.kind && blocks.equals(note.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, kind, blocks);
    }

    @Override
    public String toString() {
        return "Note[id=" + id + ", kind=" + kind + ", blocks=" + blocks + "]";
    }
}
