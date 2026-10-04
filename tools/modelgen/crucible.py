"""Crucible: a thick-walled fired-clay pot with two lugs and a pouring lip on the north side."""
import sys
from modelgen import Model

m = Model("crucible", {
    "particle": "strataindustria:block/crucible_side",
    "side": "strataindustria:block/crucible_side",
    "top": "strataindustria:block/crucible_top",
    "inside": "strataindustria:block/crucible_inside",
    "bottom": "strataindustria:block/crucible_bottom",
}, ambientocclusion=False)

# foot and base, solid
m.box((4, 0, 4), (12, 1, 12), tex="#side", down="#bottom", up="#top", name="foot")
m.box((3, 1, 3), (13, 3, 13), tex="#side", down="#bottom", up="#top", name="base")
# belly (walls 3 px), shoulder (2 px), neck and flared lip (2 px)
m.hollow((2, 3, 2), (14, 8, 14), 3)
m.hollow((3, 8, 3), (13, 10, 13), 2)
m.hollow((4, 10, 4), (12, 11, 12), 2)
# flared lip, north wall left open for the spout channel (x 6..10)
m.box((3, 11, 11), (13, 12, 13), north="#inside", south="#side", east="#side", west="#side", up="#top", down="#side", name="lip_s")
m.box((3, 11, 5), (5, 12, 11), east="#inside", west="#side", up="#top", down="#side", north=None, south=None, name="lip_w")
m.box((11, 11, 5), (13, 12, 11), west="#inside", east="#side", up="#top", down="#side", north=None, south=None, name="lip_e")
m.box((3, 11, 3), (6, 12, 5), north="#side", west="#side", east="#inside", south="#inside", up="#top", down="#side", name="lip_nw")
m.box((10, 11, 3), (13, 12, 5), north="#side", east="#side", west="#inside", south="#inside", up="#top", down="#side", name="lip_ne")
# pouring spout: floor and two cheeks
m.box((6, 10, 1), (10, 11, 4), tex="#side", up="#inside", down="#side", name="spout_floor")
m.box((5, 10, 1), (6, 12, 3), tex="#side", east="#inside", up="#top", name="spout_cheek_w")
m.box((10, 10, 1), (11, 12, 3), tex="#side", west="#inside", up="#top", name="spout_cheek_e")
# lugs
m.box((1, 5, 6), (2, 7, 10), tex="#side", up="#top", down="#side", name="lug_w")
m.box((14, 5, 6), (15, 7, 10), tex="#side", up="#top", down="#side", name="lug_e")

if __name__ == "__main__":
    print(m.write("block"))
    if "--bbmodel" in sys.argv:
        print(m.write_bbmodel("crucible.bbmodel"))
