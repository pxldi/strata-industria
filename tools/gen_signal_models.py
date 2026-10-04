#!/usr/bin/env python3
"""Hand-made models for the block signal (outposts spec 9.4): a post with a lamp and a semaphore arm.

Run from the repository root: python3 tools/gen_signal_models.py
Both models are for a signal that faces north, on the east side of its track, so the arm reaches west over the
rail. The blockstate turns them. The texture (16 x 16) has four bands: iron rows 0-5, the red and white arm rows 6-9,
the arm's dark underside rows 10-12 and the lamp rows 13-15 (left half dark glass, right half lit).
"""
import json
import os

ROOT = "src/main/resources/assets/strataindustria"
TEX = "strataindustria:block/block_signal"

IRON, ARM, UNDER = (0, 6), (6, 10), (10, 13)
LAMP_OFF, LAMP_ON = (0, 13, 8, 16), (8, 13, 16, 16)


def face(band, w, h):
    top, bottom = band
    return {"texture": "#t", "uv": [0, top, max(1, min(16, round(w))), top + max(1, min(bottom - top, round(h)))]}


def box(frm, to, band, **extra):
    (x1, y1, z1), (x2, y2, z2) = frm, to
    sx, sy, sz = x2 - x1, y2 - y1, z2 - z1
    faces = {
        "north": face(band, sx, sy), "south": face(band, sx, sy),
        "east": face(band, sz, sy), "west": face(band, sz, sy),
        "up": face(band, sx, sz), "down": face(band, sx, sz),
    }
    element = {"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces}
    element.update(extra)
    return element


def arm(rotation):
    top = box((-5, 12, 7), (8, 14, 9), ARM)
    top["faces"]["up"] = face(ARM, 13, 2)
    top["faces"]["down"] = face(UNDER, 13, 2)
    # The tip is a white block so a dropped arm still reads from far off.
    element = {**top}
    if rotation:
        element["rotation"] = {"origin": [8, 13, 8], "axis": "z", "angle": rotation}
    weight = box((8, 11.5, 7.25), (11, 14.5, 8.75), IRON)
    if rotation:
        weight["rotation"] = {"origin": [8, 13, 8], "axis": "z", "angle": rotation}
    return [element, weight]


def lamp(lit):
    housing = box((6, 10.5, 9.5), (10, 15.5, 11), IRON)
    glass = {
        "from": [6.5, 11.25, 11], "to": [9.5, 14.75, 11.02],
        "faces": {"south": {"texture": "#t", "uv": list(LAMP_ON if lit else LAMP_OFF)}},
    }
    if lit:
        glass["light_emission"] = 15
    return [housing, glass]


def model(clear):
    elements = [
        box((4, 0, 4), (12, 2, 12), IRON),
        box((6.5, 2, 6.5), (9.5, 14.5, 9.5), IRON),
    ]
    elements += lamp(not clear)
    elements += arm(45 if clear else 0)
    return {"textures": {"particle": TEX, "t": TEX}, "elements": elements}


def write(path, data):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


write("models/block/block_signal_stop.json", model(False))
write("models/block/block_signal_clear.json", model(True))
