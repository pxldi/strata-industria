"""Box-model library for Strata Industria.

Models are authored as small Python scripts (one per family) so they are reviewable, reproducible and
use exact UVs at 16 px per block. No dependencies. See README.md.

    from modelgen import Model
    m = Model("crucible", textures={"side": "strataindustria:block/crucible_side"})
    m.box((2, 0, 2), (14, 3, 14), side="#side", up="#top", down="#bottom")
    m.write("block")
"""
import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "strataindustria")
ANGLES = (-45, -22.5, 0, 22.5, 45)  # the only rotations vanilla allows
FACES = ("north", "south", "east", "west", "up", "down")
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}
AXIS = {"north": (2, 0), "south": (2, 1), "west": (0, 0), "east": (0, 1), "down": (1, 0), "up": (1, 1)}


def default_uv(face, a, b):
    """Vanilla's own default UV for a face: the block's 16 px grid projected onto it (16 texels per block)."""
    (x1, y1, z1), (x2, y2, z2) = a, b
    return {
        "down": [x1, 16 - z2, x2, 16 - z1],
        "up": [x1, z1, x2, z2],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1],
        "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]


class Box:
    def __init__(self, a, b, faces, rotation=None, name=None, shade=True):
        self.a, self.b, self.faces, self.rotation, self.name, self.shade = a, b, faces, rotation, name, shade

    def rect(self, face):
        """The face's rectangle as (plane, (lo1, hi1), (lo2, hi2)) in block coordinates."""
        axis, hi = AXIS[face]
        plane = (self.b if hi else self.a)[axis]
        others = [i for i in range(3) if i != axis]
        return axis, plane, tuple((self.a[i], self.b[i]) for i in others)


