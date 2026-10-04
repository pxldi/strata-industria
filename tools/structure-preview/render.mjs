// Renders exported structure JSON (from the structure_preview_export GameTest) to isometric PNGs.
// usage: node render.mjs --in <dir with .json> --out <dir> --assets <assets dir> [--assets ...] [--only name] [--size 900]
// Each --assets dir holds <namespace>/{blockstates,models,textures}; later dirs override earlier ones.
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const here = path.dirname(fileURLToPath(import.meta.url))
const args = process.argv.slice(2)
const opt = { assets: [], size: 900 }
for (let i = 0; i < args.length; i += 2) {
  const k = args[i].replace(/^--/, '')
  if (k === 'assets') opt.assets.push(args[i + 1]); else opt[k] = args[i + 1]
}
if (!opt.in || !opt.out || !opt.assets.length) throw new Error('need --in --out --assets')
fs.mkdirSync(opt.out, { recursive: true })

const split = id => { const [ns, p] = id.includes(':') ? id.split(':') : ['minecraft', id]; return [ns, p] }
function find(kind, id, ext) {
  const [ns, p] = split(id)
  for (const root of [...opt.assets].reverse()) {
    const f = path.join(root, ns, kind, p + ext)
    if (fs.existsSync(f)) return f
  }
  return null
}
const models = {}, defs = {}, textures = {}, missing = new Set()
function loadModel(id) {
  const key = id.includes(':') ? id : 'minecraft:' + id
  if (key in models) return
  const f = find('models', key, '.json')
  if (!f) { missing.add('model ' + key); models[key] = null; return }
  const m = JSON.parse(fs.readFileSync(f, 'utf8'))
  models[key] = m
  if (m.parent) loadModel(m.parent)
  for (let [k, t] of Object.entries(m.textures ?? {})) {
    if (typeof t !== 'string') m.textures[k] = t = t.sprite ?? t.texture ?? '#missing' // newer texture-slot objects
    if (!t.startsWith('#')) loadTexture(t)
  }
}
function loadTexture(id) {
  const key = id.includes(':') ? id : 'minecraft:' + id
  if (key in textures) return
  const f = find('textures', key, '.png')
  if (!f) { missing.add('texture ' + key); textures[key] = null; return }
  textures[key] = fs.readFileSync(f).toString('base64')
}
function loadBlock(id) {
  const key = id.includes(':') ? id : 'minecraft:' + id
  if (key in defs) return
  const f = find('blockstates', key, '.json')
  if (!f) { missing.add('blockstate ' + key); defs[key] = null; return }
  const d = JSON.parse(fs.readFileSync(f, 'utf8'))
  defs[key] = d
  const each = v => (Array.isArray(v) ? v : [v]).forEach(x => x?.model && loadModel(x.model))
  Object.values(d.variants ?? {}).forEach(each)
  ;(d.multipart ?? []).forEach(p => each(p.apply))
}

const SKIP = new Set(['minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'])
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] })
const page = await browser.newPage({ viewport: { width: +opt.size, height: +opt.size } })
await page.setContent('<html><body style="margin:0"><canvas id="c" width="' + opt.size + '" height="' + opt.size + '"></canvas></body></html>')
await page.addScriptTag({ path: path.join(here, 'node_modules/gl-matrix/gl-matrix-min.js') })
await page.addScriptTag({ path: path.join(here, 'node_modules/deepslate/dist/deepslate.umd.cjs') })

