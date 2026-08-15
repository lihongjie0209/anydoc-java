#!/bin/sh
# Assemble the fat JAR (already produced by Maven if native/dist is complete)
# and one classifier JAR per staged platform.
#
# Usage (after natives are in native/dist):
#   mvn -DskipNative -DskipTests package
#   scripts/package-jars.sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
DIST="$ROOT/native/dist"
CLASSES="$ROOT/target/classes"
OUT="$ROOT/target"
GROUP_ID=io.github.lihongjie0209
ARTIFACT_ID=anydoc

if [ ! -d "$CLASSES/dev" ]; then
  echo "error: $CLASSES is missing compiled classes; run mvn -DskipNative compile first" >&2
  exit 1
fi

VERSION=$(awk '
  /<parent>/ { skip=1 }
  /<\/parent>/ { skip=0 }
  !skip && /<version>/ {
    sub(/.*<version>/, ""); sub(/<\/version>.*/, ""); print; exit
  }
' "$ROOT/pom.xml")

if [ -z "$VERSION" ]; then
  echo "error: could not read version from pom.xml" >&2
  exit 1
fi

echo "version $VERSION"

copy_classes() {
  dest=$1
  mkdir -p "$dest"
  (cd "$CLASSES" && find . -path ./native -prune -o -type f -print) | while IFS= read -r f; do
    mkdir -p "$dest/$(dirname "$f")"
    cp -f "$CLASSES/$f" "$dest/$f"
  done
}

# Per-platform classifier JARs: Java classes + one native tree.
if [ -d "$DIST" ]; then
  for dir in "$DIST"/*; do
    [ -d "$dir" ] || continue
    classifier=$(basename "$dir")
    stage=$(mktemp -d)
    copy_classes "$stage"
    mkdir -p "$stage/native/$classifier"
    cp -a "$dir"/. "$stage/native/$classifier/"
    jar="$OUT/${ARTIFACT_ID}-${VERSION}-${classifier}.jar"
    jar cf "$jar" -C "$stage" .
    rm -rf "$stage"
    echo "  $jar"
  done
fi

fat="$OUT/${ARTIFACT_ID}-${VERSION}.jar"
if [ -f "$fat" ]; then
  echo "  $fat (fat, all staged natives)"
else
  echo "warning: $fat not found; run mvn -DskipNative -DskipTests package after staging natives" >&2
fi

# A small properties file CI can source.
cat > "$OUT/maven-coordinates.properties" <<EOF
groupId=$GROUP_ID
artifactId=$ARTIFACT_ID
version=$VERSION
EOF
