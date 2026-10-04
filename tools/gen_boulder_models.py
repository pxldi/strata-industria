#!/usr/bin/env python3
"""
Writes the boulder block models (redesign R1): for each size (1 to 3), crack stage (0 to 2) and flint nodules
(no or yes) one template that datagen fills with the rock's own texture. A boulder is a few stacked and
turned blocks of rock; cracks and nodules are overlay shells a hair outside them. Facing north; the
blockstate turns them.
Run from the repository root: python3 tools/gen_boulder_models.py
"""
import json
import os
import re

OUT = "src/main/resources/assets/strataindustria/models/block"

# Per size: (from, to, y-turn in degrees about the block centre). Pixels.
BODIES = {
    1: [
        ((3, 0, 3), (13, 3, 12), 0),
        ((4, 3, 4), (11, 5, 10), 22.5),
    ],
    2: [
        ((1, 0, 1), (15, 5, 14), 0),
        ((3, 5, 3), (13, 9, 12), -22.5),
        ((10, 0, 10), (15, 3, 15), 22.5),
    ],
    3: [
        ((0, 0, 1), (16, 7, 15), 0),
        ((2, 7, 2), (14, 11, 13), 22.5),
        ((4, 11, 4), (11, 13, 10), -22.5),
        ((9, 5, 9), (16, 9, 16), 22.5),
    ],
}


def faces(frm, to, tex, grow=0.0, down=True):
    x1, y1, z1 = frm
    x2, y2, z2 = to
    sides = {
        "up": {"uv": [x1, z1, x2, z2], "texture": tex},
        "north": {"uv": [x1, 16 - y2, x2, 16 - y1], "texture": tex},
        "south": {"uv": [x1, 16 - y2, x2, 16 - y1], "texture": tex},
        "west": {"uv": [z1, 16 - y2, z2, 16 - y1], "texture": tex},
        "east": {"uv": [z1, 16 - y2, z2, 16 - y1], "texture": tex},
    }
    if down:
        sides["down"] = {"uv": [x1, z1, x2, z2], "texture": tex, "cullface": "down"}
    return sides


def element(frm, to, turn, tex, grow=0.0, down=True):
    e = {
        "from": [frm[0] - grow, frm[1] - (0 if frm[1] == 0 else grow), frm[2] - grow],
        "to": [to[0] + grow, to[1] + grow, to[2] + grow],
        "faces": faces(frm, to, tex, grow, down),
    }
    if turn:
        e["rotation"] = {"angle": turn, "axis": "y", "origin": [8, 0, 8]}
    return e


COATS = {1: "gossan", 2: "bloom"}


def model(size, cracks, flinty, coat):
    elements = [element(f, t, turn, "#rock") for f, t, turn in BODIES[size]]
    if flinty:
        elements += [element(f, t, turn, "#nodules", 0.02, False) for f, t, turn in BODIES[size]]
    if coat:
        elements += [element(f, t, turn, "#coat", 0.03, False) for f, t, turn in BODIES[size]]
    if cracks:
        elements += [element(f, t, turn, "#crack", 0.04, False) for f, t, turn in BODIES[size]]
    textures = {"particle": "#rock", "nodules": "strataindustria:block/boulder_nodules"}
    if coat:
        textures["coat"] = "strataindustria:block/boulder_" + COATS[coat]
    if cracks:
        textures["crack"] = "strataindustria:block/boulder_crack_%d" % cracks
    if not flinty:
        del textures["nodules"]
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


os.makedirs(OUT, exist_ok=True)
for size in BODIES:
    for cracks in range(0, 3):
        for flinty in (0, 1):
            for coat in (0, 1, 2):
                name = "template_boulder_%d_%d_%d_%d.json" % (size, cracks, flinty, coat)
                with open(os.path.join(OUT, name), "w") as f:
                    text = json.dumps(model(size, cracks, flinty, coat), indent=2)
                    f.write(re.sub(r"\[\s+([-\d., \s]+?)\s+\]", lambda m: "[" + " ".join(m.group(1).split()) + "]", text) + "\n")
print("wrote", len(BODIES) * 3 * 2 * 3, "boulder models")
