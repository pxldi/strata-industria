#!/usr/bin/env python3
"""
Writes the conveyor belt's models (tier 4 spec 13.1 and 21): the frame for each slope, the four render-only
top frames per slope the renderer swaps between, and the item model. Facing north; the blockstate turns them.
Run from the repository root: python3 tools/gen_belt_models.py
"""
import json
import os

ASSETS = "src/main/resources/assets/strataindustria"
NS = "strataindustria:block/"


def face(tex, uv, rotation=None):
    f = {"texture": "#" + tex, "uv": uv}
    if rotation:
        f["rotation"] = rotation
    return f


def box(frm, to, faces, rot=None):
    e = {"from": frm, "to": to, "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def frame_elements(rot):
    els = []
    # Rails: 2 px wide, 5 px high, with a brass hub plate on the outside where an axle drives the belt.
    for x0, side in ((0, "west"), (14, "east")):
        outer = side
        inner = "east" if side == "west" else "west"
        faces = {
            outer: face("side", [0, 11, 16, 16]),
            inner: face("side", [0, 11, 16, 16]),
            "up": face("rail", [0, 0, 2, 16]),
            "down": face("bed", [0, 0, 2, 16]),
            "north": face("side", [0, 11, 2, 16]),
            "south": face("side", [0, 11, 2, 16]),
        }
        els.append(box([x0, 0, 0], [x0 + 2, 5, 16], faces, rot))
        # Hub plate, set a hair proud of the rail's outer face.
        hx0, hx1 = (-0.01, 0.0) if side == "west" else (16.0, 16.01)
        hub = {outer: face("hub", [4, 4, 12, 12])}
        els.append(box([hx0 + (0.0 if side == "west" else 0.0), 0.5, 5], [hx1, 4.5, 11], hub, rot))
    # The bed under the leather, with a dark underside.
    bed = {
        "down": face("bed", [2, 0, 14, 16]),
        "north": face("bed", [2, 0, 14, 3]),
        "south": face("bed", [2, 0, 14, 3]),
        "up": face("bed", [2, 0, 14, 16]),
    }
    els.append(box([2, 0, 0], [14, 3, 16], bed, rot))
    return els


def top_elements(rot, frame):
    tex = "top"
    faces = {
        "up": face(tex, [2, 0, 14, 16]),
        "down": face("bed", [2, 0, 14, 16]),
        "north": face("edge", [2, 12, 14, 13]),
        "south": face("edge", [2, 12, 14, 13]),
        "east": face("edge", [0, 12, 16, 13]),
        "west": face("edge", [0, 12, 16, 13]),
    }
    return [box([2, 3, 0], [14, 4, 16], faces, rot)]


def rotation(slope):
    if slope == "flat":
        return None
    if slope == "up":
        # Back (z = 16) stays put, the front rises a block.
        return {"angle": 45, "axis": "x", "origin": [8, 0, 16], "rescale": True}
    return {"angle": -45, "axis": "x", "origin": [8, 0, 0], "rescale": True}


def write(path, data):
    full = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def textures(frame):
    return {
        "particle": NS + "conveyor_belt_side",
        "side": NS + "conveyor_belt_side",
        "rail": NS + "conveyor_belt_rail",
        "bed": NS + "conveyor_belt_bed",
        "hub": NS + "conveyor_belt_hub",
        "edge": NS + "conveyor_belt_edge",
        "top": NS + "conveyor_belt_top_%d" % frame,
    }


for slope in ("flat", "up", "down"):
    rot = rotation(slope)
    write("models/block/conveyor_%s.json" % slope, {"textures": textures(0), "elements": frame_elements(rot)})
    for frame in range(4):
        write("models/block/rotor/conveyor_top_%s_%d.json" % (slope, frame),
              {"textures": textures(frame), "elements": top_elements(rot, frame)})
        write("items/rotor/conveyor_top_%s_%d.json" % (slope, frame),
              {"model": {"type": "minecraft:model", "model": "strataindustria:block/rotor/conveyor_top_%s_%d" % (slope, frame)}})

# The item shows the flat belt with its top.
item = {"parent": "minecraft:block/block", "textures": textures(0),
        "elements": frame_elements(None) + top_elements(None, 0)}
# The inventory and hand transforms are the other hand-built machines' ones.
with open(os.path.join(ASSETS, "models/block/inserter_item.json")) as f:
    item["display"] = json.load(f)["display"]
write("models/block/conveyor_item.json", item)
print("belt models written")
