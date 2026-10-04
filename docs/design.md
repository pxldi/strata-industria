# Progression Design Document

Name: **Strata Industria** (mod id `strataindustria`).

Status: draft v1, October 2026. Target: Minecraft Java 26.3 on NeoForge.

---

## 1. Vision

A single, cohesive technology mod that takes the player from knapping flint in the dirt to running a fully automated industrial base, where **every tier changes how you play**, not just which numbers go up.

It borrows the depth of GregTech New Horizons, the grounded survival and geology of TerraFirmaGreg, and the tactile, hand-made feel of Vintage Story, but is built as one mod with one design voice instead of a pack of 300 mods glued together with recipe scripts.

### Design pillars

1. **Hands first, machines later.** Early tiers are physical and interactive (knapping, pottery, smithing, pouring metal). Machines arrive as a reward that automates what the player already understands by hand.
2. **The world is the tech tree.** Geology decides what you can build and where. Finding the right rock layer, ore vein or fuel source is progression, not just a chest of iron.
3. **Depth without tedium.** Long chains are fine, repetitive clicks are not. Every chain has a hand-scale version and an automated version; the second one is always unlocked before the first becomes a chore.
4. **One coherent economy.** Byproducts from one chain feed another. Slag, gases, and low-grade ores have uses. Nothing is a pure trash item.
5. **Readable and polished.** Every multiblock has an in-world preview, every machine explains why it is not running, and the in-game journal tells you what to aim for next.

### Target experience

- Tier 0 to 3 (hand age): 6 to 12 hours. Survival-flavoured, exploratory, cozy.
- Tier 4 to 5 (steam and early electric): 15 to 30 hours. Building a base, first automation.
- Tier 6 to 8 (industrial to late game): 40+ hours. Factory planning, logistics, big projects.
- Total full playthrough: roughly 100 hours, far shorter than GTNH, far longer than a typical tech mod.

---

## 2. What makes it different

| Aspect | GTNH | TerraFirmaGreg | Vintage Story | **This mod** |
|---|---|---|---|---|
| Form | 1.7.10 modpack, 300+ mods | Modpack (TFC + GregTech CEu + Create) | Standalone game | **Single mod on current Minecraft** |
| Early game | Vanilla-ish, then gated | TFC survival, very slow | Knapping, clay, bloomery | **VS-style hand crafting, but paced faster** |
| Geology | Ore veins by dimension | Rock layers, TFC veins | Rock strata, prospecting | **Rock strata decide ore types and quality; prospecting tools per tier** |
| Mid game | Steam, then 15 voltage tiers | GregTech voltages + Create | Bronze, iron, steel by hand | **Mechanical power to steam to 4 electric tiers** |
| Recipe volume | Enormous, many arbitrary steps | Large | Small | **Medium; every step has a reason** |
| Onboarding | Quest book, wiki required | Quest book, wiki required | In-game handbook | **Built-in journal with live goals and machine diagnostics** |
| Grind | Deliberately very high | High | Moderate | **Moderate; tedium is removed by automation unlocks** |

The unique hooks, in one sentence each:

- **Material quality**: ores come in grades (poor, normal, rich) tied to geology, and metal you make carries a quality value that affects tool durability and machine efficiency. Better rocks, better gear.
- **Heat as a resource**: items and blocks have temperature. You work hot metal, quench it, and later move heat between machines through pipes and heat exchangers instead of just burning coal in each one.
- **Hand-to-machine mirroring**: every hand process (knapping, smithing, pouring, sieving, washing) has a later machine version that does exactly the same transformation, so the player always knows what a new machine does.
- **Byproduct economy**: processing chains always produce secondary outputs that are inputs elsewhere, so building out a factory pays off sideways, not just upward.
- **Multiblocks you assemble in the world** with a ghost preview, partial-structure feedback, and upgrades by swapping blocks (better casing, better coils) instead of building a whole new structure.

---

## 3. Tier overview

