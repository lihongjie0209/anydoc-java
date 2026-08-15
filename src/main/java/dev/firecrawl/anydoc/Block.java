package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** One block-level piece of a document body. */
public interface Block {

    /**
     * The kind name other language bindings publish: {@code heading}, {@code paragraph}, {@code
     * list}, {@code table}, {@code block_quote}, {@code code_block}, or {@code rule}.
     */
    String kind();

    final class Heading implements Block {
        private final int level;
        private final String anchor;
        private final List<Inline> content;

        public Heading(int level, String anchor, List<Inline> content) {
            this.level = level;
            this.anchor = anchor;
            this.content = Lists.copyOf(Objects.requireNonNull(content, "content"));
        }

        public int level() {
            return level;
        }

        public String anchor() {
            return anchor;
        }

        public List<Inline> content() {
            return content;
        }

        @Override
        public String kind() {
            return "heading";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Heading)) {
                return false;
            }
            Heading heading = (Heading) other;
            return level == heading.level
                    && Objects.equals(anchor, heading.anchor)
                    && content.equals(heading.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, anchor, content);
        }

        @Override
        public String toString() {
            return "Heading[level=" + level + ", anchor=" + anchor + ", content=" + content + "]";
        }
    }

    final class Paragraph implements Block {
        private final List<Inline> content;

        public Paragraph(List<Inline> content) {
            this.content = Lists.copyOf(Objects.requireNonNull(content, "content"));
        }

        public List<Inline> content() {
            return content;
        }

        @Override
        public String kind() {
            return "paragraph";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Paragraph)) {
                return false;
            }
            return content.equals(((Paragraph) other).content);
        }

        @Override
        public int hashCode() {
            return content.hashCode();
        }

        @Override
        public String toString() {
            return "Paragraph[content=" + content + "]";
        }
    }

    final class ListBlock implements Block {
        private final DocList list;

        public ListBlock(DocList list) {
            this.list = Objects.requireNonNull(list, "list");
        }

        public DocList list() {
            return list;
        }

        @Override
        public String kind() {
            return "list";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ListBlock)) {
                return false;
            }
            return list.equals(((ListBlock) other).list);
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }

        @Override
        public String toString() {
            return "ListBlock[list=" + list + "]";
        }
    }

    final class TableBlock implements Block {
        private final Table table;

        public TableBlock(Table table) {
            this.table = Objects.requireNonNull(table, "table");
        }

        public Table table() {
            return table;
        }

        @Override
        public String kind() {
            return "table";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TableBlock)) {
                return false;
            }
            return table.equals(((TableBlock) other).table);
        }

        @Override
        public int hashCode() {
            return table.hashCode();
        }

        @Override
        public String toString() {
            return "TableBlock[table=" + table + "]";
        }
    }

    final class BlockQuote implements Block {
        private final List<Block> blocks;

        public BlockQuote(List<Block> blocks) {
            this.blocks = Lists.copyOf(Objects.requireNonNull(blocks, "blocks"));
        }

        public List<Block> blocks() {
            return blocks;
        }

        @Override
        public String kind() {
            return "block_quote";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BlockQuote)) {
                return false;
            }
            return blocks.equals(((BlockQuote) other).blocks);
        }

        @Override
        public int hashCode() {
            return blocks.hashCode();
        }

        @Override
        public String toString() {
            return "BlockQuote[blocks=" + blocks + "]";
        }
    }

    final class CodeBlock implements Block {
        private final String lang;
        private final String text;

        public CodeBlock(String lang, String text) {
            this.lang = lang;
            this.text = Objects.requireNonNull(text, "text");
        }

        public String lang() {
            return lang;
        }

        public String text() {
            return text;
        }

        @Override
        public String kind() {
            return "code_block";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CodeBlock)) {
                return false;
            }
            CodeBlock block = (CodeBlock) other;
            return Objects.equals(lang, block.lang) && text.equals(block.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lang, text);
        }

        @Override
        public String toString() {
            return "CodeBlock[lang=" + lang + ", text=" + text + "]";
        }
    }

    final class Rule implements Block {
        public Rule() {}

        @Override
        public String kind() {
            return "rule";
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Rule;
        }

        @Override
        public int hashCode() {
            return Rule.class.hashCode();
        }

        @Override
        public String toString() {
            return "Rule[]";
        }
    }
}
