#!/usr/bin/env python3
"""Writes the oil field block models (seismic charge, wellhead, pump jack and its rotor parts).

Run from the repository root: python3 tools/texturegen/gen_oilfield_models.py
Deterministic. Models are authored facing NORTH (front = -Z). Units are 1/16 block. Verifies that every
element lies within -16..32 on each axis and from <= to. UV layouts follow OilFieldTextures.java.
"""
import json
import math
import os

OUT = "src/main/resources/assets/strataindustria/models/block"
NS = "strataindustria:block/"
FACES = ("north", "south", "east", "west", "up", "down")


def n(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def wrap(a, b):
    """Shift a uv pair into 0..16 and clamp its span to 16."""
    k = math.floor(min(a, b) / 16)
    a, b = a - 16 * k, b - 16 * k
    return (a, min(b, 16)) if b - a <= 16 else (a, a + 16)


def default_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    if face == "north":
        u, v = (16 - x1, 16 - x0), (16 - y1, 16 - y0)
    elif face == "south":
        u, v = (x0, x1), (16 - y1, 16 - y0)
    elif face == "east":
        u, v = (16 - z1, 16 - z0), (16 - y1, 16 - y0)
    elif face == "west":
        u, v = (z0, z1), (16 - y1, 16 - y0)
    elif face == "up":
        u, v = (x0, x1), (z0, z1)
    else:
        u, v = (x0, x1), (16 - z1, 16 - z0)
    u, v = wrap(*u), wrap(*v)
    return [u[0], v[0], u[1], v[1]]


def box(f, t, tex, uv="def", faces=FACES, over=None, rot=None):
    """One element. uv: 'def' default projection, 'clean' default squeezed into texture columns 0..11
    (pump_jack_beam plain steel). over maps face -> (texture, [u0,v0,u1,v1])."""
    f, t = [n(v) for v in f], [n(v) for v in t]
    fa = {}
    for face in faces:
        uvv = default_uv(face, f, t)
        if uv == "clean":
            uvv = [uvv[0] * 12 / 16, uvv[1], uvv[2] * 12 / 16, uvv[3]]
        tx = tex
        if over and face in over:
            tx, uvv = over[face]
        fa[face] = {"uv": [n(v) for v in uvv], "texture": "#" + tx}
    e = {"from": f, "to": t, "faces": fa}
    if rot:
        e["rotation"] = rot
    return e


def brass(f, t, tex="side"):
    """Valve parts: brass patch of wellhead_side (columns 0..3, rows 6..12)."""
    over = {}
    dims = {"north": (t[0] - f[0], t[1] - f[1]), "south": (t[0] - f[0], t[1] - f[1]),
            "east": (t[2] - f[2], t[1] - f[1]), "west": (t[2] - f[2], t[1] - f[1]),
            "up": (t[0] - f[0], t[2] - f[2]), "down": (t[0] - f[0], t[2] - f[2])}
    for fc, (w, h) in dims.items():
        w, h = min(max(w, 0.5), 4), min(max(h, 0.5), 6)
        over[fc] = (tex, [0, 6, w, 6 + h])
    return box(f, t, tex, over=over)


def model(textures, elements, extra=None):
    m = {"textures": textures, "elements": elements}
    if extra:
        m.update(extra)
    return m


# ------------------------------------------------------------------ seismic charge
def charge(tex):
    T = "all"
    wrapside = lambda x: {"uv": None}
    els = [
        box((4, 0, 4), (12, 2, 12), T, over={
            "north": (T, [0, 8, 8, 10]), "south": (T, [0, 8, 8, 10]), "east": (T, [0, 8, 8, 10]), "west": (T, [0, 8, 8, 10]),
            "up": (T, [8, 8, 16, 16]), "down": (T, [8, 8, 16, 16])}),
        box((5, 2, 5), (11, 10, 11), T, over={
            "north": (T, [0, 0, 6, 8]), "south": (T, [6, 0, 12, 8]), "east": (T, [4, 0, 10, 8]), "west": (T, [10, 0, 16, 8]),
            "up": (T, [0, 10, 6, 16]), "down": (T, [0, 10, 6, 16])}),
        box((6, 10, 6), (10, 11, 10), T, over={
            "north": (T, [0, 3, 4, 4]), "south": (T, [4, 3, 8, 4]), "east": (T, [8, 3, 12, 4]), "west": (T, [12, 3, 16, 4]),
            "up": (T, [1, 11, 5, 15]), "down": (T, [1, 11, 5, 15])}),
        box((7.5, 11, 7.5), (8.5, 14, 8.5), T, over={
            "north": (T, [6, 10, 7, 13]), "south": (T, [6, 10, 7, 13]), "east": (T, [6, 10, 7, 13]), "west": (T, [6, 10, 7, 13]),
            "up": (T, [7, 10, 8, 11]), "down": (T, [6, 10, 7, 11])}),
    ]
    return model({"all": NS + tex, "particle": NS + tex}, els)


# ------------------------------------------------------------------ wellhead
def wellhead(top):
    S, P = "side", "top"
    els = [
        box((0, 0, 0), (16, 3, 16), S, over={"up": (P, [0, 0, 16, 16])}),
        box((4, 3, 4), (12, 10, 12), S, faces=("north", "south", "east", "west")),
        box((2, 10, 2), (14, 16, 14), S, over={"up": (P, [2, 2, 14, 14])}),
        # Valve on the +X side: stem, hub, ring and spokes.
        brass((12, 6.5, 7.5), (14, 7.5, 8.5)),
        brass((14, 6, 7), (16, 8, 9)),
        brass((14, 9, 5), (15, 10, 11)),
        brass((14, 4, 5), (15, 5, 11)),
        brass((14, 5, 5), (15, 9, 6)),
        brass((14, 5, 10), (15, 9, 11)),
        brass((14, 6.5, 6), (15, 7.5, 10)),
        brass((14, 5, 7.5), (15, 9, 8.5)),
    ]
    return model({"side": NS + "wellhead_side", "top": NS + top, "particle": NS + "wellhead_side"}, els)


# ------------------------------------------------------------------ pump jack
def pump_jack_els(tex="base", pref=None):
    return [
        box((0, 0, 0), (16, 5, 16), tex),
        box((3, 5, 4), (13, 11, 16), tex),
        box((6, 5, 6), (10, 9, 10), tex, faces=("north", "south", "east", "west", "up")),
    ]


def legs(y0, y1):
    return [box((1, y0, 4), (4, y1, 12), "beam", "clean"), box((12, y0, 4), (15, y1, 12), "beam", "clean")]


def frame(i):
    if i == 0:
        els = legs(0, 16) + [box((0, 0, 3), (5, 2, 13), "beam", "clean"), box((11, 0, 3), (16, 2, 13), "beam", "clean")]
    elif i == 1:
        els = legs(0, 16) + [box((4, 6, 7), (12, 8, 9), "beam", "clean")]
    else:
        els = legs(0, 9) + [box((1, 8, 5), (15, 12, 11), "beam", "clean"), box((0, 9.5, 7.5), (16, 10.5, 8.5), "beam", "clean")]
    return els


def head_els(tex="head", beam="beam"):
    els = []
    z = -16
    while z < -4:
        zc = z + 1
        dz = -4 - zc
        top = round((3 + math.sqrt(144 - dz * dz)) * 2) / 2
        els.append(box((5.5, 1, z), (10.5, min(top, 15), z + 2), tex))
        z += 2
    els.append(box((5.5, 1, -4), (10.5, 15, 0), tex))
    return els


def beam_els():
    bar = box((6.5, 6.5, -8), (9.5, 9.5, 28), "beam", "clean", over={
        "up": ("beam", [12, 0, 15, 16]),
        "east": ("beam", [0, 1, 12, 4]), "west": ("beam", [0, 1, 12, 4]), "down": ("beam", [0, 1, 3, 16])})
    return [bar] + head_els() + [box((4, 0, 18), (12, 7, 28), "beam", "clean"),
                                 box((5, 1, -14), (11, 3, -12), "beam", "clean")]


def rod_els(tex="rod"):
    return [box((7.25, 5, 7.25), (8.75, 32, 8.75), tex, "clean", over={
        "north": (tex, [0, 0, 1.5, 16]), "south": (tex, [0, 0, 1.5, 16]), "east": (tex, [0, 0, 1.5, 16]), "west": (tex, [0, 0, 1.5, 16])}),
        box((5, 30, 7.5), (11, 32, 8.5), tex, "clean")]


def shifted(els, dx, dy, dz, rename=None):
    out = []
    for e in els:
        e = json.loads(json.dumps(e))
        e["from"] = [n(e["from"][0] + dx), n(e["from"][1] + dy), n(e["from"][2] + dz)]
        e["to"] = [n(e["to"][0] + dx), n(e["to"][1] + dy), n(e["to"][2] + dz)]
        if rename:
            for fc in e["faces"].values():
                fc["texture"] = "#" + rename.get(fc["texture"][1:], fc["texture"][1:])
        out.append(e)
    return out


def item_model():
    els = pump_jack_els("base")
    for i in range(3):
        els += shifted(frame(i), 0, 16 * i, 16)
    els += shifted(beam_els(), 0, 33, 16)  # pivot (8,8,8) -> frame pivot (8,42,24), 1 unit low so the head stays in range
    els += shifted(rod_els("beam"), 0, 0, 0)
    els = shifted(els, 0, -16, -14)
    disp = {
        "gui": {"rotation": [22, 255, 0], "translation": [0, 0, 0], "scale": [0.28, 0.28, 0.28]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.16, 0.16, 0.16]},
        "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.28, 0.28, 0.28]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.18, 0.18, 0.18]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.18, 0.18, 0.18]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.22, 0.22, 0.22]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.22, 0.22, 0.22]},
    }
    return model({"base": NS + "pump_jack_base", "beam": NS + "pump_jack_beam", "head": NS + "pump_jack_head",
                  "particle": NS + "pump_jack_base"}, els, {"display": disp})


