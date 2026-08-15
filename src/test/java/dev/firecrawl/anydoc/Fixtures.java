package dev.firecrawl.anydoc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

final class Fixtures {

    static final Path ROOT = Paths.get(System.getProperty("anydoc.fixtures", "../anydoc/tests/fixtures"));

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
        if (inline instanceof Inline.Link) {
            Inline.Link link = (Inline.Link) inline;
            return Stream.concat(Stream.of(link), inlines(link.content()));
        }
        return Stream.of(inline);
    }

    static Stream<Block> walk(List<Block> blocks) {
        return blocks.stream().flatMap(Fixtures::walk);
    }

    static Stream<Block> walk(Block block) {
        Stream<Block> nested;
        if (block instanceof Block.ListBlock) {
            nested =
                    ((Block.ListBlock) block)
                            .list()
                            .items()
                            .stream()
                            .flatMap(item -> walk(item.blocks()));
        } else if (block instanceof Block.TableBlock) {
            nested =
                    ((Block.TableBlock) block)
                            .table()
                            .grid()
                            .stream()
                            .flatMap(List::stream)
                            .flatMap(Fixtures::originBlocks);
        } else if (block instanceof Block.BlockQuote) {
            nested = walk(((Block.BlockQuote) block).blocks());
        } else {
            nested = Stream.empty();
        }
        return Stream.concat(Stream.of(block), nested);
    }

    private static Stream<Block> originBlocks(CellSlot slot) {
        if (slot instanceof CellSlot.Origin) {
            return walk(((CellSlot.Origin) slot).cell().blocks());
        }
        return Stream.empty();
    }

    static Stream<Inline> allInlines(Document document) {
        Stream<Inline> body = walk(document.blocks()).flatMap(Fixtures::blockInlines);
        Stream<Inline> notes =
                document.notes().stream().flatMap(note -> walk(note.blocks())).flatMap(Fixtures::blockInlines);
        return Stream.concat(body, notes);
    }

    private static Stream<Inline> blockInlines(Block block) {
        if (block instanceof Block.Heading) {
            return inlines(((Block.Heading) block).content());
        }
        if (block instanceof Block.Paragraph) {
            return inlines(((Block.Paragraph) block).content());
        }
        return Stream.empty();
    }
}
