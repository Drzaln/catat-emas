#!/usr/bin/env bash
# Bump versi -> build release -> commit -> push main -> tag vX -> push tag.
# Push tag memicu GitHub Actions yang membuat GitHub Release (dipantau fitur cek update di app).
#
#   ./scripts/ship.sh            # bump patch (default)
#   ./scripts/ship.sh minor
#   ./scripts/ship.sh major
#   ./scripts/ship.sh 1.2.0      # versi eksplisit
set -euo pipefail

cd "$(dirname "$0")/.."

GRADLE_FILE="app/build.gradle.kts"

current_code=$(grep -oE 'versionCode = [0-9]+' "$GRADLE_FILE" | grep -oE '[0-9]+' | head -1)
current_name=$(grep -oE 'versionName = "[^"]+"' "$GRADLE_FILE" | grep -oE '"[^"]+"' | tr -d '"' | head -1)

if [ -z "${current_code:-}" ] || [ -z "${current_name:-}" ]; then
  echo "Gagal membaca versionCode/versionName dari $GRADLE_FILE" >&2
  exit 1
fi

bump="${1:-patch}"
case "$bump" in
  major|minor|patch)
    major="${current_name%%.*}"
    rest="${current_name#*.}"
    minor="${rest%%.*}"
    patch="${rest##*.}"
    case "$bump" in
      major) major=$((major + 1)); minor=0; patch=0 ;;
      minor) minor=$((minor + 1)); patch=0 ;;
      patch) patch=$((patch + 1)) ;;
    esac
    new_name="$major.$minor.$patch"
    ;;
  *)
    if echo "$bump" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
      new_name="$bump"
    else
      echo "usage: ./scripts/ship.sh [patch|minor|major|<x.y.z>]" >&2
      exit 1
    fi
    ;;
esac

new_code=$((current_code + 1))

# sed -i portable (BSD/macOS & GNU/Linux)
sed_inplace() {
  local expr="$1" file="$2" tmp
  tmp="$(mktemp)"
  sed -e "$expr" "$file" > "$tmp" && mv "$tmp" "$file"
}

sed_inplace "s/versionCode = $current_code/versionCode = $new_code/" "$GRADLE_FILE"
sed_inplace "s/versionName = \"$current_name\"/versionName = \"$new_name\"/" "$GRADLE_FILE"

echo "Bump v$current_name (code $current_code) -> v$new_name (code $new_code)"

if grep -q 'CHANGE_ME/catat-emas' gradle.properties 2>/dev/null; then
  echo "PERINGATAN: gradle.properties masih 'github.repo=CHANGE_ME/catat-emas'." >&2
  echo "            Fitur cek update di APK tidak akan menemukan release." >&2
fi

# build lokal dulu sebagai sanity check (butuh JDK 17 + Android SDK)
./gradlew assembleRelease --no-daemon

git add -A
if git diff --cached --quiet; then
  echo "Tidak ada perubahan untuk di-commit." >&2
else
  git commit -m "Release v$new_name (versionCode $new_code)"
fi

git push origin HEAD
git tag -f "v$new_name"
git push origin "v$new_name"

echo "Shipped v$new_name (versionCode $new_code). CI akan build + buat GitHub Release."