| Tier | Name | Key unlock | Power | Core materials |
|---|---|---|---|---|
| 0 | Stone | Knapping, flint-strike fire, primitive tools | Muscle | Flint, stone, sticks, bark cord |
| 1 | Fire and Clay | Pottery, fire pit firing, charcoal, crucible | Fire | Clay, charcoal, fired ceramics |
| 2 | Copper and Bronze | Smelting and casting, anvil smithing | Fire | Copper, tin, bronze, (arsenical bronze) |
| 3 | Iron | Bloomery, wrought iron, water wheel | Water, wind (mechanical) | Wrought iron, leather, lumber |
| 4 | Steel and Steam | Crucible steel, blast furnace, steam engines | Steam (mechanical and heat) | Steel, brass, fire brick |
| 5 | Electric (LV, MV) | Generators, wiring, motors, first electric machines | Electricity | Copper wire, steel, rubber, redstone alloys |
| 6 | Industrial (HV) | Oil and petrochemistry, plastics, chemical plant | Electricity, fuels | Plastics, aluminium, stainless steel |
| 7 | Precision (EV) | Silicon, circuits, cleanroom, automation logic | Electricity | Silicon, gold, titanium |
| 8 | Atomic (IV) | Fission, deep core drilling, endgame alloys | Nuclear | Uranium, tungsten, exotic alloys |
| Endgame | Megaprojects | One or two big constructions as a "win" | All | Everything |

Tiers 0 to 2 are the first playable slice. Tiers 3 and 4 are the second milestone. Tiers 5 and beyond come later and are described here at lower resolution on purpose.

---

## 4. Tiers in detail

### Tier 0: Stone

**Goal:** survive the first night without wood tools from a crafting table.

- Wood tools are removed. Logs cannot be punched; you need a stone axe.
- **Knapping**: hold a rock shard or flint, pick a shape, strike it on a surface in the world. The last blow binds the head to a stick and bark cord from your inventory and the finished tool pops out; its durability comes from the rock.
- **Boulders in the world**: weathered boulders of the local rock stand on the surface. Hit one with a bare hand: cracks open, it splits and rock shards hop out. Rock type matches the stone below, so you learn geology from the first minute.
- **Branches and cord**: shake leaves to snap a branch (sticks and a bark strip), fell whole trees with an axe, twist two bark strips into cord by hand, beat cord into bark cloth on a stone.
- Tools: stone knife, axe, shovel, hammer, spear. Low durability, quick to make.
- **Flint strike**: strike flint on a rock over the fire pit to light it. Campfire cooks food and can heat small items.
- Crafting table requires a stone axe and sawn planks (axe-chopped planks are 2 per log instead of 4).

Exit condition: having a campfire, stone tools and a stash of clay.

### Tier 1: Fire and Clay

**Goal:** control heat.

- **Clay forming**: the same in-world verb as knapping, with clay: crucibles, ingot molds, tool-head molds, bricks.
- **Firing**: stand unfired pottery on the hearth beside a hot fire pit (up to four pieces). It glows and rings when done.
- **Charcoal pit**: stack logs, cover with dirt, light. Yields charcoal by volume. First "multiblock" in spirit, no GUI.
- **Forge (basic)**: a small charcoal-fired hearth that heats items to working temperature. Introduces the temperature system and the temperature colour on item tooltips.
- **Prospecting I**: by hand, look at boulders and ore nuggets ("surface indicators") that hint at veins below.

Exit condition: fired crucible, ingot molds, a forge, and found surface copper.

### Tier 2: Copper and Bronze

**Goal:** first metal, first real tools.

