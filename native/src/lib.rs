//! Java 8 JNI bindings for anydoc.

use std::path::Path;

use jni::objects::{JByteArray, JClass, JObject, JString, JValue};
use jni::{Env, EnvUnowned, jni_sig, jni_str};

mod document;
mod error;

use error::{BindError, ConvertPolicy};

const FORMATS: [(&str, anydoc::Format); 12] = [
    ("doc", anydoc::Format::Doc),
    ("docx", anydoc::Format::Docx),
    ("odt", anydoc::Format::Odt),
    ("pdf", anydoc::Format::Pdf),
    ("ppt", anydoc::Format::Ppt),
    ("pptx", anydoc::Format::Pptx),
    ("rtf", anydoc::Format::Rtf),
    ("epub", anydoc::Format::Epub),
    ("xlsx", anydoc::Format::Excel),
    ("ods", anydoc::Format::Ods),
    ("odp", anydoc::Format::Odp),
    ("csv", anydoc::Format::Csv),
];

fn format_enum_name(format: anydoc::Format) -> &'static str {
    match format {
        anydoc::Format::Doc => "DOC",
        anydoc::Format::Docx => "DOCX",
        anydoc::Format::Odt => "ODT",
        anydoc::Format::Pdf => "PDF",
        anydoc::Format::Ppt => "PPT",
        anydoc::Format::Pptx => "PPTX",
        anydoc::Format::Rtf => "RTF",
        anydoc::Format::Epub => "EPUB",
        anydoc::Format::Excel => "XLSX",
        anydoc::Format::Ods => "ODS",
        anydoc::Format::Odp => "ODP",
        anydoc::Format::Csv => "CSV",
    }
}

fn parse_format(
    env: &mut Env<'_>,
    format: &JObject<'_>,
) -> Result<Option<anydoc::Format>, BindError> {
    if format.is_null() {
        return Ok(None);
    }
    let name = env
        .call_method(format, jni_str!("wireName"), jni_sig!(() -> java.lang.String), &[])?
        .l()?;
    let name = env.cast_local::<JString>(name)?.try_to_string(env)?;
    FORMATS
        .iter()
        .find(|(known, _)| *known == name)
        .map(|(_, format)| *format)
        .map(Some)
        .ok_or_else(|| {
            BindError::Jni(jni::errors::Error::WrongJValueType("known Format", "unknown Format"))
        })
}

fn java_format<'local>(
    env: &mut Env<'local>,
    format: anydoc::Format,
) -> Result<JObject<'local>, BindError> {
    let name = JString::from_str(env, format_enum_name(format))?;
    Ok(env
        .call_static_method(
            jni_str!("dev/firecrawl/anydoc/Format"),
            jni_str!("valueOf"),
            jni_sig!((java.lang.String) -> dev.firecrawl.anydoc.Format),
            &[JValue::Object(name.as_ref())],
        )?
        .l()?)
}

fn java_string<'local>(env: &mut Env<'local>, value: &str) -> Result<JObject<'local>, BindError> {
    Ok(JString::from_str(env, value)?.into())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_firecrawl_anydoc_Anydoc_nativeToMarkdown<'local>(
    mut unowned: EnvUnowned<'local>,
    _class: JClass<'local>,
    path: JString<'local>,
) -> JObject<'local> {
    unowned
        .with_env(|env| -> Result<JObject, BindError> {
            let path = path.try_to_string(env)?;
            let markdown = anydoc::to_markdown(Path::new(&path))?;
            java_string(env, &markdown)
        })
        .resolve::<ConvertPolicy>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_firecrawl_anydoc_Anydoc_nativeToMarkdownBytes<'local>(
    mut unowned: EnvUnowned<'local>,
    _class: JClass<'local>,
    data: JByteArray<'local>,
    format: JObject<'local>,
) -> JObject<'local> {
    unowned
        .with_env(|env| -> Result<JObject, BindError> {
            let bytes = env.convert_byte_array(&data)?;
            let format = parse_format(env, &format)?;
            let markdown = anydoc::to_markdown_bytes(&bytes, format)?;
            java_string(env, &markdown)
        })
        .resolve::<ConvertPolicy>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_firecrawl_anydoc_Anydoc_nativeToDocument<'local>(
    mut unowned: EnvUnowned<'local>,
    _class: JClass<'local>,
    data: JByteArray<'local>,
    format: JObject<'local>,
) -> JObject<'local> {
    unowned
        .with_env(|env| -> Result<JObject, BindError> {
            let bytes = env.convert_byte_array(&data)?;
            let format = parse_format(env, &format)?;
            let parsed = anydoc::to_document(&bytes, format)?;
            document::document(env, parsed)
        })
        .resolve::<ConvertPolicy>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_firecrawl_anydoc_Anydoc_nativeFormatFromBytes<'local>(
    mut unowned: EnvUnowned<'local>,
    _class: JClass<'local>,
    data: JByteArray<'local>,
) -> JObject<'local> {
    unowned
        .with_env(|env| -> Result<JObject, BindError> {
            let bytes = env.convert_byte_array(&data)?;
            match anydoc::Format::from_bytes(&bytes) {
                Some(format) => java_format(env, format),
                None => Ok(JObject::null()),
            }
        })
        .resolve::<ConvertPolicy>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_firecrawl_anydoc_Anydoc_nativeFormatFromExtension<'local>(
    mut unowned: EnvUnowned<'local>,
    _class: JClass<'local>,
    extension: JString<'local>,
) -> JObject<'local> {
    unowned
        .with_env(|env| -> Result<JObject, BindError> {
            let extension = extension.try_to_string(env)?;
            match anydoc::Format::from_extension(extension.trim_start_matches('.')) {
                Some(format) => java_format(env, format),
                None => Ok(JObject::null()),
            }
        })
        .resolve::<ConvertPolicy>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_firecrawl_anydoc_Anydoc_nativeFormatFromPath<'local>(
    mut unowned: EnvUnowned<'local>,
    _class: JClass<'local>,
    path: JString<'local>,
) -> JObject<'local> {
    unowned
        .with_env(|env| -> Result<JObject, BindError> {
            let path = path.try_to_string(env)?;
            match anydoc::Format::from_path(Path::new(&path)) {
                Some(format) => java_format(env, format),
                None => Ok(JObject::null()),
            }
        })
        .resolve::<ConvertPolicy>()
}
