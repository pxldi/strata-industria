#!/usr/bin/env python3
"""Hand-made models for the trolley bracket and the trolley wire segment (outposts spec 9.2).

Run from the repository root: python3 tools/gen_tram_models.py
The bracket arm points north (towards low z); the blockstate turns it. Its texture is three bands:
steel rows 0-7, ceramic rows 8-11, copper clamp rows 12-15.
"""
import json
import os

ROOT = "src/main/resources/assets/strataindustria"

STEEL, GLAZE, CLAMP = (0, 0, 8), (0, 8, 12), (0, 12, 16)


def element(frm, to, band):
    _, top, bottom = band
    (x1, y1, z1), (x2, y2, z2) = frm, to
    size = {"x": x2 - x1, "y": y2 - y1, "z": z2 - z1}

    def face(w, h, flip=False):
        # Scale the band onto the face without stretching it much: clamp to the band's height.
        h = min(h, bottom - top)
        return {"texture": "#t", "uv": [0, top, min(16, max(1, w)), top + max(1, h)]}

    faces = {
        "north": face(size["x"], size["y"]), "south": face(size["x"], size["y"]),
        "east": face(size["z"], size["y"]), "west": face(size["z"], size["y"]),
        "up": face(size["x"], size["z"]), "down": face(size["x"], size["z"]),
    }
    return {"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces}


def write(path, data):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


bracket = {
    "textures": {"particle": "strataindustria:block/trolley_bracket", "t": "strataindustria:block/trolley_bracket"},
    "elements": [
        element((5, 4, 13), (11, 16, 16), STEEL),
        element((7, 11, 1), (9, 14, 14), STEEL),
        element((7.25, 9, 11), (8.75, 11, 13), STEEL),
        element((7.25, 10, 8), (8.75, 12, 11), STEEL),
        element((6.5, 9, 1.5), (9.5, 11, 3.5), GLAZE),
        element((7, 7.75, 1.75), (9, 9, 3.25), CLAMP),
    ],
}
write("models/block/trolley_bracket.json", bracket)

segment = {
    "textures": {"particle": "strataindustria:block/trolley_line", "line": "strataindustria:block/trolley_line"},
    "elements": [{
        "from": [7.5, 7.5, 0], "to": [8.5, 8.5, 16],
        "faces": {side: {"texture": "#line", "uv": [0, 0, 1, 16]} for side in ("up", "down", "east", "west")},
    }],
}
write("models/block/rotor/trolley_segment.json", segment)
write("items/rotor/trolley_segment.json", {"model": {"type": "minecraft:model", "model": "strataindustria:block/rotor/trolley_segment"}})