- **Ore mining** with stone pickaxe is slow; native copper and malachite are minable by stone tools, tin (cassiterite) needs copper tools.
- **Smelting** in a crucible over the forge. You put ore pieces in by "units" (not whole ingots), watch the melt percentage and composition in the crucible GUI.
- **Alloying** happens in fixed whole parts in the crucible: bronze is 3 parts copper to 1 part tin, in ingots, nuggets or ore, any multiple. Wrong parts give no alloy. Arsenical bronze and bismuth bronze exist as regional alternatives so geology matters.
- **Casting**: pour molten metal into ceramic molds for ingots or tool heads. Fired molds last.
- **Anvil smithing**: a stone anvil first (the iron anvil comes with tier 4). There is no screen: put a hot piece on the anvil, sneak and use the hammer to pick a shape, strike. Each shape takes a few blows; blows while the metal is bright and on the hammer's rebound glint count for more and give a better piece. Nothing can be ruined, a cold piece only thuds and waits for a reheat.
- **Bronze tools and armour**: a real step up.
- **Prospecting II**: bronze prospector's pick. Right-click a block to get a reading of nearby ore types and rough quantity.
- **Quern**: hand-cranked mill for flour, crushed ore, and pigments. First "mirrored" process (later replaced by the crusher).

Exit condition: full bronze kit, an anvil, and a reliable tin supply. This is the end of the first playable slice.

### Tier 3: Iron

**Goal:** iron, and the first mechanical power.

- **Bloomery**: a multiblock of fire bricks and a chimney. Load ore and charcoal, run it for a day, get an iron bloom.
- **Bloom refining**: hammer the hot bloom on the anvil into wrought iron. Must reheat between hits; heat management becomes skill.
- **Mechanical power**: water wheel and windmill drive axles, gearboxes and belts. Power has speed and torque, kept simple (a single "stress" number per network, similar in spirit to Create but much more restrained).
- **First machines** run on rotation: mechanical quern (crusher), saw mill, trip hammer (automates some anvil work), bellows (boosts forge and bloomery temperature).
- **Leather and tanning**: barrels with tannin, leather for belts and bellows.
- **Ore processing I**: crushed ore in a sluice or washing pan gives cleaner ore plus small byproducts (gravel, sand, trace metals).
- **Prospecting III**: core sampler that drills a column and reports ore and rock layers by depth.

Exit condition: wrought iron tools, a water-wheel-driven workshop, steady charcoal.

### Tier 4: Steel and Steam

**Goal:** steel, steam, and the first real factory.

- **Crucible steel** (small batches, by hand) and then the **blast furnace** multiblock that produces pig iron, and a **converter** that turns pig iron into steel. Coke oven for coke and creosote.
- **Brass** via zinc (from sphalerite, needs roasting): first gas byproduct (sulfur dioxide to sulfuric acid later).
- **Steam**: boilers (solid fuel, later liquid fuel), steam engines that output rotation, and steam-powered machines that use steam directly. Boilers have pressure; overpressure vents and later damages the boiler. No instant explosions without warning.
- **Heat network**: heat pipes move heat from a firebox to machines. A single big firebox can serve several machines.
- **Fluids**: copper and bronze pipes, tanks, valves. Fluids have temperature limits per pipe material.
- **Automation basics**: conveyor belts, chutes, item filters, simple inserters. Enough to automate a single chain.
- **Ore processing II**: crusher, washer, magnetic separator. Each step adds a byproduct.

Exit condition: steel, a running steam plant, and at least one fully automated chain.

### Tier 5: Electric (LV and MV)

**Goal:** electricity changes logistics; power can now travel.

- **Generators**: steam turbine, combustion generator.
- **Energy model**: four voltage tiers only (LV, MV, HV, EV, plus IV for tier 8). Cables have a voltage rating and loss per block. Overvolting a machine stops it and shows a clear warning; it does not delete the machine. Stated in plain units (the mod uses its own unit, interoperable with the standard NeoForge energy capability through converters).
- **Rubber**: tree tapping then vulcanisation with sulfur.
- **Electric machines** that mirror earlier ones (electric furnace, macerator, wiremill, bender, lathe, extruder, assembler).
- **Electrolysis** for aluminium (expensive), hydrogen, oxygen, chlorine.
- **Logistics I**: item and fluid pipes with filters, a simple storage controller.

