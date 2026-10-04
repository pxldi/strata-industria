#!/usr/bin/env python3
"""
Writes the conveyor belt's models (tier 4 spec 13.1 and 21): the frame for each slope, the four render-only
top frames per slope the renderer swaps between, and the item model. Also the belt diverter's (spec 13.2): a
flat frame with its push-side rail lowered to a brass lip, with and without the filter tag, and the two
render-only paddles. Facing north; the blockstate turns them.
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


# ---- Belt diverter (spec 13.2). Facing north; "right" pushes east, "left" west. The push-side rail is
# lowered to a brass lip with two posts so items can leave, and the paddle (a render-only rotor model)
# hinges on the opposite rail at z = 2.5 and sweeps across.
DIV = {"particle": NS + "belt_diverter_side", "lip": NS + "belt_diverter_side", "tag": NS + "inserter_tag"}


def diverter_elements(push):
    open_x0 = 14 if push == "right" else 0
    keep = []
    for el in frame_elements(None):
        f, t = el["from"], el["to"]
        is_rail = t[1] == 5 and t[0] - f[0] == 2 and f[0] == open_x0
        if not is_rail:
            keep.append(el)
    outer = "east" if push == "right" else "west"
    inner = "west" if push == "right" else "east"
    lip_faces = {
        outer: face("lip", [0, 1, 16, 4]),
        inner: face("lip", [0, 1, 16, 4]),
        "up": face("lip", [0, 0, 2, 16]),
        "north": face("lip", [0, 1, 2, 4]),
        "south": face("lip", [0, 1, 2, 4]),
    }
    keep.append(box([open_x0, 0, 0], [open_x0 + 2, 3.5, 16], lip_faces))
    for z0 in (0, 14):
        post = {d: face("lip", [0, 0, 2, 5]) for d in ("north", "south", "east", "west")}
        post["up"] = face("lip", [0, 0, 2, 2])
        keep.append(box([open_x0, 3.5, z0], [open_x0 + 2, 5, z0 + 2], post))
    return keep


def tag_element(push):
    x0 = 0.3 if push == "right" else 13.7
    faces = {"up": face("tag", [4, 3, 12, 12]), "north": face("tag", [4, 3, 12, 4]), "south": face("tag", [4, 3, 12, 4]),
             "east": face("tag", [4, 3, 5, 4]), "west": face("tag", [4, 3, 5, 4])}
    return box([x0, 5, 10], [x0 + 2, 5.4, 14], faces)


def paddle_element(push):
    cx = 2.5 if push == "right" else 13.5
    faces = {"up": face("paddle", [0, 0, 1.5, 11]), "down": face("paddle", [0, 0, 1.5, 11]),
             "east": face("paddle", [0, 0, 11, 1.5]), "west": face("paddle", [0, 0, 11, 1.5]),
             "north": face("paddle", [0, 0, 1.5, 1.5]), "south": face("paddle", [0, 0, 1.5, 1.5])}
    return box([cx - 0.75, 5.1, 2.5], [cx + 0.75, 6.6, 13.5], faces)


for push in ("right", "left"):
    for tagged in (False, True):
        els = diverter_elements(push) + ([tag_element(push)] if tagged else [])
        # The paddle model is the same shape on the other rail; the renderer turns it, so it is not part of the block model.
        write("models/block/belt_diverter_%s%s.json" % (push, "_tagged" if tagged else ""),
              {"textures": dict(textures(0), **DIV), "elements": els})
    write("models/block/rotor/belt_diverter_paddle_%s.json" % push,
          {"textures": {"particle": NS + "belt_diverter_paddle", "paddle": NS + "belt_diverter_paddle"}, "elements": [paddle_element(push)]})
    write("items/rotor/belt_diverter_paddle_%s.json" % push,
          {"model": {"type": "minecraft:model", "model": "strataindustria:block/rotor/belt_diverter_paddle_%s" % push}})

div_item = {"parent": "minecraft:block/block", "textures": dict(textures(0), **DIV, paddle=NS + "belt_diverter_paddle"),
            "elements": diverter_elements("right") + top_elements(None, 0) + [paddle_element("right")], "display": item["display"]}
write("models/block/belt_diverter_item.json", div_item)
print("diverter models written")
