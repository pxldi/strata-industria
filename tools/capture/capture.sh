#!/usr/bin/env bash
# Records a scene in the real game client on a virtual screen (Xvfb + Mesa lavapipe Vulkan) and builds a GIF.
# usage: tools/capture/capture.sh <scene file> <out dir> [fps]   (scenes: tools/capture/scenes/*.scene, format in CaptureClient)
set -euo pipefail
cd "$(dirname "$0")/../.."
SCENE=$(realpath "${1:?scene file}"); OUT=$(realpath -m "${2:?out dir}"); FPS=${3:-20}
CROP=${CROP:-}   # optional ffmpeg crop, e.g. CROP="crop=400:300:230:200,"
command -v Xvfb >/dev/null || { echo "Xvfb missing (apt-get install -y xvfb)"; exit 1; }
[ -e /usr/share/vulkan/icd.d/lvp_icd.json ] || { echo "run: apt-get install -y mesa-vulkan-drivers"; exit 1; }
mkdir -p "$OUT" run; rm -rf "$OUT/screenshots" "$OUT"/*.png "$OUT"/*.gif
# skip the first-launch narrator screen
touch run/options.txt
if grep -q '^onboardAccessibility:' run/options.txt; then sed -i 's/^onboardAccessibility:.*/onboardAccessibility:false/' run/options.txt
else echo 'onboardAccessibility:false' >> run/options.txt; fi
pgrep -x Xvfb >/dev/null || { Xvfb :99 -screen 0 1280x720x24 +extension GLX +render -noreset >/dev/null 2>&1 & sleep 2; }
DISPLAY=:99 VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.json \
  ./gradlew runClient --console=plain -Pcapture="$SCENE" -PcaptureOut="$OUT" 2>&1 | tail -40
mv "$OUT"/screenshots/*.png "$OUT"/ 2>/dev/null && rmdir "$OUT/screenshots" || true
# one GIF per frame series (name_000.png ...)
for first in "$OUT"/*_000.png; do
  [ -e "$first" ] || continue
  base=$(basename "$first" _000.png)
  ffmpeg -y -loglevel error -framerate "$FPS" -i "$OUT/${base}_%03d.png" \
    -vf "${CROP}scale=640:-1:flags=neighbor,split[a][b];[a]palettegen[p];[b][p]paletteuse" "$OUT/$base.gif"
  echo "wrote $OUT/$base.gif"
done
