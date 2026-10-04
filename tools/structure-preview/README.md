# Structure preview

Headless isometric renders of generated structures, with modded blocks (vanilla assets from the Minecraft
client jar, mod blockstates/models/textures from `src/main` and `src/generated`).

```
tools/structure-preview/preview.sh <out dir> ['mining_camp_.*|collapsed_adit']   # second arg: regex on names
```

1. `./gradlew runGameTestServer -PpreviewDir=… [-PpreviewOnly=regex]` runs `structure_preview_export`
   (`PreviewExport.java`): builds each plan in `Plans.all()` (and the collapsed adit in a hillside) on bare
   ground and writes `<name>.json` (palette plus block positions). Without `-PpreviewDir` the test does nothing.
2. `render.mjs` loads the JSON into [deepslate](https://github.com/misode/deepslate) in headless Chromium
   (Playwright, software WebGL) and writes `<name>_{south-west,south-east,north-east,north-west}.png`.

Needs Node 20+, `npm ci` in this folder (the script does it), and a Chromium reachable by `playwright-core`
(`PLAYWRIGHT_BROWSERS_PATH`). Blocks with a block entity renderer (beds, signs, chests) and entities are not drawn.
To add a structure, build it in `PreviewExport.export` and call `write`.
