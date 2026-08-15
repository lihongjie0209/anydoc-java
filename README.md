# anydoc for Java

[![CI](https://github.com/lihongjie0209/anydoc-java/actions/workflows/ci.yml/badge.svg)](https://github.com/lihongjie0209/anydoc-java/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Convert Word, PowerPoint, Excel, OpenDocument, RTF, EPUB, CSV, and PDF files into clean GitHub-Flavored Markdown. Java 21 bindings for the [anydoc](https://github.com/firecrawl/anydoc) Rust crate.

Requires **Java 21**. Linux GNU builds are linked with [Zig](https://ziglang.org) against **glibc 2.17** (CentOS 7 / manylinux2014).

## Install (GitHub Packages)

Coordinates: `io.github.lihongjie0209:anydoc:0.1.8`

The default artifact is the **fat JAR** (every native library). Classifier JARs ship one platform each.

```xml
<dependency>
  <groupId>io.github.lihongjie0209</groupId>
  <artifactId>anydoc</artifactId>
  <version>0.1.8</version>
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

GitHub Packages needs a token. See [`settings.xml.example`](settings.xml.example).

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

The loader picks `/native/{classifier}/` from the JAR. Override with `-Danydoc.native.classifier=...` or `-Danydoc.native.path=/path/to/libanydoc_java.so`.

## Usage

```java
import dev.firecrawl.anydoc.Anydoc;
import dev.firecrawl.anydoc.Document;
import dev.firecrawl.anydoc.Format;

import java.nio.file.Path;

String markdown = Anydoc.toMarkdown(Path.of("report.docx"));
String fromBytes = Anydoc.toMarkdownBytes(data);
String fromCsv = Anydoc.toMarkdownBytes(data, Format.CSV);
Document document = Anydoc.toDocument(data);
```

```java
for (Block block : document.blocks()) {
    switch (block) {
        case Block.Heading heading -> System.out.println(heading.level());
        case Block.TableBlock table -> System.out.println(table.table().grid().size());
        default -> {}
    }
}
```

## Errors

| Exception                | When                                                              |
| ------------------------ | ----------------------------------------------------------------- |
| `UnsupportedException`   | Unknown format, or cannot be converted (an image-only PDF)        |
| `MalformedException`     | Structurally unusable                                             |
| `EncryptedException`     | Encrypted or password-protected                                   |
| `ResourceLimitException` | Safety limit (decompression, nesting, node count)                 |
| `MissingPartException`   | A required package part is absent                                 |
| `IOException`            | The file could not be read, from `toMarkdown` only                |

All five conversion failures subclass `ConvertException`. `code()` matches the Node/wasm `error.code` strings.

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

That produces `target/anydoc-0.1.8.jar` (fat) and `target/anydoc-0.1.8-<classifier>.jar`.

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