### Tier 6: Industrial (HV)

- **Oil**: discovered by seismic survey, extracted by pump jack, distilled in a column multiblock into fractions.
- **Plastics and petrochemistry**: polyethylene, PVC, synthetic rubber.
- **Stainless steel and aluminium** become mainstream.
- **Chemical reactor** multiblock with recipes as data.
- **Logistics II**: networked storage with crafting requests (deliberately not as powerful as full AE2; meant to complement it if present).

### Tier 7: Precision (EV)

- **Silicon**: arc furnace from quartz, then single-crystal boule growth.
- **Circuits**: wafers, etching, a **cleanroom** multiblock. Circuits come in a short tiered list (basic, good, advanced, elite), each with one main recipe and one alternative recipe, never seven.
- **Titanium** via a Kroll-like process (chlorination, reduction).
- **Programmable logic**: simple factory controller blocks for recipes and redstone-free control.

### Tier 8: Atomic (IV)

- **Fission reactor** multiblock with heat management (ties back to the heat system).
- **Deep core drill**: mines ore from the deep layers that no longer generate near the surface.
- **Tungsten and exotic alloys** for top-tier tools and machine casings.

### Endgame

One or two flagship megaprojects that act as a "you finished it" moment, for example a **planetary core tap** (infinite ore of any type at a high power cost) or a **foundry of the ages** (multiblock that crafts the ultimate machine casing). Final scope decided once tiers 5 to 8 exist.

---

## 5. Core mechanics

### 5.1 Geology and world generation

- **Rock strata**: replace vanilla stone in the overworld with rock types grouped into categories (sedimentary, metamorphic, igneous intrusive, igneous extrusive). Layers vary by region. Built with data-driven world-gen (density functions, noise settings and placed features via datapack JSON produced by datagen).
- **Ore veins**: large, sparse veins instead of evenly scattered ores. Each vein type lists the host rocks it can appear in, the depth range, and the ore minerals with weights. Vanilla ores are disabled in the overworld.
- **Ore grades**: poor, normal, rich. Rich ore is in vein cores.
- **Surface indicators**: stains on the ground and boulders above veins (gossan for iron, malachite bloom for copper, sulfur crust), black sand downstream of tin, indicator plants.
- **Deep layers**: below a certain depth, ore is more concentrated but needs better tools and lighting.
- **Nether and End**: get their own ore tables later (tier 5+ materials) so these dimensions remain worth visiting.
- **Compatibility switch**: a config option to keep vanilla world-gen for players who want the tech chain only.

### 5.2 Materials system

- A central **material registry** defines each material once (name, colour, melting point, density, forms it comes in, tool properties) and generates items, blocks, fluids, tags, models, lang and recipes from it via datagen. This is how a mod with dozens of metals stays maintainable.
- Forms: ore, crushed ore, washed ore, dust, small dust, nugget, ingot, plate, rod, wire, gear, molten fluid.
- **Quality**: metal items carry a craft quality (data component) from how many blows were struck bright. It nudges durability by a few percent and is never required to progress. Ore grade affects ore yield and prospecting, not quality.

### 5.3 Heat

- Items have a temperature (data component) that decays over time when not in a heat source. Tooltip shows a coloured heat band.
- Working temperature and welding temperature per material. Too cold: cannot work. Too hot: melts.
- Hot items in the inventory without tongs burn the player slightly (configurable, off by default).
- Later, heat is a machine resource moved through heat pipes, shared by machines, and produced by fireboxes, boilers and reactors.

### 5.4 Smithing

- In-world anvil striking (see tier 2). Each recipe is data: ingredient, count, result and a blow count.
- The trip hammer (tier 3), the steam hammer (tier 4) and the power hammer (tier 5) automate smithing: pick a shape with the button on the machine screen and they work it on every piece you feed them.

### 5.5 Power

