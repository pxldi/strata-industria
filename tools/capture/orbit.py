#!/usr/bin/env python3
"""Writes a scene that sets a block up on a stone slab and orbits the camera once around it.
usage: orbit.py <block id> <scene out> [frames=36] [radius=2.0]
Then: tools/capture/capture.sh <scene out> <out dir>"""
import math, sys
block, out = sys.argv[1], sys.argv[2]
n = int(sys.argv[3]) if len(sys.argv) > 3 else 36
r = float(sys.argv[4]) if len(sys.argv) > 4 else 2.0
name = block.split(":")[-1] + "_orbit"
lines = ["gamerule advance_time false", "time set noon", "weather clear", "gamerule spawn_mobs false",
         "setblock 0 -61 0 minecraft:smooth_stone", f"setblock 0 -60 0 {block}", "gamemode spectator @p"]
body = [f"cmd {c}" for c in lines] + ["wait 60", "cmd tick freeze"]
for i in range(n):
    a = 2 * math.pi * i / n
    x, z = 0.5 + r * math.sin(a), 0.5 + r * math.cos(a)
    yaw = (math.degrees(math.atan2(-(x - 0.5), (z - 0.5))) + 180) % 360  # looking at the block centre
    body += [f"cmd tp @p {x:.2f} -59.7 {z:.2f} {yaw:.1f} 28", "wait 1", f"frames {name} 1"]
body.append("quit")
open(out, "w").write("\n".join(body) + "\n")
