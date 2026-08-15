#!/bin/sh
# Print the Maven version for HEAD: the git tag with a leading v stripped
# (v0.1.8 → 0.1.8). VERSION= in the environment wins.
set -eu

if [ -n "${VERSION:-}" ]; then
  printf '%s\n' "$VERSION"
  exit 0
fi

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
tag=$(git -C "$ROOT" describe --tags --exact-match HEAD 2>/dev/null) || {
  echo "error: HEAD is not tagged; checkout a vX.Y.Z tag or set VERSION=" >&2
  exit 1
}

printf '%s\n' "${tag#v}"
