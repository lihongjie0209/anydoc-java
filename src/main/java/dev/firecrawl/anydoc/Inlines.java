package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/**
 * Crate helpers over a run of inlines: flatten to plain text, and decide whether the run would
 * render as visible content.
 */
public final class Inlines {

    private Inlines() {}

    /**
     * Flatten inlines to their text, dropping styling and links but keeping link text and image alt
     * text. Line breaks become newlines; anchors and note references contribute nothing.
     */
    public static String toPlainText(List<Inline> inlines) {
        Objects.requireNonNull(inlines, "inlines");
        StringBuilder out = new StringBuilder();
        collectPlainText(inlines, out);
        return out.toString();
    }

    /**
     * True when nothing here would render as visible content: only whitespace, empty-target links,
     * anchors, and line breaks. An image or a note reference always counts as content.
     */
    public static boolean areEmpty(List<Inline> inlines) {
        Objects.requireNonNull(inlines, "inlines");
        return inlines.stream().allMatch(Inlines::isEmpty);
    }

    private static boolean isEmpty(Inline inline) {
        return switch (inline) {
            case Inline.Text text -> text.text().trim().isEmpty();
            case Inline.Link link -> link.target().isEmpty() && areEmpty(link.content());
            case Inline.Image ignored -> false;
            case Inline.NoteRef ignored -> false;
            case Inline.Anchor ignored -> true;
            case Inline.LineBreak ignored -> true;
        };
    }

    private static void collectPlainText(List<Inline> inlines, StringBuilder out) {
        for (Inline inline : inlines) {
            switch (inline) {
                case Inline.Text text -> out.append(text.text());
                case Inline.Link link -> collectPlainText(link.content(), out);
                case Inline.Image image -> out.append(image.alt());
                case Inline.Anchor ignored -> {}
                case Inline.NoteRef ignored -> {}
                case Inline.LineBreak ignored -> out.append('\n');
            }
        }
    }
}
