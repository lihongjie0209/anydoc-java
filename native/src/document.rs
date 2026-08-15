//! Convert the Rust document model into Java objects.

use jni::Env;
use jni::objects::{JObject, JString, JValue};
use jni::strings::JNIStr;
use jni::{jni_sig, jni_str};

use anydoc::model;

use crate::error::BindError;

pub fn document<'local>(
    env: &mut Env<'local>,
    document: model::Document,
) -> Result<JObject<'local>, BindError> {
    let blocks = blocks(env, document.blocks)?;
    let notes = map_list(env, document.notes, note_to_java)?;
    let assets = map_list(env, document.assets, asset_to_java)?;
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/Document"),
        jni_sig!((java.util.List, java.util.List, java.util.List) -> void),
        &[JValue::Object(&blocks), JValue::Object(&notes), JValue::Object(&assets)],
    )?)
}

fn blocks<'local>(
    env: &mut Env<'local>,
    items: Vec<model::Block>,
) -> Result<JObject<'local>, BindError> {
    map_list(env, items, block_to_java)
}

fn inlines<'local>(
    env: &mut Env<'local>,
    items: Vec<model::Inline>,
) -> Result<JObject<'local>, BindError> {
    map_list(env, items, inline_to_java)
}

fn block_to_java<'local>(
    env: &mut Env<'local>,
    block: model::Block,
) -> Result<JObject<'local>, BindError> {
    match block {
        model::Block::Heading { level, anchor, content } => {
            let anchor = opt_string(env, anchor)?;
            let content = inlines(env, content)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Block$Heading"),
                jni_sig!((int, java.lang.String, java.util.List) -> void),
                &[JValue::Int(i32::from(level)), JValue::Object(&anchor), JValue::Object(&content)],
            )?)
        }
        model::Block::Paragraph(content) => {
            let content = inlines(env, content)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Block$Paragraph"),
                jni_sig!((java.util.List) -> void),
                &[JValue::Object(&content)],
            )?)
        }
        model::Block::List(list) => {
            let list = list_to_java(env, list)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Block$ListBlock"),
                jni_sig!((dev.firecrawl.anydoc.DocList) -> void),
                &[JValue::Object(&list)],
            )?)
        }
        model::Block::Table(table) => {
            let table = table_to_java(env, table)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Block$TableBlock"),
                jni_sig!((dev.firecrawl.anydoc.Table) -> void),
                &[JValue::Object(&table)],
            )?)
        }
        model::Block::BlockQuote(inner) => {
            let inner = blocks(env, inner)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Block$BlockQuote"),
                jni_sig!((java.util.List) -> void),
                &[JValue::Object(&inner)],
            )?)
        }
        model::Block::CodeBlock { lang, text } => {
            let lang = opt_string(env, lang)?;
            let text = java_string(env, &text)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Block$CodeBlock"),
                jni_sig!((java.lang.String, java.lang.String) -> void),
                &[JValue::Object(&lang), JValue::Object(&text)],
            )?)
        }
        model::Block::Rule => Ok(env.new_object(
            jni_str!("dev/firecrawl/anydoc/Block$Rule"),
            jni_sig!(() -> void),
            &[],
        )?),
    }
}

fn inline_to_java<'local>(
    env: &mut Env<'local>,
    inline: model::Inline,
) -> Result<JObject<'local>, BindError> {
    match inline {
        model::Inline::Text { text, style } => {
            let text = java_string(env, &text)?;
            let style = style_to_java(env, style)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Inline$Text"),
                jni_sig!((java.lang.String, dev.firecrawl.anydoc.Style) -> void),
                &[JValue::Object(&text), JValue::Object(&style)],
            )?)
        }
        model::Inline::Link { content, target } => {
            let content = inlines(env, content)?;
            let target = link_target_to_java(env, target)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Inline$Link"),
                jni_sig!((java.util.List, dev.firecrawl.anydoc.LinkTarget) -> void),
                &[JValue::Object(&content), JValue::Object(&target)],
            )?)
        }
        model::Inline::Image { alt, source } => {
            let alt = java_string(env, &alt)?;
            let source = image_source_to_java(env, source)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Inline$Image"),
                jni_sig!((java.lang.String, dev.firecrawl.anydoc.ImageSource) -> void),
                &[JValue::Object(&alt), JValue::Object(&source)],
            )?)
        }
        model::Inline::Anchor(id) => {
            let id = java_string(env, &id)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Inline$Anchor"),
                jni_sig!((java.lang.String) -> void),
                &[JValue::Object(&id)],
            )?)
        }
        model::Inline::NoteRef(id) => {
            let id = java_string(env, &id)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/Inline$NoteRef"),
                jni_sig!((java.lang.String) -> void),
                &[JValue::Object(&id)],
            )?)
        }
        model::Inline::LineBreak => Ok(env.new_object(
            jni_str!("dev/firecrawl/anydoc/Inline$LineBreak"),
            jni_sig!(() -> void),
            &[],
        )?),
    }
}

