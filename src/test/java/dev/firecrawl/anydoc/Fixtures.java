package dev.firecrawl.anydoc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class Fixtures {

    static final Path ROOT = Path.of(System.getProperty("anydoc.fixtures", "../anydoc/tests/fixtures"));

    private Fixtures() {}

    static Path path(String... parts) {
        Path p = ROOT;
        for (String part : parts) {
            p = p.resolve(part);
        }
        return p;
    }

    static byte[] bytes(String... parts) throws IOException {
        return Files.readAllBytes(path(parts));
    }

    static Stream<Inline> inlines(List<Inline> items) {
        return items.stream().flatMap(Fixtures::inlines);
    }

    static Stream<Inline> inlines(Inline inline) {
        return switch (inline) {
            case Inline.Link link -> Stream.concat(Stream.of(link), inlines(link.content()));
            default -> Stream.of(inline);
        };
    }

    static Stream<Block> walk(List<Block> blocks) {
        return blocks.stream().flatMap(Fixtures::walk);
    }

    static Stream<Block> walk(Block block) {
        Stream<Block> nested =
                switch (block) {
                    case Block.ListBlock list ->
                            list.list().items().stream().flatMap(item -> walk(item.blocks()));
                    case Block.TableBlock table ->
                            table.table().grid().stream()
                                    .flatMap(List::stream)
                                    .flatMap(
                                            slot ->
                                                    slot instanceof CellSlot.Origin origin
                                                            ? walk(origin.cell().blocks())
                                                            : Stream.empty());
                    case Block.BlockQuote quote -> walk(quote.blocks());
                    default -> Stream.empty();
                };
        return Stream.concat(Stream.of(block), nested);
    }

    static Stream<Inline> allInlines(Document document) {
        Stream<Inline> body =
                walk(document.blocks())
                        .flatMap(
                                block ->
                                        switch (block) {
                                            case Block.Heading heading -> inlines(heading.content());
                                            case Block.Paragraph paragraph -> inlines(paragraph.content());
                                            default -> Stream.empty();
                                        });
        Stream<Inline> notes =
                document.notes().stream().flatMap(note -> walk(note.blocks())).flatMap(block -> switch (block) {
                    case Block.Heading heading -> inlines(heading.content());
                    case Block.Paragraph paragraph -> inlines(paragraph.content());
                    default -> Stream.empty();
                });
        return Stream.concat(body, notes);
    }
}
