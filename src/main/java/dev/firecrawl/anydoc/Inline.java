package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** One span of inline content. */
public interface Inline {

    /**
     * The kind name other language bindings publish: {@code text}, {@code link}, {@code image},
     * {@code anchor}, {@code note_ref}, or {@code line_break}.
     */
    String kind();

    final class Text implements Inline {
        private final String text;
        private final Style style;

        public Text(String text, Style style) {
            this.text = Objects.requireNonNull(text, "text");
            this.style = Objects.requireNonNull(style, "style");
        }

        public String text() {
            return text;
        }

        public Style style() {
            return style;
        }

        @Override
        public String kind() {
            return "text";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Text)) {
                return false;
            }
            Text that = (Text) other;
            return text.equals(that.text) && style.equals(that.style);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, style);
        }

        @Override
        public String toString() {
            return "Text[text=" + text + ", style=" + style + "]";
        }
    }

    final class Link implements Inline {
        private final List<Inline> content;
        private final LinkTarget target;

        public Link(List<Inline> content, LinkTarget target) {
            this.content = Lists.copyOf(Objects.requireNonNull(content, "content"));
            this.target = Objects.requireNonNull(target, "target");
        }

        public List<Inline> content() {
            return content;
        }

        public LinkTarget target() {
            return target;
        }

        @Override
        public String kind() {
            return "link";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Link)) {
                return false;
            }
            Link link = (Link) other;
            return content.equals(link.content) && target.equals(link.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(content, target);
        }

        @Override
        public String toString() {
            return "Link[content=" + content + ", target=" + target + "]";
        }
    }

    final class Image implements Inline {
        private final String alt;
        private final ImageSource source;

        public Image(String alt, ImageSource source) {
            this.alt = Objects.requireNonNull(alt, "alt");
            this.source = Objects.requireNonNull(source, "source");
        }

        public String alt() {
            return alt;
        }

        public ImageSource source() {
            return source;
        }

        @Override
        public String kind() {
            return "image";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Image)) {
                return false;
            }
            Image image = (Image) other;
            return alt.equals(image.alt) && source.equals(image.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(alt, source);
        }

        @Override
        public String toString() {
            return "Image[alt=" + alt + ", source=" + source + "]";
        }
    }

    /** Zero-width marker for an internal link target at this position. */
    final class Anchor implements Inline {
        private final String id;

        public Anchor(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public String id() {
            return id;
        }

        @Override
        public String kind() {
            return "anchor";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Anchor)) {
                return false;
            }
            return id.equals(((Anchor) other).id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "Anchor[id=" + id + "]";
        }
    }

    /** A reference to the {@link Note} with this id in {@link Document#notes()}. */
    final class NoteRef implements Inline {
        private final String noteId;

        public NoteRef(String noteId) {
            this.noteId = Objects.requireNonNull(noteId, "noteId");
        }

        public String noteId() {
            return noteId;
        }

        @Override
        public String kind() {
            return "note_ref";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof NoteRef)) {
                return false;
            }
            return noteId.equals(((NoteRef) other).noteId);
        }

        @Override
        public int hashCode() {
            return noteId.hashCode();
        }

        @Override
        public String toString() {
            return "NoteRef[noteId=" + noteId + "]";
        }
    }

    final class LineBreak implements Inline {
        public LineBreak() {}

        @Override
        public String kind() {
            return "line_break";
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof LineBreak;
        }

        @Override
        public int hashCode() {
            return LineBreak.class.hashCode();
        }

        @Override
        public String toString() {
            return "LineBreak[]";
        }
    }
}
