# capture

Scripted scenes recorded in the real game client under a virtual screen (software Vulkan), turned into GIFs.

```
apt-get install -y xvfb mesa-vulkan-drivers          # once per container
python3 tools/capture/orbit.py strataindustria:crucible tools/capture/scenes/crucible_orbit.scene
tools/capture/capture.sh tools/capture/scenes/crucible_orbit.scene out/ [fps]   # CROP="crop=w:h:x:y," optional
```

About a minute per run after the first. The client creates a flat creative world, runs the scene and saves
`<name>_NNN.png` frames (854x480) plus a GIF per frame series. Frames are captured by freezing the game and stepping one
tick per frame, so slow software rendering does not distort animations. Dev only: `CaptureClient` does nothing without
`-Dstrata.capture` (set by `-Pcapture=<scene>` on `runClient`).

Scene lines: `cmd <server command>`, `wait <client ticks>`, `shot <name>`, `frames <name> <count>` (repeat lines with one
name to continue the series), `quit`. `#` comments.