class Model:
    def __init__(self, name, textures, parent="block/block", ambientocclusion=None, display=None):
        self.name, self.textures, self.parent = name, dict(textures), parent
        self.ambientocclusion, self.display = ambientocclusion, display
        self.boxes = []

    # -- building blocks --------------------------------------------------------------------------------------------
    def box(self, a, b, tex=None, rotate=None, name=None, shade=True, cull=True, **faces):
        """Add a box. `tex` textures every face, `up=`, `down=`, `side=` (four sides) or single faces override it.
        A face given as None is left out. A texture is "#key". `uv_<face>=(u1, v1, u2, v2)` overrides the UV.
        `rotate=(origin, axis, angle)` with a vanilla angle step."""
        a, b = tuple(float(v) for v in a), tuple(float(v) for v in b)
        assert all(a[i] < b[i] for i in range(3)), f"empty box {a} {b}"
        assert all(0 <= v <= 16 for v in a + b) or rotate is not None or True
        chosen = {f: tex for f in FACES}
        side = faces.pop("side", None)
        if side is not None:
            for f in ("north", "south", "east", "west"):
                chosen[f] = side
        for f in FACES:
            if f in faces:
                chosen[f] = faces.pop(f)
        uvs = {k[3:]: faces.pop(k) for k in list(faces) if k.startswith("uv_")}
        assert not faces, f"unknown face arguments {sorted(faces)}"
        out = {}
        for f, t in chosen.items():
            if t is None:
                continue
            assert t.startswith("#") and t[1:] in self.textures, f"unknown texture {t}"
            out[f] = {"texture": t, "uv": list(uvs.get(f) or default_uv(f, a, b)), "cull": cull}
        rot = None
        if rotate is not None:
            origin, axis, angle = rotate
            assert angle in ANGLES, f"rotation {angle} is not a vanilla step"
            rot = {"origin": list(origin), "axis": axis, "angle": angle}
        self.boxes.append(Box(a, b, out, rot, name, shade))
        return self

    def hollow(self, a, b, wall, inner="#inside", top="#top", bottom=None, side="#side", floor=None):
        """An open-topped (or open-bottomed) square ring of four boxes, `wall` px thick; inner faces use `inner`."""
        (x1, y1, z1), (x2, y2, z2) = a, b
        w = wall
        # north and south walls run the full width, west and east fill between them
        self.box((x1, y1, z1), (x2, y2, z1 + w), south=inner, north=side, east=side, west=side, up=top, down=bottom)
        self.box((x1, y1, z2 - w), (x2, y2, z2), north=inner, south=side, east=side, west=side, up=top, down=bottom)
        self.box((x1, y1, z1 + w), (x1 + w, y2, z2 - w), east=inner, west=side, up=top, down=bottom,
                 north=None, south=None)
        self.box((x2 - w, y1, z1 + w), (x2, y2, z2 - w), west=inner, east=side, up=top, down=bottom,
                 north=None, south=None)
        return self

    def ring_stack(self, y0, steps, wall, **kw):
        """A pot-like profile: `steps` is a list of (outer_lo, outer_hi, height); every step is a hollow ring."""
        y = y0
        for lo, hi, h in steps:
            self.hollow((lo, y, lo), (hi, y + h, hi), wall, **kw)
            y += h
        return self

    def mirror_x(self, start=0):
        """Mirror every box added since index `start` across the block's centre (x -> 16 - x)."""
        for bx in list(self.boxes[start:]):
            faces = {}
            for f, d in bx.faces.items():
                m = {"east": "west", "west": "east"}.get(f, f)
                u = d["uv"]
                faces[m] = dict(d, uv=[u[2], u[1], u[0], u[3]] if f in ("up", "down") else list(u))
            self.boxes.append(Box((16 - bx.b[0], bx.a[1], bx.a[2]), (16 - bx.a[0], bx.b[1], bx.b[2]), faces,
                                  bx.rotation, bx.name, bx.shade))
        return self

    # -- output -----------------------------------------------------------------------------------------------------
    def _culled(self, bx, face):
        """True when another box's opposite face lies in the same plane and covers this face entirely."""
        if not bx.faces[face].get("cull", True) or bx.rotation:
            return False
        axis, plane, (r1, r2) = bx.rect(face)
        for other in self.boxes:
            if other is bx or other.rotation or OPPOSITE[face] not in other.faces:
                continue
            ax2, plane2, (s1, s2) = other.rect(OPPOSITE[face])
            if ax2 == axis and plane2 == plane and s1[0] <= r1[0] and s1[1] >= r1[1] and s2[0] <= r2[0] and s2[1] >= r2[1]:
                return True
        return False

    def to_json(self):
        elements = []
        for bx in self.boxes:
            faces = {}
            for f, d in bx.faces.items():
                if self._culled(bx, f):
                    continue
                entry = {"texture": d["texture"], "uv": [round(v, 4) for v in d["uv"]]}
                # blocks resting on the ground never need their underside drawn
                if f == "down" and bx.a[1] == 0 and not bx.rotation:
                    entry["cullface"] = "down"
                faces[f] = entry
            el = {"from": list(bx.a), "to": list(bx.b)}
            if bx.name:
                el["name"] = bx.name
            if bx.rotation:
                el["rotation"] = bx.rotation
            if not bx.shade:
                el["shade"] = False
            el["faces"] = faces
            elements.append(el)
        doc = {"parent": self.parent, "textures": self.textures}
        if self.ambientocclusion is not None:
            doc["ambientocclusion"] = self.ambientocclusion
        if self.display:
            doc["display"] = self.display
        doc["elements"] = elements
        return doc

    def write(self, kind="block", out_dir=None):
        path = os.path.join(out_dir or os.path.join(ASSETS, "models", kind), self.name + ".json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            json.dump(self.to_json(), f, indent=2)
            f.write("\n")
        return path

    def write_bbmodel(self, path):
        """Blockbench project file so the model can be opened and tweaked by hand."""
        import uuid
        els = []
        for i, bx in enumerate(self.boxes):
            el = {"name": bx.name or f"box{i}", "type": "cube", "from": list(bx.a), "to": list(bx.b),
                  "uuid": str(uuid.uuid4()), "faces": {}, "color": i % 8}
            if bx.rotation:
                el["origin"] = bx.rotation["origin"]
                el["rotation"] = [bx.rotation["angle"] if bx.rotation["axis"] == a else 0 for a in "xyz"]
            for f in FACES:
                d = bx.faces.get(f)
                el["faces"][f] = {"uv": d["uv"] if d else [0, 0, 0, 0], "texture": list(self.textures).index(d["texture"][1:]) if d else None}
            els.append(el)
        proj = {"meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
                "name": self.name, "elements": els, "outliner": [e["uuid"] for e in els],
                "textures": [{"id": str(i), "name": k, "path": v} for i, (k, v) in enumerate(self.textures.items())],
                "resolution": {"width": 16, "height": 16}}
        with open(path, "w") as f:
            json.dump(proj, f, indent=2)
        return path
