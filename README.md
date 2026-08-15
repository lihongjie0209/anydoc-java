# anydoc for Java

[![CI](https://github.com/lihongjie0209/anydoc-java/actions/workflows/ci.yml/badge.svg)](https://github.com/lihongjie0209/anydoc-java/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Convert Word, PowerPoint, Excel, OpenDocument, RTF, EPUB, CSV, and PDF files into clean GitHub-Flavored Markdown, or into a typed document model. Java 8 bindings for the [anydoc](https://github.com/firecrawl/anydoc) Rust crate.

Linux GNU builds are linked with [Zig](https://ziglang.org) against **glibc 2.17** (CentOS 7 / manylinux2014).

## Requirements

Runs on **Java 8 and newer** (11, 17, 21 included). The published JAR is compiled with `-source 1.8` / `-target 1.8`.

The public model is Java 8-shaped: `Block`, `Inline`, `CellSlot`, `LinkTarget`, and `ImageSource` are interfaces with nested implementation classes. Inspect them with `instanceof` and a cast — there are no records, sealed types, or pattern-matching `switch` in the API.

## Install (GitHub Packages)

Coordinates: `io.github.lihongjie0209:anydoc:0.1.9`

The default artifact is the **fat JAR** (every native library). Classifier JARs ship one platform each.

Maven:

```xml
<dependency>
  <groupId>io.github.lihongjie0209</groupId>
  <artifactId>anydoc</artifactId>
  <version>0.1.9</version>
  <!-- optional: linux-x86_64, linux-aarch64, linux-x86_64-musl,
       linux-aarch64-musl, macos-x86_64, macos-aarch64, windows-x86_64 -->
  <!-- <classifier>linux-x86_64</classifier> -->
</dependency>

<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/lihongjie0209/anydoc-java</url>
  </repository>
</repositories>
```

Gradle:

```kotlin
repositories {
    maven { url = uri("https://maven.pkg.github.com/lihongjie0209/anydoc-java") }
}

dependencies {
    implementation("io.github.lihongjie0209:anydoc:0.1.9")
}
```

GitHub Packages needs a token. See [`settings.xml.example`](settings.xml.example).

## Usage

All public types live in `dev.firecrawl.anydoc`. The entry point is `Anydoc`. Format is detected from file content; the path extension is only a fallback for signature-less formats (CSV) and unrecognizable containers. In-memory CSV must name `Format.CSV` explicitly.

### Convert to Markdown

```java
import dev.firecrawl.anydoc.Anydoc;
import dev.firecrawl.anydoc.Format;

import java.nio.file.Files;
import java.nio.file.Paths;

String fromFile = Anydoc.toMarkdown(Paths.get("report.docx"));
String fromBytes = Anydoc.toMarkdownBytes(Files.readAllBytes(Paths.get("report.docx")));
String fromCsv = Anydoc.toMarkdownBytes(csvBytes, Format.CSV);
```

`toMarkdown(Path)` / `toMarkdown(String)` throw `IOException` if the file cannot be read. Conversion failures throw a `ConvertException` subclass (unchecked).

### Parse the document model

```java
import dev.firecrawl.anydoc.Anydoc;
import dev.firecrawl.anydoc.Block;
import dev.firecrawl.anydoc.Document;
import dev.firecrawl.anydoc.Inlines;

import java.nio.file.Paths;

Document document = Anydoc.toDocument(Paths.get("report.docx"));

for (Block block : document.blocks()) {
    if (block instanceof Block.Heading) {
        Block.Heading heading = (Block.Heading) block;
        System.out.println(heading.level() + " " + Inlines.toPlainText(heading.content()));
    } else if (block instanceof Block.Paragraph) {
        System.out.println(Inlines.toPlainText(((Block.Paragraph) block).content()));
    } else if (block instanceof Block.ListBlock) {
        System.out.println("list items: " + ((Block.ListBlock) block).list().items().size());
    } else if (block instanceof Block.TableBlock) {
        System.out.println("rows: " + ((Block.TableBlock) block).table().grid().size());
    } else if (block instanceof Block.BlockQuote) {
        System.out.println("quote blocks: " + ((Block.BlockQuote) block).blocks().size());
    } else if (block instanceof Block.CodeBlock) {
        Block.CodeBlock code = (Block.CodeBlock) block;
        System.out.println(code.lang() + "\n" + code.text());
    } else if (block instanceof Block.Rule) {
        System.out.println("---");
    }
}
```

PDF has no document-model form: use `toMarkdown`. `toDocument` on a PDF throws `UnsupportedException`.

### Detect the format

```java
Anydoc.formatFromBytes(bytes);      // content signature; empty for CSV / unknown
Anydoc.formatFromPath("sheet.csv"); // extension only
Anydoc.formatFromExtension(".pptm"); // container variants map onto Format (PPTX, …)
```

Path-based `toMarkdown` / `toDocument` try content first, then the extension.

### Load the native library

The JNI library is extracted from `/native/{classifier}/` inside the JAR on first use of `Anydoc`. Override when the automatic guess is wrong:

| Property | Meaning |
| -------- | ------- |
| `-Danydoc.native.classifier=linux-x86_64` | Force a packaged classifier |
| `-Danydoc.native.path=/path/to/libanydoc_java.so` | Load a file from disk |

## Core API

The binding is a thin, typed view of the crate. Variant types (`Block`, `Inline`, `CellSlot`, `LinkTarget`, `ImageSource`) are interfaces with nested implementation classes, meant to be inspected with `instanceof`.

### `Anydoc`

| Method | Returns | Notes |
| ------ | ------- | ----- |
| `toMarkdown(Path \| String)` | `String` | File on disk; may throw `IOException` |
| `toMarkdownBytes(byte[])` | `String` | Detect format from content |
| `toMarkdownBytes(byte[], Format)` | `String` | Required for CSV |
| `toDocument(Path \| String)` | `Document` | Same detect rules as `toMarkdown`; not for PDF |
| `toDocument(byte[])` | `Document` | Detect format from content |
| `toDocument(byte[], Format)` | `Document` | Required for CSV |
| `formatFromBytes(byte[])` | `Optional<Format>` | Signature / container identity |
| `formatFromPath(Path \| String)` | `Optional<Format>` | Extension only |
| `formatFromExtension(String)` | `Optional<Format>` | With or without a leading dot |

### `Format`

`DOC`, `DOCX`, `ODT`, `PDF`, `PPT`, `PPTX`, `RTF`, `EPUB`, `XLSX`, `ODS`, `ODP`, `CSV`.

`wireName()` is the lowercase name other bindings use (`"docx"`, `"xlsx"`). `Format.fromWireName("docx")` parses it. Container siblings (`.docm`, `.xlsm`, `.ppsx`, …) collapse onto these values.

### `Document`

```text
Document
├── blocks()   List<Block>    body
├── notes()    List<Note>     footnotes / endnotes
└── assets()   List<Asset>    embedded binaries (images, objects)
```

A document is self-contained: asset bytes stay on the `Asset`, so you do not need the original file after parse.

### `Block`

| Type | `kind()` | Fields |
| ---- | -------- | ------ |
| `Block.Heading` | `heading` | `level`, `anchor`, `content` (`List<Inline>`) |
| `Block.Paragraph` | `paragraph` | `content` |
| `Block.ListBlock` | `list` | `list` (`DocList`) |
| `Block.TableBlock` | `table` | `table` (`Table`) |
| `Block.BlockQuote` | `block_quote` | `blocks` (nested) |
| `Block.CodeBlock` | `code_block` | `lang`, `text` |
| `Block.Rule` | `rule` | (thematic break) |

### `Inline`

| Type | `kind()` | Fields |
| ---- | -------- | ------ |
| `Inline.Text` | `text` | `text`, `style` (`Style`: bold / italic / strike / code) |
| `Inline.Link` | `link` | `content`, `target` (`LinkTarget`) |
| `Inline.Image` | `image` | `alt`, `source` (`ImageSource`) |
| `Inline.Anchor` | `anchor` | `id` (zero-width internal target) |
| `Inline.NoteRef` | `note_ref` | `noteId` → `Document.notes()` |
| `Inline.LineBreak` | `line_break` | |

`Style.PLAIN` is all toggles off.

**`LinkTarget`:** `External` (absolute URL), `Relative` (as written), `Anchor` (heading or `Inline.Anchor`). `isEmpty()` is true when the target string is empty.

**`ImageSource`:** `External` (URL), `AssetRef` (`assetId` into `Document.assets()`), `Unavailable` (alt text only).

### Lists

`DocList(marker, start, items)` — named so it does not collide with `java.util.List`.

- `marker()`: `MarkerKind` — `BULLET`, `DECIMAL`, `LOWER_ALPHA`, `UPPER_ALPHA`, `LOWER_ROMAN`, `UPPER_ROMAN`
- `ordered()`: every kind except `BULLET`
- `marker.label(n)` / `marker.ordinal(n)`: `3.`, `c.`, `iv.` (1-based)
- `ListItem(blocks, checked, markerLabel)`: `checked` is non-null for task-list items; `blocks` may nest further lists

### Tables

`Table(grid, headerRows, kind)` is a canonical grid: every logical position appears once.

- `TableKind.DATA` vs `TableKind.LAYOUT` (text boxes / positioning)
- `isSingleCell()`: one origin cell
- `CellSlot.Origin(cell)` holds content and spans
- `CellSlot.Covered(originRow, originCol)` is padding under a span
- `Cell(blocks, colSpan, rowSpan).isEmpty()`: true only when every block is an empty paragraph

### Notes and assets

- `Note(id, kind, blocks)` — `NoteKind.FOOTNOTE` or `ENDNOTE`
- `Asset(id, mediaType, originPart, data)` — `data()` returns a clone; `id` matches `ImageSource.AssetRef`

### Helpers

| Type | Method | Role |
| ---- | ------ | ---- |
| `Inlines` | `toPlainText(List<Inline>)` | Flatten text; keep link text and image alt; drop anchors / note refs; line breaks → `\n` |
| `Inlines` | `areEmpty(List<Inline>)` | True when nothing would render (whitespace, empty-target links, anchors, breaks) |
| `Platform` | `detect()` / `all()` | Native target the loader picked |

## Errors

Catch `ConvertException` for every conversion failure, or a subclass to single one out.

| Exception | `code()` | When |
| --------- | -------- | ---- |
| `UnsupportedException` | `unsupported` | Unknown format, image-only / scanned PDF, or `toDocument` on a PDF |
| `MalformedException` | `malformed` | Structurally unusable; `part()` names the stream if one is at fault |
| `EncryptedException` | `encrypted` | Encrypted or password-protected |
| `ResourceLimitException` | `resourceLimit` | Safety limit; `limit()` names it (`max_entry_bytes`, …) |
| `MissingPartException` | `missingPart` | A required package part is absent; `part()` names it |

`code()` matches the Node/wasm `error.code` strings. An unreadable file from `toMarkdown(Path)` throws `IOException`, not `ConvertException`.

```java
try {
    String markdown = Anydoc.toMarkdown(path);
} catch (EncryptedException e) {
    // ask for a password elsewhere; anydoc does not decrypt
} catch (ConvertException e) {
    System.err.println(e.code() + ": " + e.getMessage());
}
```

## Supported platforms

| Classifier            | Rust target                   | Notes                          |
| --------------------- | ----------------------------- | ------------------------------ |
| `linux-x86_64`        | `x86_64-unknown-linux-gnu`    | glibc **2.17+** (CentOS 7)     |
| `linux-aarch64`       | `aarch64-unknown-linux-gnu`   | glibc **2.17+**                |
| `linux-x86_64-musl`   | `x86_64-unknown-linux-musl`   | Alpine                         |
| `linux-aarch64-musl`  | `aarch64-unknown-linux-musl`  | Alpine arm64                   |
| `macos-x86_64`        | `x86_64-apple-darwin`         | Intel Mac                      |
| `macos-aarch64`       | `aarch64-apple-darwin`        | Apple Silicon                  |
| `windows-x86_64`      | `x86_64-pc-windows-msvc`      | Windows x64                    |

## Building

Linux GNU targets need [Zig](https://ziglang.org) and [`cargo-zigbuild`](https://github.com/rust-cross/cargo-zigbuild) so the `.so` stays on glibc 2.17:

```bash
cargo install cargo-zigbuild
# plus a Zig install on PATH

scripts/build-native.sh              # this machine
scripts/build-native.sh linux-x86_64 # explicit classifier
scripts/build-native.sh --all        # every target this host can build
mvn test
```

`mvn test` runs `scripts/build-native.sh` itself. After staging several classifiers into `native/dist/`:

```bash
mvn -DskipNative -DskipTests package
scripts/package-jars.sh
```

That produces `target/anydoc-0.1.9.jar` (fat) and `target/anydoc-0.1.9-<classifier>.jar`.

A tagged `v*` push (or **Actions → CI → Run workflow** on `main`) builds every platform, publishes the fat JAR plus each classifier to [GitHub Packages](https://github.com/lihongjie0209/anydoc-java/packages), and attaches the JARs to the GitHub Release.

Pull requests — including the automated upstream bump — only compile the host JNI library and run unit tests.

## Upstream anydoc

The JNI crate pins [firecrawl/anydoc](https://github.com/firecrawl/anydoc) by git tag in `native/Cargo.toml`. A scheduled workflow ([Upstream anydoc](.github/workflows/upstream.yml), daily plus **Run workflow**) checks GitHub Releases:

1. If a newer `v*` tag exists, it rewrites the pin and `cargo update`s `Cargo.lock`.
2. It opens a PR on `upstream/anydoc-vX.Y.Z`.
3. It dispatches **CI** on that branch so compile + unit tests run against the new crate.

Merge the PR only when that check is green.

To have the PR itself emit the usual `pull_request` CI check (instead of a dispatched run), add a classic PAT with `repo` as the `UPSTREAM_BUMP_TOKEN` repository secret. Without it, `GITHUB_TOKEN` can still open the PR; GitHub will not start a `pull_request` workflow from that token, so the bump job dispatches CI explicitly.

Locally:

```bash
scripts/bump-upstream.sh check          # current vs latest
scripts/bump-upstream.sh apply          # rewrite the pin to latest
scripts/bump-upstream.sh apply v0.1.9   # pin a specific tag
```

## License

[MIT](LICENSE)
