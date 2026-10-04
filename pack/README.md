# Modpack

`mods.json` lists the companion mods and exact versions (Modrinth, NeoForge 26.3).
`mods.lock.json` pins the resolved files (URL, hashes, size) and is generated; do not edit it.

```
python3 pack/build_mrpack.py lock                      # re-resolve pins after editing mods.json
python3 pack/build_mrpack.py build --mod-jar build/libs/<mod>.jar
```

The result is `build/Strata-Industria-<version>.mrpack`. Companion mods are download links, so the
file holds only the index and the mod jar. CI builds it on every run (artifact `modpack`) and
publishes the newest build from `main` to the `modpack-latest` release. Import it in Prism or the
Modrinth app.

## Included

Sodium 0.9.2, Iris, FerriteCore, ImmediatelyFast, Distant Horizons, Sound Physics Remastered, Jade,
JEI (+ MezzConfig), Xaero's Minimap and World Map, Mouse Tweaks.

## Left out

- EMI: no NeoForge 26.x build; JEI is used instead.
- Sodium 0.9.3-alpha: alpha; 0.9.2 is pinned.
- Embeddium, Oculus: not needed, Sodium and Iris are native on NeoForge.
- ModernFix: official build has no 26.3 release; the unofficial fork is not trusted yet.
- Entity Culling, Presence Footsteps: no 26.3 NeoForge build.
- Terrain, biome and tech mods: they conflict with our worldgen and progression.

## Known risks (untested in game)

- Distant Horizons works with Sodium and Iris, but LODs with shader packs are the most likely
  source of glitches. Disable DH or the shader pack first when debugging rendering.
- Only beta builds exist for JEI and Sound Physics Remastered on 26.3.
