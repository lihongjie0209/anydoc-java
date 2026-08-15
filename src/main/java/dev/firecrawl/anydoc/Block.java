package dev.firecrawl.anydoc;

import java.util.List;
import java.util.Objects;

/** One block-level piece of a document body. */
public sealed interface Block
        permits Block.Heading,
                Block.Paragraph,
                Block.ListBlock,
                Block.TableBlock,
                Block.BlockQuote,
                Block.CodeBlock,
                Block.Rule {

    /**
     * The kind name other language bindings publish: {@code heading}, {@code paragraph}, {@code
     * list}, {@code table}, {@code block_quote}, {@code code_block}, or {@code rule}.
     */
    String kind();

    record Heading(int level, String anchor, List<Inline> content) implements Block {
        public Heading {
            content = List.copyOf(Objects.requireNonNull(content, "content"));
        }

        @Override
        public String kind() {
            return "heading";
        }
    }

    record Paragraph(List<Inline> content) implements Block {
        public Paragraph {
            content = List.copyOf(Objects.requireNonNull(content, "content"));
        }

        @Override
        public String kind() {
            return "paragraph";
        }
    }

    record ListBlock(DocList list) implements Block {
        public ListBlock {
            Objects.requireNonNull(list, "list");
        }

        @Override
        public String kind() {
            return "list";
        }
    }

    record TableBlock(Table table) implements Block {
        public TableBlock {
            Objects.requireNonNull(table, "table");
        }

        @Override
        public String kind() {
            return "table";
        }
    }

    record BlockQuote(List<Block> blocks) implements Block {
        public BlockQuote {
            blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
        }

        @Override
        public String kind() {
            return "block_quote";
        }
    }

    record CodeBlock(String lang, String text) implements Block {
        public CodeBlock {
            Objects.requireNonNull(text, "text");
        }

        @Override
        public String kind() {
            return "code_block";
        }
    }

    record Rule() implements Block {
        @Override
        public String kind() {
            return "rule";
        }
    }
}