- **Mechanical** (tiers 3 to 4): rotation with speed and stress capacity. Shafts, gearboxes, belts.
- **Steam** (tier 4): fluid with pressure. Can drive engines (to mechanical) or machines directly.
- **Electric** (tier 5+): own energy unit and voltage tiers; exposes the standard NeoForge energy capability so other mods' cables can connect through an adapter block.
- **Heat** (tier 4+): its own network, see 5.3.

### 5.6 Multiblocks

- Defined as data (pattern plus allowed blocks per symbol).
- Holographic ghost preview when holding the controller or the journal.
- Missing or wrong blocks are highlighted in-world.
- Tier upgrades by swapping specific blocks (casings, coils, heating elements).

### 5.7 Machines and diagnostics

- Every machine has a status line: running, no input, output full, not enough power, wrong voltage, overheating, structure incomplete.
- Recipes are data-driven custom recipe types, so packs can add or change them with plain datapacks.

### 5.8 Journal (in-game guide)

- A craftable field journal (from tier 0) with:
  - "Next goals" for the current tier, updating as you progress.
  - Entries unlocked by discovery (finding a rock, an ore, crafting a tool).
  - Multiblock previews and recipe views.
- Recipe viewer integration (EMI, with JEI as fallback) for all custom recipe types.

### 5.9 Vanilla changes (all configurable)

- Wood tools removed; punching logs does nothing.
- Vanilla metal tool and armour recipes replaced with the smithing chain.
- Vanilla furnace replaced in progression by the forge and later machines; still exists but needs fire bricks and only cooks food until tier 3.
- Vanilla ores off in the overworld (on by config).
- Food and hunger: not touched in v1. A light food spoilage system is an optional later module.

---

## 6. Anti-tedium rules

These are hard rules for every recipe and feature:

1. **No step without a reason.** If a step does not introduce a mechanic, a choice, a byproduct, or a visible world interaction, cut it.
2. **Automation before boredom.** If a hand action is needed more than about 50 times to progress, its automated version must be unlocked by then.
3. **Batch sizes scale up.** Machines in higher tiers process more per operation, not just faster.
4. **No recipe hell for intermediates.** At most two recipes for a common intermediate (main and alternative).
5. **Failure is visible and recoverable.** Wrong alloy ratio returns a usable "slag metal", overpressure vents before it breaks, overvoltage stops instead of exploding (explosions are a config option for hardcore players).
6. **The journal always shows the next goal.** A player should never need a wiki to find out what to do next.

---

## 7. Tech stack recommendation

### Decision: NeoForge, single loader, on Minecraft 26.3

**Recommended stack**

