package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** One span of inline content. */
public sealed interface Inline
        permits Inline.Text,
                Inline.Link,
                Inline.Image,
                Inline.Anchor,
                Inline.NoteRef,
                Inline.LineBreak {

    /**
     * The kind name other language bindings publish: {@code text}, {@code link}, {@code image},
     * {@code anchor}, {@code note_ref}, or {@code line_break}.
     */
    String kind();

    record Text(String text, Style style) implements Inline {
        public Text {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(style, "style");
        }

        @Override
        public String kind() {
            return "text";
        }
    }

    record Link(List<Inline> content, LinkTarget target) implements Inline {
        public Link {
            content = List.copyOf(Objects.requireNonNull(content, "content"));
            Objects.requireNonNull(target, "target");
        }

        @Override
        public String kind() {
            return "link";
        }
    }

    record Image(String alt, ImageSource source) implements Inline {
        public Image {
            Objects.requireNonNull(alt, "alt");
            Objects.requireNonNull(source, "source");
        }

        @Override
        public String kind() {
            return "image";
        }
    }

    /** Zero-width marker for an internal link target at this position. */
    record Anchor(String id) implements Inline {
        public Anchor {
            Objects.requireNonNull(id, "id");
        }

        @Override
        public String kind() {
            return "anchor";
        }
    }

    /** A reference to the {@link Note} with this id in {@link Document#notes()}. */
    record NoteRef(String noteId) implements Inline {
        public NoteRef {
            Objects.requireNonNull(noteId, "noteId");
        }

        @Override
        public String kind() {
            return "note_ref";
        }
    }

    record LineBreak() implements Inline {
        @Override
        public String kind() {
            return "line_break";
        }
    }
}
