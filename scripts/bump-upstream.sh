#!/bin/sh
# Check or apply an anydoc (firecrawl/anydoc) release bump.
#
# Usage:
#   scripts/bump-upstream.sh current
#   scripts/bump-upstream.sh latest
#   scripts/bump-upstream.sh check     # exit 0 if current, 10 if a bump is available
#   scripts/bump-upstream.sh apply     # rewrite Cargo.toml + Cargo.lock to latest
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
CARGO="$ROOT/native/Cargo.toml"

current_tag() {
  sh "$ROOT/scripts/anydoc-tag.sh"
}

latest_tag() {
  if command -v gh >/dev/null 2>&1; then
    tag=$(gh api repos/firecrawl/anydoc/releases/latest --jq .tag_name 2>/dev/null || true)
    if [ -n "$tag" ] && [ "$tag" != "null" ]; then
      printf '%s\n' "$tag"
      return
    fi
  fi
  # Fallback: highest v* tag (works without a GitHub "latest" release).
  git ls-remote --tags --refs https://github.com/firecrawl/anydoc.git 'v*' \
    | awk '{print $2}' \
    | sed 's|refs/tags/||' \
    | grep -E '^v[0-9]+(\.[0-9]+)*$' \
    | sort -t. -k1,1V -k2,2V -k3,3V \
    | tail -n 1
}

bare() {
  printf '%s' "$1" | sed 's/^v//'
}

newer_than() {
  # true if $1 > $2 (dotted numeric, no leading v)
  [ "$(printf '%s\n%s\n' "$2" "$1" | sort -t. -k1,1n -k2,2n -k3,3n | tail -n 1)" = "$1" ] && [ "$1" != "$2" ]
}

apply_tag() {
  tag=$1
  case $tag in
    v*) ;;
    *) tag="v$tag" ;;
  esac
  tmp=$(mktemp)
  sed "s|anydoc = { git = \"https://github.com/firecrawl/anydoc\", tag = \"v[^\"]*\" }|anydoc = { git = \"https://github.com/firecrawl/anydoc\", tag = \"$tag\" }|" "$CARGO" > "$tmp"
  mv "$tmp" "$CARGO"
  if ! grep -q "tag = \"$tag\"" "$CARGO"; then
    echo "error: failed to write $tag into native/Cargo.toml" >&2
    exit 1
  fi
  cargo update -p anydoc --manifest-path "$ROOT/native/Cargo.toml"
  echo "pinned anydoc $tag"
}

cmd=${1:-check}
case $cmd in
  current)
    current_tag
    ;;
  latest)
    latest=$(latest_tag)
    if [ -z "$latest" ]; then
      echo "error: could not determine the latest firecrawl/anydoc tag" >&2
      exit 1
    fi
    printf '%s\n' "$latest"
    ;;
  check)
    current=$(current_tag)
    latest=$(latest_tag)
    if [ -z "$latest" ]; then
      echo "error: could not determine the latest firecrawl/anydoc tag" >&2
      exit 1
    fi
    echo "current $current"
    echo "latest  $latest"
    if newer_than "$(bare "$latest")" "$(bare "$current")"; then
      echo "bump available"
      exit 10
    fi
    echo "up to date"
    ;;
  apply)
    current=$(current_tag)
    latest=${2:-$(latest_tag)}
    if [ -z "$latest" ]; then
      echo "error: could not determine the latest firecrawl/anydoc tag" >&2
      exit 1
    fi
    if [ "$current" = "$latest" ]; then
      echo "already on $current"
      exit 0
    fi
    apply_tag "$latest"
    ;;
  *)
    echo "usage: scripts/bump-upstream.sh current|latest|check|apply [tag]" >&2
    exit 2
    ;;
esac