for (const file of fs.readdirSync(opt.in).filter(f => f.endsWith('.json')).sort()) {
  const name = file.replace(/\.json$/, '')
  if (opt.only && !new RegExp(opt.only).test(name)) continue
  const data = JSON.parse(fs.readFileSync(path.join(opt.in, file), 'utf8'))
  for (const p of data.palette) loadBlock(p.split('[')[0])
  const out = await page.evaluate(async ({ data, models, defs, textures, size, skip }) => {
    const ds = window.deepslate
    const Id = ds.Identifier
    const blobs = {}
    for (const [k, b64] of Object.entries(textures)) {
      if (!b64) continue
      const bin = Uint8Array.from(atob(b64), c => c.charCodeAt(0))
      blobs[k] = new Blob([bin], { type: 'image/png' })
    }
    // deepslate wants texture ids as given in models ("block/stone", "ns:block/x")
    const atlas = await ds.TextureAtlas.fromBlobs(Object.fromEntries(Object.entries(blobs).map(([k, v]) => [k, v])))
    const idOf = s => Id.parse(s).toString()
    const bm = {}, bd = {}
    for (const [k, v] of Object.entries(defs)) if (v) bd[idOf(k)] = ds.BlockDefinition.fromJson(v)
    for (const [k, v] of Object.entries(models)) if (v) bm[idOf(k)] = ds.BlockModel.fromJson(v)
    const resources = {
      getBlockDefinition: id => bd[id.toString()] ?? null,
      getBlockModel: id => bm[id.toString()] ?? null,
      getTextureUV: id => atlas.getTextureUV(id),
      getTextureAtlas: () => atlas.getTextureAtlas(),
      getPixelSize: () => atlas.getPixelSize(),
      getBlockFlags: id => ({ opaque: false, semi_transparent: false, self_culling: false }),
      getBlockProperties: () => null,
      getDefaultBlockProperties: () => null,
    }
    Object.values(bm).forEach(m => m.flatten(resources))
    const [sx, sy, sz] = data.size
    const st = new ds.Structure([sx, sy, sz])
    const props = s => {
      const m = s.match(/\[(.*)\]$/); if (!m) return {}
      return Object.fromEntries(m[1].split(',').map(kv => kv.split('=')))
    }
    for (const [x, y, z, i] of data.blocks) {
      const s = data.palette[i]
      if (skip.includes(s.split('[')[0])) continue
      st.addBlock([x, y, z], s.split('[')[0], props(s))
    }
    const canvas = document.getElementById('c')
    const gl = canvas.getContext('webgl', { preserveDrawingBuffer: true, antialias: true })
    const r = new ds.StructureRenderer(gl, st, resources, { chunkSize: 16 })
    const mat4 = window.glMatrix.mat4
    const shots = []
    const lo = [1e9, 1e9, 1e9], hi = [-1e9, -1e9, -1e9]
    for (const b of data.blocks) for (let a = 0; a < 3; a++) { lo[a] = Math.min(lo[a], b[a]); hi[a] = Math.max(hi[a], b[a] + 1) }
    // frame the blocks actually present, not the (mostly empty) export box
    const cx = (lo[0] + hi[0]) / 2, cy = (lo[1] + hi[1]) / 2, cz = (lo[2] + hi[2]) / 2
    const radius = Math.hypot(hi[0] - lo[0], hi[2] - lo[2]) * 0.5 + (hi[1] - lo[1]) * 0.35 + 1
    for (const [label, yaw] of [['south-west', 45], ['south-east', 135], ['north-east', 225], ['north-west', 315]]) {
      const v = mat4.create()
      // orthographic isometric: pitch 30deg, yaw around the structure centre
      mat4.rotateX(v, v, 30 * Math.PI / 180)
      mat4.rotateY(v, v, yaw * Math.PI / 180)
      mat4.translate(v, v, [-cx, -cy, -cz])
      const proj = mat4.create()
      mat4.ortho(proj, -radius, radius, -radius, radius, -500, 500)
      r.setViewport(0, 0, size, size)
      r.projMatrix = proj
      gl.clearColor(0.62, 0.78, 0.95, 1)
      gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
      r.drawStructure(v)
      shots.push([label, canvas.toDataURL('image/png')])
    }
    return shots
  }, { data, models, defs, textures, size: +opt.size, skip: [...SKIP] })
  for (const [label, url] of out) {
    fs.writeFileSync(path.join(opt.out, `${name}_${label}.png`), Buffer.from(url.split(',')[1], 'base64'))
  }
  console.log('rendered', name)
}
if (missing.size) console.log('missing assets:\n  ' + [...missing].sort().join('\n  '))
await browser.close()
