//! Map crate errors onto the Java exception hierarchy.

use jni::Env;
use jni::errors::ErrorPolicy;
use jni::objects::{JObject, JThrowable};
use jni::strings::JNIString;
use jni::{jni_sig, jni_str};

/// Failure inside a native method: a conversion error, or JNI itself.
#[derive(Debug)]
pub enum BindError {
    Convert(anydoc::ConvertError),
    Jni(jni::errors::Error),
}

impl From<anydoc::ConvertError> for BindError {
    fn from(error: anydoc::ConvertError) -> Self {
        BindError::Convert(error)
    }
}

impl From<jni::errors::Error> for BindError {
    fn from(error: jni::errors::Error) -> Self {
        BindError::Jni(error)
    }
}

impl std::fmt::Display for BindError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            BindError::Convert(error) => write!(f, "{error}"),
            BindError::Jni(error) => write!(f, "{error}"),
        }
    }
}

impl std::error::Error for BindError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            BindError::Convert(error) => Some(error),
            BindError::Jni(error) => Some(error),
        }
    }
}

/// Throw the Java exception that names the failure, then return a null/zero
/// default so the native method can return to the JVM.
pub struct ConvertPolicy;

impl<T: Default> ErrorPolicy<T, BindError> for ConvertPolicy {
    type Captures<'unowned_env_local: 'native_method, 'native_method> = ();

    fn on_error<'unowned_env_local: 'native_method, 'native_method>(
        env: &mut Env<'unowned_env_local>,
        _cap: &mut Self::Captures<'unowned_env_local, 'native_method>,
        err: BindError,
    ) -> jni::errors::Result<T> {
        if env.exception_check() {
            return Ok(T::default());
        }
        let _ = throw_bind_error(env, err);
        Ok(T::default())
    }

    fn on_panic<'unowned_env_local: 'native_method, 'native_method>(
        env: &mut Env<'unowned_env_local>,
        _cap: &mut Self::Captures<'unowned_env_local, 'native_method>,
        payload: Box<dyn std::any::Any + Send + 'static>,
    ) -> jni::errors::Result<T> {
        if env.exception_check() {
            return Ok(T::default());
        }
        let message = panic_message(payload);
        let _ = env.throw_new(jni_str!("java/lang/RuntimeException"), JNIString::new(message));
        Ok(T::default())
    }
}

fn panic_message(payload: Box<dyn std::any::Any + Send + 'static>) -> String {
    match payload.downcast::<String>() {
        Ok(message) => *message,
        Err(payload) => match payload.downcast::<&'static str>() {
            Ok(message) => (*message).to_string(),
            Err(_) => "panic in anydoc native method".into(),
        },
    }
}

fn throw_bind_error(env: &mut Env<'_>, error: BindError) -> jni::errors::Result<()> {
    match error {
        BindError::Jni(error) => {
            let _ = env.throw_new(
                jni_str!("java/lang/RuntimeException"),
                JNIString::new(error.to_string()),
            );
        }
        BindError::Convert(anydoc::ConvertError::Io(io)) => throw_io(env, io)?,
        BindError::Convert(error) => throw_convert(env, error)?,
    }
    Ok(())
}

fn throw_io(env: &mut Env<'_>, error: std::io::Error) -> jni::errors::Result<()> {
    let message = JNIString::new(error.to_string());
    match error.kind() {
        std::io::ErrorKind::NotFound => {
            let _ = env.throw_new(jni_str!("java/nio/file/NoSuchFileException"), message);
        }
        std::io::ErrorKind::PermissionDenied => {
            let _ = env.throw_new(jni_str!("java/nio/file/AccessDeniedException"), message);
        }
        _ => {
            let _ = env.throw_new(jni_str!("java/io/IOException"), message);
        }
    }
    Ok(())
}

fn throw_convert(env: &mut Env<'_>, error: anydoc::ConvertError) -> jni::errors::Result<()> {
    let message = error.to_string();
    match &error {
        anydoc::ConvertError::Unsupported(_) => {
            let _ = env.throw_new(
                jni_str!("dev/firecrawl/anydoc/UnsupportedException"),
                JNIString::new(message),
            );
        }
        anydoc::ConvertError::Encrypted => {
            let _ = env.throw_new(
                jni_str!("dev/firecrawl/anydoc/EncryptedException"),
                JNIString::new(message),
            );
        }
        anydoc::ConvertError::Malformed { part, .. } => {
            throw_with_optional_string(
                env,
                jni_str!("dev/firecrawl/anydoc/MalformedException"),
                &message,
                part.as_deref(),
            )?;
        }
        anydoc::ConvertError::MissingPart { part } => {
            throw_with_string(
                env,
                jni_str!("dev/firecrawl/anydoc/MissingPartException"),
                &message,
                part,
            )?;
        }
        anydoc::ConvertError::ResourceLimit { limit, .. } => {
            throw_with_string(
                env,
                jni_str!("dev/firecrawl/anydoc/ResourceLimitException"),
                &message,
                limit,
            )?;
        }
        anydoc::ConvertError::Io(_) => unreachable!("Io is handled by throw_io"),
        _ => {
            let _ = env.throw_new(
                jni_str!("dev/firecrawl/anydoc/ConvertException"),
                JNIString::new(message),
            );
        }
    }
    Ok(())
}

fn throw_with_string(
    env: &mut Env<'_>,
    class: &jni::strings::JNIStr,
    message: &str,
    extra: &str,
) -> jni::errors::Result<()> {
    let message = jni::objects::JString::from_str(env, message)?;
    let extra = jni::objects::JString::from_str(env, extra)?;
    let thrown = env.new_object(
        class,
        jni_sig!((java.lang.String, java.lang.String) -> void),
        &[
            jni::objects::JValue::Object(message.as_ref()),
            jni::objects::JValue::Object(extra.as_ref()),
        ],
    )?;
    throw_object(env, thrown)
}

fn throw_with_optional_string(
    env: &mut Env<'_>,
    class: &jni::strings::JNIStr,
    message: &str,
    extra: Option<&str>,
) -> jni::errors::Result<()> {
    let message = jni::objects::JString::from_str(env, message)?;
    let extra = match extra {
        Some(value) => jni::objects::JString::from_str(env, value)?.into(),
        None => JObject::null(),
    };
    let thrown = env.new_object(
        class,
        jni_sig!((java.lang.String, java.lang.String) -> void),
        &[jni::objects::JValue::Object(message.as_ref()), jni::objects::JValue::Object(&extra)],
    )?;
    throw_object(env, thrown)
}

fn throw_object(env: &mut Env<'_>, thrown: JObject<'_>) -> jni::errors::Result<()> {
    let thrown = env.cast_local::<JThrowable>(thrown)?;
    let _ = env.throw(thrown);
    Ok(())
}
