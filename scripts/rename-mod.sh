#!/usr/bin/env bash
# Renames the mod in one go: mod id, Java package, main class, display name and asset folder.
#
#   scripts/rename-mod.sh <new_mod_id> "<New Display Name>" [NewMainClassName]
#
# Example: scripts/rename-mod.sh kindled "Kindled" Kindled
set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "usage: $0 <new_mod_id> \"<New Display Name>\" [NewMainClassName]" >&2
    exit 1
fi

NEW_ID="$1"
NEW_NAME="$2"
NEW_CLASS="${3:-$(tr '[:lower:]' '[:upper:]' <<< "${NEW_ID:0:1}")${NEW_ID:1}}"

if ! [[ "$NEW_ID" =~ ^[a-z][a-z0-9_]{1,63}$ ]]; then
    echo "mod id must match [a-z][a-z0-9_]{1,63}" >&2
    exit 1
fi

cd "$(dirname "$0")/.."

OLD_ID="$(sed -n 's/^mod_id=//p' gradle.properties)"
OLD_NAME="$(sed -n 's/^mod_name=//p' gradle.properties)"
OLD_GROUP="$(sed -n 's/^mod_group_id=//p' gradle.properties)"
OLD_CLASS="$(grep -l "@Mod(" -r src/main/java | grep -v '/client/' | head -n1 | xargs basename | sed 's/\.java$//')"
NEW_GROUP="dev.${NEW_ID}"

echo "Renaming ${OLD_ID} (${OLD_NAME}, ${OLD_CLASS}) -> ${NEW_ID} (${NEW_NAME}, ${NEW_CLASS})"

OLD_PKG_DIR="src/main/java/${OLD_GROUP//.//}"
NEW_PKG_DIR="src/main/java/${NEW_GROUP//.//}"
mkdir -p "$(dirname "$NEW_PKG_DIR")"
git mv "$OLD_PKG_DIR" "$NEW_PKG_DIR"
git mv "$NEW_PKG_DIR/${OLD_CLASS}.java" "$NEW_PKG_DIR/${NEW_CLASS}.java"
git mv "$NEW_PKG_DIR/client/${OLD_CLASS}Client.java" "$NEW_PKG_DIR/client/${NEW_CLASS}Client.java"

for dir in src/main/resources/assets src/main/resources/data src/generated/resources/assets src/generated/resources/data; do
    if [[ -d "$dir/$OLD_ID" ]]; then
        git mv "$dir/$OLD_ID" "$dir/$NEW_ID"
    fi
done

# Text replacements across tracked text files.
git ls-files -z | xargs -0 grep -Il . 2>/dev/null | while read -r f; do
    sed -i \
        -e "s/${OLD_GROUP//./\\.}/${NEW_GROUP}/g" \
        -e "s/${OLD_NAME}/${NEW_NAME}/g" \
        -e "s/\b${OLD_CLASS}Client\b/${NEW_CLASS}Client/g" \
        -e "s/\b${OLD_CLASS}\b/${NEW_CLASS}/g" \
        -e "s/\b${OLD_ID}\b/${NEW_ID}/g" \
        "$f"
done

echo "Done. Run ./gradlew runData build to verify."
