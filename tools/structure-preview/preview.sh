#!/usr/bin/env bash
# Structure preview: build structures in a GameTest world, export block lists, render isometric PNGs from 4 sides.
# usage: tools/structure-preview/preview.sh <out dir> [name regex, e.g. 'mining_camp_.*|collapsed_adit']
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=$(realpath -m "${1:?out dir}"); ONLY=${2:-.*}
TMP=build/structure-preview; mkdir -p "$TMP/json"; rm -f "$TMP"/json/*.json
./gradlew runGameTestServer -PpreviewDir="$PWD/$TMP/json" -PpreviewOnly="$ONLY" --console=plain
# vanilla assets from the client jar the build already downloaded
if [ ! -d "$TMP/vanilla/minecraft" ]; then
  JAR=$(find ~/.gradle/caches/neoformruntime/artifacts -name 'minecraft_*_client.jar' 2>/dev/null | head -1)
  [ -n "$JAR" ] || { echo "no Minecraft client jar found"; exit 1; }
  mkdir -p "$TMP/vanilla"; (cd "$TMP/vanilla" && unzip -qo "$JAR" 'assets/minecraft/blockstates/*' 'assets/minecraft/models/*' 'assets/minecraft/textures/block/*' 'assets/minecraft/textures/item/*' && mv assets/minecraft . && rmdir assets)
fi
cd tools/structure-preview; [ -d node_modules ] || npm ci
node render.mjs --in "../../$TMP/json" --out "$OUT" --only "$ONLY" \
  --assets "../../$TMP/vanilla" --assets ../../src/main/resources/assets --assets ../../src/generated/resources/assets
