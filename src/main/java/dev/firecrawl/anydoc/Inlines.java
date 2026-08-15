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
        for (Inline inline : inlines) {
            if (!isEmpty(inline)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(Inline inline) {
        if (inline instanceof Inline.Text) {
            return ((Inline.Text) inline).text().trim().isEmpty();
        }
        if (inline instanceof Inline.Link) {
            Inline.Link link = (Inline.Link) inline;
            return link.target().isEmpty() && areEmpty(link.content());
        }
        if (inline instanceof Inline.Image || inline instanceof Inline.NoteRef) {
            return false;
        }
        if (inline instanceof Inline.Anchor || inline instanceof Inline.LineBreak) {
            return true;
        }
        throw new IllegalArgumentException("unknown inline: " + inline);
    }

    private static void collectPlainText(List<Inline> inlines, StringBuilder out) {
        for (Inline inline : inlines) {
            if (inline instanceof Inline.Text) {
                out.append(((Inline.Text) inline).text());
            } else if (inline instanceof Inline.Link) {
                collectPlainText(((Inline.Link) inline).content(), out);
            } else if (inline instanceof Inline.Image) {
                out.append(((Inline.Image) inline).alt());
            } else if (inline instanceof Inline.LineBreak) {
                out.append('\n');
            } else if (inline instanceof Inline.Anchor || inline instanceof Inline.NoteRef) {
                // markers contribute nothing
            } else {
                throw new IllegalArgumentException("unknown inline: " + inline);
            }
        }
    }
}