fn style_to_java<'local>(
    env: &mut Env<'local>,
    style: model::Style,
) -> Result<JObject<'local>, BindError> {
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/Style"),
        jni_sig!((bool, bool, bool, bool) -> void),
        &[
            JValue::Bool(style.bold),
            JValue::Bool(style.italic),
            JValue::Bool(style.strike),
            JValue::Bool(style.code),
        ],
    )?)
}

fn link_target_to_java<'local>(
    env: &mut Env<'local>,
    target: model::LinkTarget,
) -> Result<JObject<'local>, BindError> {
    let (class, value): (&JNIStr, String) = match target {
        model::LinkTarget::External(value) => {
            (jni_str!("dev/firecrawl/anydoc/LinkTarget$External"), value)
        }
        model::LinkTarget::Relative(value) => {
            (jni_str!("dev/firecrawl/anydoc/LinkTarget$Relative"), value)
        }
        model::LinkTarget::Anchor(value) => {
            (jni_str!("dev/firecrawl/anydoc/LinkTarget$Anchor"), value)
        }
    };
    let value = java_string(env, &value)?;
    Ok(env.new_object(class, jni_sig!((java.lang.String) -> void), &[JValue::Object(&value)])?)
}

fn image_source_to_java<'local>(
    env: &mut Env<'local>,
    source: model::ImageSource,
) -> Result<JObject<'local>, BindError> {
    match source {
        model::ImageSource::External(url) => {
            let url = java_string(env, &url)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/ImageSource$External"),
                jni_sig!((java.lang.String) -> void),
                &[JValue::Object(&url)],
            )?)
        }
        model::ImageSource::Asset(id) => Ok(env.new_object(
            jni_str!("dev/firecrawl/anydoc/ImageSource$AssetRef"),
            jni_sig!((int) -> void),
            &[JValue::Int(usize_to_i32(id.0))],
        )?),
        model::ImageSource::Unavailable => Ok(env.new_object(
            jni_str!("dev/firecrawl/anydoc/ImageSource$Unavailable"),
            jni_sig!(() -> void),
            &[],
        )?),
    }
}

fn list_to_java<'local>(
    env: &mut Env<'local>,
    list: model::List,
) -> Result<JObject<'local>, BindError> {
    let marker = marker_kind(env, list.marker)?;
    let items = map_list(env, list.items, list_item_to_java)?;
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/DocList"),
        jni_sig!((dev.firecrawl.anydoc.MarkerKind, long, java.util.List) -> void),
        &[JValue::Object(&marker), JValue::Long(u64_to_i64(list.start)), JValue::Object(&items)],
    )?)
}

fn list_item_to_java<'local>(
    env: &mut Env<'local>,
    item: model::ListItem,
) -> Result<JObject<'local>, BindError> {
    let blocks = blocks(env, item.blocks)?;
    let checked = match item.checked {
        Some(value) => env
            .call_static_method(
                jni_str!("java/lang/Boolean"),
                jni_str!("valueOf"),
                jni_sig!((bool) -> java.lang.Boolean),
                &[JValue::Bool(value)],
            )?
            .l()?,
        None => JObject::null(),
    };
    let marker_label = opt_string(env, item.marker_label)?;
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/ListItem"),
        jni_sig!((java.util.List, java.lang.Boolean, java.lang.String) -> void),
        &[JValue::Object(&blocks), JValue::Object(&checked), JValue::Object(&marker_label)],
    )?)
}

fn table_to_java<'local>(
    env: &mut Env<'local>,
    table: model::Table,
) -> Result<JObject<'local>, BindError> {
    let grid = map_list(env, table.grid, |env, row| map_list(env, row, cell_slot_to_java))?;
    let kind = table_kind(env, table.kind)?;
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/Table"),
        jni_sig!((java.util.List, int, dev.firecrawl.anydoc.TableKind) -> void),
        &[
            JValue::Object(&grid),
            JValue::Int(usize_to_i32(table.header_rows)),
            JValue::Object(&kind),
        ],
    )?)
}

fn cell_slot_to_java<'local>(
    env: &mut Env<'local>,
    slot: model::CellSlot,
) -> Result<JObject<'local>, BindError> {
    match slot {
        model::CellSlot::Origin(cell) => {
            let cell = cell_to_java(env, cell)?;
            Ok(env.new_object(
                jni_str!("dev/firecrawl/anydoc/CellSlot$Origin"),
                jni_sig!((dev.firecrawl.anydoc.Cell) -> void),
                &[JValue::Object(&cell)],
            )?)
        }
        model::CellSlot::Covered { origin_row, origin_col } => Ok(env.new_object(
            jni_str!("dev/firecrawl/anydoc/CellSlot$Covered"),
            jni_sig!((int, int) -> void),
            &[JValue::Int(usize_to_i32(origin_row)), JValue::Int(usize_to_i32(origin_col))],
        )?),
    }
}

fn cell_to_java<'local>(
    env: &mut Env<'local>,
    cell: model::Cell,
) -> Result<JObject<'local>, BindError> {
    let blocks = blocks(env, cell.blocks)?;
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/Cell"),
        jni_sig!((java.util.List, int, int) -> void),
        &[
            JValue::Object(&blocks),
            JValue::Int(u32_to_i32(cell.col_span)),
            JValue::Int(u32_to_i32(cell.row_span)),
        ],
    )?)
}