| Piece | Choice |
|---|---|
| Minecraft | Java Edition **26.3** (latest stable, September 2026). Port to 26.4 when it is released. |
| Loader | **NeoForge** (26.3 builds are available) |
| Language | **Java 25** (required by Minecraft 26.x) |
| Build | **Gradle 9** with **ModDevGradle 2** (NeoForge's official plugin), Gradle Kotlin DSL |
| Mappings | Official Mojang names (Minecraft ships unobfuscated since 26.1, so no Parchment needed) |
| Assets and data | **Datagen for everything**: models, blockstates, lang, tags, loot tables, recipes, world-gen, advancements. No hand-written JSON in `src/main/resources` beyond the mod metadata. |
| Recipes | Custom `RecipeType`s with codecs, so datapacks can change them |
| Item state | Data components (temperature, quality, contents) |
| Transfer | NeoForge capabilities (items, fluids, energy) |
| Config | NeoForge `ModConfigSpec` (common and server configs) |
| Networking | NeoForge payload networking (`CustomPacketPayload` + `StreamCodec`) |
| Rendering | Vanilla JSON models plus block entity renderers; no heavy animation library in v1 |
| Mixins | Only where no event or hook exists, kept in one package and documented |
| Recipe viewer | EMI integration (primary), JEI plugin (secondary), both optional dependencies |
| Tests | NeoForge GameTest framework for in-world behaviour, JUnit 5 for pure logic (alloy parts, smithing blow counts, vein generation math) |
| CI | GitHub Actions: build, run datagen and check for a clean diff, run GameTests headless |
| License | **MIT** (simple and permissive, common for mods; switch to LGPL-3.0 if you want forks to stay open) |
| Distribution | Modrinth and CurseForge later, from GitHub release builds |

### Why NeoForge over Fabric

- **The genre lives there.** The modern GregTech port, TerraFirmaCraft, TerraFirmaGreg, Create and most large tech and content mods target NeoForge. Players who want this kind of mod already run NeoForge packs, and compatibility with Create, AE2, Mekanism and JEI/EMI matters for a tech mod.
- **The APIs a big tech mod needs are built in.** Capabilities for item, fluid and energy transfer, a fluid API, data maps, registry helpers, config, networking and a GameTest integration all ship with the loader. On Fabric the same needs Fabric API plus the Transfer API and several community libraries.
- **Content-heavy mods benefit from NeoForge's event and hook coverage**, which means fewer mixins and easier ports between Minecraft versions.
- **Fabric's advantages** (lighter loader, faster day-one updates, strong performance-mod scene) matter less here. Sodium, Lithium and similar now ship NeoForge builds too.
- **No multi-loader.** Architectury or a common module would roughly double the porting and testing surface for little gain in this genre. Revisit only if there is real demand.

### Project layout (for the skeleton thread)

```
build.gradle.kts / settings.gradle.kts / gradle.properties
src/main/java/<package>/
  StrataIndustria.java          // @Mod entry point
  registry/                  // DeferredRegisters: blocks, items, block entities, menus, recipe types, data components, creative tabs
  material/                  // material registry and generated forms
  heat/                      // temperature data component and logic
  knapping/ smithing/ casting/ geology/ machine/ multiblock/ power/
  datagen/                   // one provider per output type, wired in GatherDataEvent
  compat/emi/ compat/jei/    // optional integrations
src/main/resources/META-INF/neoforge.mods.toml
src/generated/resources/     // datagen output, committed
src/test/java/               // JUnit
.github/workflows/build.yml
```

Package name suggestion: `dev.strataindustria` (neutral, no personal names).

---

## 8. Milestones

| Milestone | Scope | Done when |
|---|---|---|
| M0 Skeleton | Project builds, runs client and server, datagen works, one test block and item, CI green | `./gradlew build runData` clean in CI |
| M1 First playable (tiers 0 to 2) | Boulders, branches and bark cord, in-world knapping, stone tools, open fire and flint-strike fire, clay forming, firing on the fire pit, charcoal pit, forge with heat, crucible smelting and alloying, casting, stone anvil smithing, bronze tools and armour, surface indicators and prospector's pick, journal with tier 0 to 2 goals. World-gen: 6 to 8 rock types and copper, tin and bismuth veins. | A new player can go from spawn to a full bronze kit without a wiki in 3 to 5 hours |
| M2 Iron and mechanical (tier 3) | Bloomery, bloom refining, water wheel and windmill, shafts and gearboxes, mechanical quern, saw mill, trip hammer, bellows, sluice | Playtest pass on M1 + M2 |
| M3 Steel and steam (tier 4) | Blast furnace, coke oven, boilers, steam engines, heat pipes, fluids, conveyors, ore processing II | |
| M4+ | Electric and beyond | |

For M1 the world-gen can start simpler (strata only underground, no full overworld rewrite) as long as rock types and veins work, then be expanded in M2.

---

## 9. Open questions

1. Working title and mod id: keep "Strata Industria" or pick another before the first public release?
2. Should food spoilage and temperature-based survival (TFC-style) be part of the core, or an optional module? Default in this doc: not in v1.
3. How strict should vanilla gating be by default? Default in this doc: strict (wood tools removed, vanilla ores off), with config switches.
4. Should the mod ship its own world type, or always replace overworld generation? Default: replace overworld generation, with a config to disable.
