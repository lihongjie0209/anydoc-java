# anydoc Java 绑定

[English](README.md) | **简体中文**

[![CI](https://github.com/lihongjie0209/anydoc-java/actions/workflows/ci.yml/badge.svg)](https://github.com/lihongjie0209/anydoc-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.lihongjie0209/anydoc.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.lihongjie0209/anydoc)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

将 Word、PowerPoint、Excel、OpenDocument、RTF、EPUB、CSV、PDF 转为干净的 GitHub Flavored Markdown，或解析为类型化文档模型。[anydoc](https://github.com/firecrawl/anydoc) Rust crate 的 **Java 8** 绑定。

Linux GNU 构建用 [Zig](https://ziglang.org) 链接，最低 **glibc 2.17**（CentOS 7 / manylinux2014）。

## 运行要求

支持 **Java 8 及以上**（含 11、17、21）。发布的 JAR 以 `-source 1.8` / `-target 1.8` 编译。

公开模型按 Java 8 设计：`Block`、`Inline`、`CellSlot`、`LinkTarget`、`ImageSource` 是带嵌套实现类的接口，用 `instanceof` 加强制转换判断类型，API 里没有 record、sealed、模式匹配 `switch`。

## 安装

已发布到 [Maven Central](https://central.sonatype.com/artifact/io.github.lihongjie0209/anydoc/0.1.10)，**不必**再加 `<repository>`。

坐标：`io.github.lihongjie0209:anydoc:0.1.10`

默认产物是 **fat JAR**（包含全部平台 native）。分平台 classifier JAR 在 [GitHub Release](https://github.com/lihongjie0209/anydoc-java/releases/tag/v0.1.10)。

Maven：

```xml
<dependency>
  <groupId>io.github.lihongjie0209</groupId>
  <artifactId>anydoc</artifactId>
  <version>0.1.10</version>
  <!-- 可选：linux-x86_64、linux-aarch64、linux-x86_64-musl、
       linux-aarch64-musl、macos-x86_64、macos-aarch64、windows-x86_64 -->
  <!-- <classifier>linux-x86_64</classifier> -->
</dependency>
```

Gradle：

```kotlin
dependencies {
    implementation("io.github.lihongjie0209:anydoc:0.1.10")
}
```

打 tag 的构建还会同步到 [GitHub Packages](https://github.com/lihongjie0209/anydoc-java/packages)（需要 token，见 [`settings.xml.example`](settings.xml.example)）。

## 使用

公开类型都在 `dev.firecrawl.anydoc`，入口是 `Anydoc`。格式优先按文件内容探测；扩展名只作为无签名格式（CSV）和无法识别容器的回退。内存中的 CSV 必须显式传入 `Format.CSV`。

### 转为 Markdown

```java
import dev.firecrawl.anydoc.Anydoc;
import dev.firecrawl.anydoc.Format;

import java.nio.file.Files;
import java.nio.file.Paths;

String fromFile = Anydoc.toMarkdown(Paths.get("report.docx"));
String fromBytes = Anydoc.toMarkdownBytes(Files.readAllBytes(Paths.get("report.docx")));
String fromCsv = Anydoc.toMarkdownBytes(csvBytes, Format.CSV);
```

`toMarkdown(Path)` / `toMarkdown(String)` 在文件读失败时抛 `IOException`。转换失败抛 `ConvertException` 的子类（非检查异常）。

### 解析文档模型

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

PDF 没有文档模型，请用 `toMarkdown`。对 PDF 调用 `toDocument` 会抛 `UnsupportedException`。

### 探测格式

```java
Anydoc.formatFromBytes(bytes);      // 内容签名；CSV / 未知返回 empty
Anydoc.formatFromPath("sheet.csv"); // 只看扩展名
Anydoc.formatFromExtension(".pptm"); // 容器变体映射到 Format（PPTX 等）
```

基于路径的 `toMarkdown` / `toDocument` 先看内容，再看扩展名。

### 加载 native 库

首次使用 `Anydoc` 时，会从 JAR 内 `/native/{classifier}/` 抽出 JNI 库。自动探测不准时可用：

| 属性 | 含义 |
| ---- | ---- |
| `-Danydoc.native.classifier=linux-x86_64` | 强制使用某个打包好的 classifier |
| `-Danydoc.native.path=/path/to/libanydoc_java.so` | 从磁盘加载指定文件 |

## 核心接口

绑定是 crate 的一层薄类型视图。变体类型（`Block`、`Inline`、`CellSlot`、`LinkTarget`、`ImageSource`）是带嵌套实现类的接口，用 `instanceof` 判断。

### `Anydoc`

| 方法 | 返回 | 说明 |
| ---- | ---- | ---- |
| `toMarkdown(Path \| String)` | `String` | 磁盘文件；可能抛 `IOException` |
| `toMarkdownBytes(byte[])` | `String` | 按内容探测格式 |
| `toMarkdownBytes(byte[], Format)` | `String` | CSV 必须指定 |
| `toDocument(Path \| String)` | `Document` | 探测规则同 `toMarkdown`；不支持 PDF |
| `toDocument(byte[])` | `Document` | 按内容探测格式 |
| `toDocument(byte[], Format)` | `Document` | CSV 必须指定 |
| `formatFromBytes(byte[])` | `Optional<Format>` | 签名 / 容器身份 |
| `formatFromPath(Path \| String)` | `Optional<Format>` | 只看扩展名 |
| `formatFromExtension(String)` | `Optional<Format>` | 可带或不带前导点 |

### `Format`

`DOC`、`DOCX`、`ODT`、`PDF`、`PPT`、`PPTX`、`RTF`、`EPUB`、`XLSX`、`ODS`、`ODP`、`CSV`。

`wireName()` 是其它语言绑定用的小写名（`"docx"`、`"xlsx"`）。`Format.fromWireName("docx")` 可解析。容器兄弟格式（`.docm`、`.xlsm`、`.ppsx` 等）会归并到这些值。

### `Document`

```text
Document
├── blocks()   List<Block>    正文
├── notes()    List<Note>     脚注 / 尾注
└── assets()   List<Asset>    内嵌二进制（图片、对象）
```

文档是自包含的：资源字节在 `Asset` 上，解析后不必再保留原文件。

### `Block`

| 类型 | `kind()` | 字段 |
| ---- | -------- | ---- |
| `Block.Heading` | `heading` | `level`、`anchor`、`content`（`List<Inline>`） |
| `Block.Paragraph` | `paragraph` | `content` |
| `Block.ListBlock` | `list` | `list`（`DocList`） |
| `Block.TableBlock` | `table` | `table`（`Table`） |
| `Block.BlockQuote` | `block_quote` | `blocks`（嵌套） |
| `Block.CodeBlock` | `code_block` | `lang`、`text` |
| `Block.Rule` | `rule` | （分隔线） |

### `Inline`

| 类型 | `kind()` | 字段 |
| ---- | -------- | ---- |
| `Inline.Text` | `text` | `text`、`style`（`Style`：粗体 / 斜体 / 删除线 / 代码） |
| `Inline.Link` | `link` | `content`、`target`（`LinkTarget`） |
| `Inline.Image` | `image` | `alt`、`source`（`ImageSource`） |
| `Inline.Anchor` | `anchor` | `id`（零宽内部锚点） |
| `Inline.NoteRef` | `note_ref` | `noteId` → `Document.notes()` |
| `Inline.LineBreak` | `line_break` | |

`Style.PLAIN` 表示所有开关关闭。

**`LinkTarget`：** `External`（带协议的绝对 URL）、`Relative`（原文相对引用）、`Anchor`（标题或 `Inline.Anchor`）。目标字符串为空时 `isEmpty()` 为 true。

**`ImageSource`：** `External`（URL）、`AssetRef`（`assetId` 指向 `Document.assets()`）、`Unavailable`（只剩 alt）。

### 列表

`DocList(marker, start, items)` — 避免和 `java.util.List` 重名。

- `marker()`：`MarkerKind` — `BULLET`、`DECIMAL`、`LOWER_ALPHA`、`UPPER_ALPHA`、`LOWER_ROMAN`、`UPPER_ROMAN`
- `ordered()`：除 `BULLET` 外都为 true
- `marker.label(n)` / `marker.ordinal(n)`：`3.`、`c.`、`iv.`（从 1 起）
- `ListItem(blocks, checked, markerLabel)`：任务列表项的 `checked` 非 null；`blocks` 里可以再嵌套列表

### 表格

`Table(grid, headerRows, kind)` 是规范网格：每个逻辑格子只出现一次。

- `TableKind.DATA` 与 `TableKind.LAYOUT`（文本框 / 定位表）
- `isSingleCell()`：只有一个 origin 单元格
- `CellSlot.Origin(cell)` 保存内容和跨度
- `CellSlot.Covered(originRow, originCol)` 是跨度覆盖的占位
- `Cell(blocks, colSpan, rowSpan).isEmpty()`：仅当每个块都是空段落时为 true

### 注释与资源

- `Note(id, kind, blocks)` — `NoteKind.FOOTNOTE` 或 `ENDNOTE`
- `Asset(id, mediaType, originPart, data)` — `data()` 返回副本；`id` 对应 `ImageSource.AssetRef`

### 辅助方法

| 类型 | 方法 | 作用 |
| ---- | ---- | ---- |
| `Inlines` | `toPlainText(List<Inline>)` | 展平为纯文本；保留链接文字和图片 alt；丢掉锚点和注释引用；换行变成 `\n` |
| `Inlines` | `areEmpty(List<Inline>)` | 没有任何会渲染的内容（空白、空目标链接、锚点、换行）时为 true |
| `Platform` | `detect()` / `all()` | 加载器选中的 native 目标 |

## 错误

转换失败统一可 catch `ConvertException`，或 catch 某个子类单独处理。

| 异常 | `code()` | 何时 |
| ---- | -------- | ---- |
| `UnsupportedException` | `unsupported` | 未知格式、纯图片 / 扫描 PDF，或对 PDF 调用 `toDocument` |
| `MalformedException` | `malformed` | 结构无法使用；`part()` 指出有问题的流（若有） |
| `EncryptedException` | `encrypted` | 加密或有密码保护 |
| `ResourceLimitException` | `resourceLimit` | 触发安全上限；`limit()` 给出名称（如 `max_entry_bytes`） |
| `MissingPartException` | `missingPart` | 缺少必要包部件；`part()` 指出是哪一块 |

`code()` 与 Node/wasm 的 `error.code` 一致。`toMarkdown(Path)` 读不到文件时抛 `IOException`，不是 `ConvertException`。

```java
try {
    String markdown = Anydoc.toMarkdown(path);
} catch (EncryptedException e) {
    // 另处索要密码；anydoc 不解密
} catch (ConvertException e) {
    System.err.println(e.code() + ": " + e.getMessage());
}
```

## 支持的平台

| Classifier            | Rust target                   | 说明                           |
| --------------------- | ----------------------------- | ------------------------------ |
| `linux-x86_64`        | `x86_64-unknown-linux-gnu`    | glibc **2.17+**（CentOS 7）    |
| `linux-aarch64`       | `aarch64-unknown-linux-gnu`   | glibc **2.17+**                |
| `linux-x86_64-musl`   | `x86_64-unknown-linux-musl`   | Alpine                         |
| `linux-aarch64-musl`  | `aarch64-unknown-linux-musl`  | Alpine arm64                   |
| `macos-x86_64`        | `x86_64-apple-darwin`         | Intel Mac                      |
| `macos-aarch64`       | `aarch64-apple-darwin`        | Apple Silicon                  |
| `windows-x86_64`      | `x86_64-pc-windows-msvc`      | Windows x64                    |

## 构建

Linux GNU 目标需要 [Zig](https://ziglang.org) 和 [`cargo-zigbuild`](https://github.com/rust-cross/cargo-zigbuild)，才能把 `.so` 钉在 glibc 2.17：

```bash
cargo install cargo-zigbuild
# 并把 Zig 放进 PATH

scripts/build-native.sh              # 当前机器
scripts/build-native.sh linux-x86_64 # 指定 classifier
scripts/build-native.sh --all        # 本机编得动的全部目标
mvn test
```

`mvn test` 自己会跑 `scripts/build-native.sh`。把多个 classifier 放到 `native/dist/` 之后：

```bash
mvn -DskipNative -DskipTests package
scripts/package-jars.sh
```

会得到 `target/anydoc-0.1.10.jar`（fat）和 `target/anydoc-0.1.10-<classifier>.jar`。

推送 `v*` tag（或在 `main` 上 **Actions → CI → Run workflow**）会编全部平台，把 fat JAR 发到 [Maven Central](https://central.sonatype.com/artifact/io.github.lihongjie0209/anydoc)，同时把 fat JAR 和各 classifier 拷到 [GitHub Packages](https://github.com/lihongjie0209/anydoc-java/packages)，并挂到 GitHub Release。

Pull request（含自动上游 bump）只编译本机 JNI 并跑单测。

## 上游 anydoc

JNI crate 在 `native/Cargo.toml` 里按 git tag 钉住 [firecrawl/anydoc](https://github.com/firecrawl/anydoc)。定时工作流（[Upstream anydoc](.github/workflows/upstream.yml)，每天一次，也可 **Run workflow**）会查 GitHub Releases：

1. 若有更新的 `v*` tag，就改 pin 并 `cargo update` `Cargo.lock`。
2. 在 `upstream/anydoc-vX.Y.Z` 开 PR。
3. 对该分支 dispatch **CI**，跑编译和单测。

检查变绿后再合并。

若希望 PR 本身触发常规 `pull_request` CI（而不是 dispatched run），把带 `repo` 权限的 classic PAT 配成 `UPSTREAM_BUMP_TOKEN`。没有的话 `GITHUB_TOKEN` 仍能开 PR，但 GitHub 不会用该 token 启动 `pull_request` 工作流，所以 bump 任务会显式 dispatch CI。

本地：

```bash
scripts/bump-upstream.sh check          # 当前 vs 最新
scripts/bump-upstream.sh apply          # 把 pin 改到最新
scripts/bump-upstream.sh apply v0.1.9   # 钉到指定 tag
```

## 许可证

[MIT](LICENSE)
