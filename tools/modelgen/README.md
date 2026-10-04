# modelgen

Box models as Python scripts: reviewable, rerunnable, exact UVs at 16 px per block, no dependencies.

```
python3 tools/modelgen/crucible.py [--bbmodel]     # rewrites models/block/crucible.json (and a Blockbench file)
```

`modelgen.py` gives `Model(name, textures)` with `box()`, `hollow()` (open-top ring), `ring_stack()`, `mirror_x()`,
automatic culling of hidden faces, rotation limited to vanilla's steps, and `write("block"|"item")`.
One script per family (`crucible.py` is the example). Rules: STYLE_GUIDE section 11.

In-world 3D items (flat sprite in the GUI, model elsewhere): `InWorld3d.apply(itemModels, item, modelId)` in datagen.

Check a model next to vanilla (needs `tools/structure-preview/preview.sh` run once):
`python3 tools/structure-preview/models.py <out> crucible [--no-refs]` and `--frames <prefix>` for `<prefix>_f0..fN` poses.
Record it in the real client: `tools/capture/` (see its README).
