#!/bin/sh
# Build the JNI library for one or more platforms and stage it under native/dist/.
#
# Linux GNU targets are linked with Zig against glibc 2.17 (CentOS 7 /
# manylinux2014). Other targets use cargo zigbuild when Zig can handle them,
# otherwise a host cargo build.
#
# Usage:
#   scripts/build-native.sh              # this machine
#   scripts/build-native.sh linux-x86_64
#   scripts/build-native.sh --all        # every target this host can build
#   scripts/build-native.sh --list
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
NATIVE="$ROOT/native"
DIST="$NATIVE/dist"
GLIBC=2.17
LIB=anydoc_java

usage() {
  cat <<EOF
usage: scripts/build-native.sh [--all | --list | <classifier> ...]

Classifiers:
  linux-x86_64         x86_64-unknown-linux-gnu   (glibc $GLIBC via Zig)
  linux-aarch64        aarch64-unknown-linux-gnu  (glibc $GLIBC via Zig)
  linux-x86_64-musl    x86_64-unknown-linux-musl
  linux-aarch64-musl   aarch64-unknown-linux-musl
  macos-x86_64         x86_64-apple-darwin
  macos-aarch64        aarch64-apple-darwin
  windows-x86_64       x86_64-pc-windows-msvc (or windows-gnu via Zig)
EOF
}

rust_target() {
  case $1 in
    linux-x86_64) echo x86_64-unknown-linux-gnu ;;
    linux-aarch64) echo aarch64-unknown-linux-gnu ;;
    linux-x86_64-musl) echo x86_64-unknown-linux-musl ;;
    linux-aarch64-musl) echo aarch64-unknown-linux-musl ;;
    macos-x86_64) echo x86_64-apple-darwin ;;
    macos-aarch64) echo aarch64-apple-darwin ;;
    windows-x86_64) echo x86_64-pc-windows-msvc ;;
    *) echo "unknown classifier: $1" >&2; return 1 ;;
  esac
}

lib_name() {
  case $1 in
    windows-*) echo "${LIB}.dll" ;;
    macos-*) echo "lib${LIB}.dylib" ;;
    *) echo "lib${LIB}.so" ;;
  esac
}

host_os() {
  case $(uname -s) in
    Linux) echo linux ;;
    Darwin) echo macos ;;
    MINGW*|MSYS*|CYGWIN*) echo windows ;;
    *) uname -s | tr '[:upper:]' '[:lower:]' ;;
  esac
}

host_arch() {
  case $(uname -m) in
    x86_64|amd64) echo x86_64 ;;
    aarch64|arm64) echo aarch64 ;;
    *) uname -m ;;
  esac
}

host_classifier() {
  os=$(host_os)
  arch=$(host_arch)
  if [ "$os" = linux ]; then
    if [ -f "/lib/ld-musl-${arch}.so.1" ]; then
      echo "linux-${arch}-musl"
      return
    fi
  fi
  echo "${os}-${arch}"
}

is_linux_gnu() {
  case $1 in linux-x86_64|linux-aarch64) return 0 ;; *) return 1 ;; esac
}

have() { command -v "$1" >/dev/null 2>&1; }

ensure_target() {
  triple=$1
  if rustup target list --installed 2>/dev/null | grep -qx "$triple"; then
    return
  fi
  rustup target add "$triple"
}

max_glibc() {
  so=$1
  if have objdump; then
    objdump -T "$so" 2>/dev/null | sed -n 's/.*GLIBC_\([0-9.]*\).*/\1/p' | sort -t. -k1,1n -k2,2n -k3,3n | tail -n 1
  elif have readelf; then
    readelf -V "$so" 2>/dev/null | sed -n 's/.*GLIBC_\([0-9.]*\).*/\1/p' | sort -t. -k1,1n -k2,2n -k3,3n | tail -n 1
  else
    echo ""
  fi
}

version_gt() {
  # true if $1 > $2 (dotted numeric)
  awk -v a="$1" -v b="$2" 'BEGIN {
    n=split(a,A,"."); m=split(b,B,".");
    max=n>m?n:m;
    for (i=1;i<=max;i++) {
      x=i<=n?A[i]+0:0; y=i<=m?B[i]+0:0;
      if (x>y) exit 0;
      if (x<y) exit 1;
    }
    exit 1
  }'
}

