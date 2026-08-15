#!/bin/sh
# Print the firecrawl/anydoc git tag this binding compiles against (e.g. v0.1.6).
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
tag=$(sed -n 's/.*anydoc = { git = "https:\/\/github.com\/firecrawl\/anydoc", tag = "\(v[^"]*\)".*/\1/p' "$ROOT/native/Cargo.toml" | head -n 1)
if [ -z "$tag" ]; then
  echo "error: could not read anydoc git tag from native/Cargo.toml" >&2
  exit 1
fi
printf '%s\n' "$tag"
