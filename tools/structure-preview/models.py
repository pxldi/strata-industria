#!/usr/bin/env python3
"""Model sheets: render block models (mod or vanilla) from 4 angles next to vanilla reference blocks.

usage: models.py <out dir> <model> [<model> ...] [--frames prefix] [--no-refs]
  <model>          a model id like strataindustria:block/crucible (or just crucible = strataindustria:block/crucible)
  --frames prefix  adds prefix_f0, prefix_f1, ... (every existing model with that prefix) as one row, for animation poses
Writes <out>/<name>_<angle>.png via render.mjs plus <out>/<name>_sheet.png (4 angles side by side, needs ImageMagick).
Run preview.sh once first (it unpacks the vanilla assets into build/structure-preview/vanilla).
"""
import json, os, subprocess, sys, glob, shutil

here = os.path.dirname(os.path.abspath(__file__))
root = os.path.normpath(os.path.join(here, "..", ".."))
tmp = os.path.join(root, "build", "structure-preview")
REFS = ["minecraft:anvil[facing=north]", "minecraft:campfire[facing=north,lit=false,signal_fire=false]",
        "minecraft:lantern[hanging=false]", "minecraft:cauldron"]

args = sys.argv[1:]
if len(args) < 2: sys.exit(__doc__)
out = os.path.abspath(args.pop(0))
refs = "--no-refs" not in args
args = [a for a in args if a != "--no-refs"]
models = []
while args:
    a = args.pop(0)
    if a == "--frames":
        prefix = args.pop(0)
        ns = prefix.split(":")[0] if ":" in prefix else "strataindustria"
        rel = prefix.split(":")[-1]
        rel = rel if "/" in rel else "block/" + rel
        found = sorted(glob.glob(os.path.join(root, "src/main/resources/assets", ns, "models", rel + "_f*.json")),
                       key=lambda p: int(p.rsplit("_f", 1)[1][:-5]))
        models.append([f"{ns}:{rel}_f{os.path.basename(p).rsplit('_f',1)[1][:-5]}" for p in found])
    else:
        models.append([a if ":" in a else "strataindustria:" + (a if "/" in a else "block/" + a)])

vanilla = os.path.join(tmp, "vanilla")
if not os.path.isdir(os.path.join(vanilla, "minecraft")): sys.exit("run tools/structure-preview/preview.sh once first (unpacks vanilla assets)")
fake = os.path.join(tmp, "models-assets"); shutil.rmtree(fake, ignore_errors=True)
jsondir = os.path.join(tmp, "models-json"); shutil.rmtree(jsondir, ignore_errors=True); os.makedirs(jsondir)
os.makedirs(os.path.join(fake, "preview", "blockstates"))
for group in models:
    palette, blocks = [], []
    for i, model in enumerate(group):
        key = "model_" + model.replace(":", "_").replace("/", "_")
        with open(os.path.join(fake, "preview", "blockstates", key + ".json"), "w") as f:
            json.dump({"variants": {"": {"model": model}}}, f)
        palette.append("preview:" + key); blocks.append([i * 2, 0, 0, len(palette) - 1])
    n = len(group)
    if refs and n == 1:
        for j, r in enumerate(REFS):
            palette.append(r); blocks.append([2 + j * 2, 0, 0, len(palette) - 1])
    name = group[0].split(":")[1].replace("/", "_")
    if n > 1: name = name.rsplit("_f", 1)[0] + "_frames"
    # a floor strip under everything makes scale and height readable
    json.dump({"size": [max(b[0] for b in blocks) + 1, 1, 1], "palette": palette, "blocks": blocks},
              open(os.path.join(jsondir, name + ".json"), "w"))
os.chdir(here)
if not os.path.isdir("node_modules"): subprocess.run(["npm", "ci"], check=True)
subprocess.run(["node", "render.mjs", "--in", jsondir, "--out", out, "--assets", vanilla,
                "--assets", os.path.join(root, "src/main/resources/assets"),
                "--assets", os.path.join(root, "src/generated/resources/assets"), "--assets", fake, "--size", "900"], check=True)
for j in glob.glob(os.path.join(jsondir, "*.json")):
    nm = os.path.basename(j)[:-5]
    shots = [os.path.join(out, f"{nm}_{a}.png") for a in ("south-west", "south-east", "north-east", "north-west")]
    if shutil.which("convert") and all(os.path.exists(s) for s in shots):
        # close-up sheets (no reference blocks) are cropped around the model and enlarged without smoothing
        crop = [] if len(models) and any(len(g) > 1 for g in models) or refs else ["-gravity", "center", "-crop", "560x560+0+0", "+repage"]
        subprocess.run(["convert", *shots, *crop, "+append", os.path.join(out, nm + "_sheet.png")] if not crop else
                       ["convert", *[x for s_ in shots for x in ("(", s_, *crop, ")")], "+append", "-filter", "point", "-resize", "150%", os.path.join(out, nm + "_sheet.png")], check=True)