check_glibc() {
  so=$1
  found=$(max_glibc "$so")
  if [ -z "$found" ]; then
    echo "warning: could not read GLIBC symbols from $so" >&2
    return
  fi
  echo "  GLIBC max $found (required <= $GLIBC)"
  if version_gt "$found" "$GLIBC"; then
    echo "error: $so requires GLIBC_$found; CentOS 7 needs <= $GLIBC. Use Zig (cargo zigbuild)." >&2
    return 1
  fi
}

build_one() {
  classifier=$1
  triple=$(rust_target "$classifier")
  name=$(lib_name "$classifier")
  echo "==> $classifier ($triple)"

  ensure_target "$triple"

  zig_triple=$triple
  if is_linux_gnu "$classifier"; then
    zig_triple="${triple}.${GLIBC}"
  fi

  out=""
  if have cargo-zigbuild && have zig; then
    # Zig links Linux GNU against glibc 2.17; musl and windows-gnu also work.
    # Skip MSVC (needs the Windows SDK) and macOS unless the host is Darwin
    # (cross-linking macOS needs an SDK we do not bundle).
    use_zig=0
    case $classifier in
      linux-*) use_zig=1 ;;
      windows-x86_64)
        if [ "$(host_os)" != windows ]; then
          zig_triple=x86_64-pc-windows-gnu
          triple=x86_64-pc-windows-gnu
          use_zig=1
        fi
        ;;
      macos-*)
        if [ "$(host_os)" = macos ]; then
          use_zig=1
        fi
        ;;
    esac
    if [ "$use_zig" -eq 1 ]; then
      cargo zigbuild --release --manifest-path "$NATIVE/Cargo.toml" --target "$zig_triple"
      out="$NATIVE/target/${triple}/release/${name}"
    fi
  fi

  if [ -z "$out" ]; then
    same_os=0
    case $classifier in
      "$(host_os)"-*) same_os=1 ;;
    esac
    if [ "$same_os" -eq 1 ]; then
      if [ "$(host_classifier)" != "$classifier" ]; then
        echo "  cargo --target $triple (same OS, no Zig)"
      else
        echo "warning: Zig not available; host cargo build may require GLIBC newer than $GLIBC" >&2
      fi
      cargo build --release --manifest-path "$NATIVE/Cargo.toml" --target "$triple"
      out="$NATIVE/target/${triple}/release/${name}"
    else
      echo "error: cannot build $classifier on $(host_classifier) without Zig (install zig and cargo-zigbuild)" >&2
      return 1
    fi
  fi

  if [ ! -f "$out" ]; then
    echo "error: expected $out" >&2
    return 1
  fi

  if is_linux_gnu "$classifier"; then
    check_glibc "$out"
  fi

  dest="$DIST/$classifier/$name"
  mkdir -p "$(dirname "$dest")"
  cp -f "$out" "$dest"
  echo "  staged $dest"
}

host=$(host_classifier)
targets=""
if [ "$#" -eq 0 ]; then
  targets=$host
else
  for arg in "$@"; do
    case $arg in
      -h|--help) usage; exit 0 ;;
      --list) usage; exit 0 ;;
      --all)
        case $(host_os) in
          linux) targets="linux-x86_64 linux-aarch64 linux-x86_64-musl linux-aarch64-musl" ;;
          macos) targets="macos-x86_64 macos-aarch64" ;;
          windows) targets="windows-x86_64" ;;
          *) targets=$host ;;
        esac
        if have cargo-zigbuild && have zig && [ "$(host_os)" = linux ]; then
          targets="$targets windows-x86_64"
        fi
        ;;
      linux-*|macos-*|windows-*)
        targets="${targets:+$targets }$arg"
        ;;
      *)
        echo "unknown argument: $arg" >&2
        usage >&2
        exit 2
        ;;
    esac
  done
fi

for c in $targets; do
  build_one "$c"
done