fn note_to_java<'local>(
    env: &mut Env<'local>,
    note: model::Note,
) -> Result<JObject<'local>, BindError> {
    let id = java_string(env, &note.id)?;
    let kind = note_kind(env, note.kind)?;
    let blocks = blocks(env, note.blocks)?;
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/Note"),
        jni_sig!((java.lang.String, dev.firecrawl.anydoc.NoteKind, java.util.List) -> void),
        &[JValue::Object(&id), JValue::Object(&kind), JValue::Object(&blocks)],
    )?)
}

fn asset_to_java<'local>(
    env: &mut Env<'local>,
    asset: model::Asset,
) -> Result<JObject<'local>, BindError> {
    let media_type = java_string(env, &asset.media_type)?;
    let origin_part = java_string(env, &asset.origin_part)?;
    let data = env.byte_array_from_slice(&asset.bytes)?;
    Ok(env.new_object(
        jni_str!("dev/firecrawl/anydoc/Asset"),
        jni_sig!((int, java.lang.String, java.lang.String, [byte]) -> void),
        &[
            JValue::Int(usize_to_i32(asset.id.0)),
            JValue::Object(&media_type),
            JValue::Object(&origin_part),
            JValue::Object(data.as_ref()),
        ],
    )?)
}

fn marker_kind<'local>(
    env: &mut Env<'local>,
    marker: model::MarkerKind,
) -> Result<JObject<'local>, BindError> {
    let name = match marker {
        model::MarkerKind::Bullet => "BULLET",
        model::MarkerKind::Decimal => "DECIMAL",
        model::MarkerKind::LowerAlpha => "LOWER_ALPHA",
        model::MarkerKind::UpperAlpha => "UPPER_ALPHA",
        model::MarkerKind::LowerRoman => "LOWER_ROMAN",
        model::MarkerKind::UpperRoman => "UPPER_ROMAN",
    };
    java_enum(
        env,
        jni_str!("dev/firecrawl/anydoc/MarkerKind"),
        name,
        jni_sig!((java.lang.String) -> dev.firecrawl.anydoc.MarkerKind),
    )
}

fn table_kind<'local>(
    env: &mut Env<'local>,
    kind: model::TableKind,
) -> Result<JObject<'local>, BindError> {
    let name = match kind {
        model::TableKind::Data => "DATA",
        model::TableKind::Layout => "LAYOUT",
    };
    java_enum(
        env,
        jni_str!("dev/firecrawl/anydoc/TableKind"),
        name,
        jni_sig!((java.lang.String) -> dev.firecrawl.anydoc.TableKind),
    )
}

fn note_kind<'local>(
    env: &mut Env<'local>,
    kind: model::NoteKind,
) -> Result<JObject<'local>, BindError> {
    let name = match kind {
        model::NoteKind::Footnote => "FOOTNOTE",
        model::NoteKind::Endnote => "ENDNOTE",
    };
    java_enum(
        env,
        jni_str!("dev/firecrawl/anydoc/NoteKind"),
        name,
        jni_sig!((java.lang.String) -> dev.firecrawl.anydoc.NoteKind),
    )
}

fn java_enum<'local, 'sig, 'args>(
    env: &mut Env<'local>,
    class: &JNIStr,
    name: &str,
    sig: impl AsRef<jni::signature::MethodSignature<'sig, 'args>>,
) -> Result<JObject<'local>, BindError> {
    let name = java_string(env, name)?;
    Ok(env.call_static_method(class, jni_str!("valueOf"), sig, &[JValue::Object(&name)])?.l()?)
}

fn map_list<'local, T>(
    env: &mut Env<'local>,
    items: impl IntoIterator<Item = T>,
    mut map: impl FnMut(&mut Env<'local>, T) -> Result<JObject<'local>, BindError>,
) -> Result<JObject<'local>, BindError> {
    let list = env.new_object(jni_str!("java/util/ArrayList"), jni_sig!(() -> void), &[])?;
    for item in items {
        let item = map(env, item)?;
        env.call_method(
            &list,
            jni_str!("add"),
            jni_sig!((java.lang.Object) -> bool),
            &[JValue::Object(&item)],
        )?;
    }
    Ok(list)
}

fn java_string<'local>(env: &mut Env<'local>, value: &str) -> Result<JObject<'local>, BindError> {
    Ok(JString::from_str(env, value)?.into())
}

fn opt_string<'local>(
    env: &mut Env<'local>,
    value: Option<impl AsRef<str>>,
) -> Result<JObject<'local>, BindError> {
    match value {
        Some(value) => java_string(env, value.as_ref()),
        None => Ok(JObject::null()),
    }
}

fn usize_to_i32(value: usize) -> i32 {
    i32::try_from(value).unwrap_or(i32::MAX)
}

fn u32_to_i32(value: u32) -> i32 {
    i32::try_from(value).unwrap_or(i32::MAX)
}

fn u64_to_i64(value: u64) -> i64 {
    i64::try_from(value).unwrap_or(i64::MAX)
}