def main():
    beam_tex = {"beam": NS + "pump_jack_beam", "particle": NS + "pump_jack_beam"}
    files = {
        "seismic_charge": charge("seismic_charge"),
        "seismic_charge_lit": charge("seismic_charge_lit"),
        "wellhead": wellhead("wellhead_top"),
        "wellhead_drilled": wellhead("wellhead_drilled_top"),
        "pump_jack": model({"base": NS + "pump_jack_base", "particle": NS + "pump_jack_base"}, pump_jack_els()),
        "rotor/pump_jack_frame_0": model(beam_tex, frame(0)),
        "rotor/pump_jack_frame_1": model(beam_tex, frame(1)),
        "rotor/pump_jack_frame_2": model(beam_tex, frame(2)),
        "rotor/pump_jack_beam": model({"beam": NS + "pump_jack_beam", "head": NS + "pump_jack_head",
                                       "particle": NS + "pump_jack_beam"}, beam_els()),
        "rotor/pump_jack_rod": model({"rod": NS + "pump_jack_beam", "particle": NS + "pump_jack_beam"}, rod_els()),
        "pump_jack_item": item_model(),
    }
    for name, m in files.items():
        for e in m["elements"]:
            assert all(a <= b for a, b in zip(e["from"], e["to"])), (name, e)
            assert all(-16 <= v <= 32 for v in e["from"] + e["to"]), (name, e)
            for fc in e["faces"].values():
                assert fc["texture"][1:] in m["textures"], (name, fc)
                assert all(0 <= v <= 16 for v in fc["uv"]), (name, fc)
        path = os.path.join(OUT, name + ".json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as fh:
            json.dump(m, fh, indent=2)
            fh.write("\n")
        print(path, len(m["elements"]), "elements")


if __name__ == "__main__":
    main()
