#!/usr/bin/env python3
"""Reproducible Modrinth modpack builder.

  pack/build_mrpack.py lock                 resolve pack/mods.json against Modrinth into pack/mods.lock.json
  pack/build_mrpack.py build [--mod-jar J]  write build/<name>-<version>.mrpack from the lock file

The lock file pins exact files (URL, hashes, size). Builds never touch the network.
"""
import argparse, json, pathlib, sys, urllib.parse, urllib.request, zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
PACK = ROOT / "pack"
API = "https://api.modrinth.com/v2/"
ENV = {"client_only": ("required", "unsupported"), "server_only": ("unsupported", "required")}


def get(path):
    req = urllib.request.Request(API + path, headers={"User-Agent": "strata-industria-pack/1"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def props():
    out = {}
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def lock():
    spec = json.loads((PACK / "mods.json").read_text())
    q = "?loaders=%s&game_versions=%s" % (
        urllib.parse.quote('["neoforge"]'), urllib.parse.quote(json.dumps([spec["minecraft"]])))
    entries = []
    for m in spec["mods"]:
        versions = get("project/%s/version%s" % (m["slug"], q))
        if m["version"] == "auto":
            versions = versions[:1]
        else:
            versions = [v for v in versions if v["version_number"] == m["version"]]
        if not versions:
            sys.exit("no NeoForge %s file for %s %s" % (spec["minecraft"], m["slug"], m["version"]))
        v = versions[0]
        f = next(f for f in v["files"] if f["primary"])
        entries.append({
            "slug": m["slug"], "version": v["version_number"], "channel": v["version_type"],
            "environment": v.get("environment", ""),
            "path": "mods/" + f["filename"], "url": f["url"], "size": f["size"],
            "sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"],
        })
        print("%-26s %-28s %s" % (m["slug"], v["version_number"], v["version_type"]))
    (PACK / "mods.lock.json").write_text(json.dumps(entries, indent=2) + "\n")


def build(mod_jar):
    spec = json.loads((PACK / "mods.json").read_text())
    p = props()
    lockd = json.loads((PACK / "mods.lock.json").read_text())
    files = []
    for e in lockd:
        client, server = ENV.get(e["environment"], ("required", "required"))
        if e["environment"].startswith("client_only_server_optional"):
            client, server = "required", "optional"
        files.append({
            "path": e["path"], "hashes": {"sha1": e["sha1"], "sha512": e["sha512"]},
            "env": {"client": client, "server": server},
            "downloads": [e["url"]], "fileSize": e["size"],
        })
    index = {
        "formatVersion": 1, "game": "minecraft",
        "versionId": p["mod_version"], "name": spec["name"], "summary": spec["summary"],
        "files": files,
        "dependencies": {"minecraft": spec["minecraft"], "neoforge": spec["neoforge"]},
    }
    out = ROOT / "build"
    out.mkdir(exist_ok=True)
    target = out / ("%s-%s.mrpack" % (spec["name"].replace(" ", "-"), p["mod_version"]))
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("modrinth.index.json", json.dumps(index, indent=2))
        if mod_jar:
            jar = pathlib.Path(mod_jar)
            z.write(jar, "overrides/mods/" + jar.name)
    print("wrote %s (%d bytes, %d mods%s)" % (
        target, target.stat().st_size, len(files), ", with mod jar" if mod_jar else ", WITHOUT mod jar"))
    return target


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["lock", "build"])
    ap.add_argument("--mod-jar")
    a = ap.parse_args()
    lock() if a.cmd == "lock" else build(a.mod_jar)
