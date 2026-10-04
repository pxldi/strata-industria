package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.JournalContent;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.registry.Tier6Sounds;
import dev.strataindustria.structure.StructureContent;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/** sounds.json. Our sounds are built from vanilla stone samples, pitched to suit each action. */
final class ModSoundsProvider extends SoundDefinitionsProvider {
    ModSoundsProvider(PackOutput output) {
        super(output, StrataIndustria.MOD_ID);
    }

    @Override
    public void registerSounds() {
        tier5();
        tier6();
        // A dull chip off a rock.
        add(ModSounds.KNAP_ROCK, stone("knapping.rock", 1.15f, 0.9f));
        // Flint rings sharper and higher.
        add(ModSounds.KNAP_FLINT, stone("knapping.flint", 1.7f, 0.8f));
        // The last flake falls and the head comes free: a deeper knock.
        add(ModSounds.KNAP_FINISH, stone("knapping.finish", 0.75f, 1.0f));

        // Fire: wood friction for the drill, then vanilla fire samples.
        SoundDefinition drill = definition().subtitle(subtitle("firestarter.drill"));
        for (int i = 1; i <= 4; i++) drill.with(sound("minecraft:step/wood" + i).pitch(1.6f).volume(0.5f));
        add(ModSounds.FIRESTARTER_DRILL, drill);
        add(ModSounds.FIRE_PIT_IGNITE, definition().subtitle(subtitle("fire_pit.ignite"))
                .with(sound("minecraft:fire/ignite").pitch(0.9f))
                .with(sound("minecraft:fire/fire").pitch(1.2f).volume(0.6f)));
        add(ModSounds.FIRE_PIT_EXTINGUISH, definition().subtitle(subtitle("fire_pit.extinguish"))
                .with(sound("minecraft:random/fizz")));
        add(ModSounds.FIRE_PIT_TORCH, definition().subtitle(subtitle("fire_pit.torch"))
                .with(sound("minecraft:fire/ignite").pitch(1.3f).volume(0.7f)));

        // Clay: wet squelches for shaping, then the kiln's thatch, logs and the cooling crackle.
        SoundDefinition shape = definition().subtitle(subtitle("clay.shape"));
        for (int i = 1; i <= 5; i++) shape.with(sound("minecraft:mob/slime/small" + i).pitch(0.8f).volume(0.35f));
        add(ModSounds.CLAY_SHAPE, shape);
        SoundDefinition finish = definition().subtitle(subtitle("clay.finish"));
        for (int i = 1; i <= 3; i++) finish.with(sound("minecraft:mob/slime/big" + i).pitch(1.1f).volume(0.5f));
        add(ModSounds.CLAY_FINISH, finish);
        SoundDefinition straw = definition().subtitle(subtitle("pit_kiln.straw"));
        for (int i = 1; i <= 6; i++) straw.with(sound("minecraft:step/grass" + i).pitch(0.9f));
        add(ModSounds.KILN_STRAW, straw);
        SoundDefinition log = definition().subtitle(subtitle("pit_kiln.log"));
        for (int i = 1; i <= 4; i++) log.with(sound("minecraft:dig/wood" + i).pitch(0.8f));
        add(ModSounds.KILN_LOG, log);
        add(ModSounds.SEAR, definition().subtitle(subtitle("heat.sear"))
                .with(sound("minecraft:random/fizz").pitch(1.8f).volume(0.4f))
                .with(sound("minecraft:liquid/lavapop").pitch(1.5f).volume(0.5f)));
        add(ModSounds.QUENCH, definition().subtitle(subtitle("heat.quench"))
                .with(sound("minecraft:random/fizz").pitch(1.4f).volume(0.6f))
                .with(sound("minecraft:random/fizz").pitch(1.7f).volume(0.5f)));
        add(ModSounds.FORGE_IGNITE, definition().subtitle(subtitle("forge.ignite"))
                .with(sound("minecraft:fire/ignite").pitch(0.7f))
                .with(sound("minecraft:fire/fire").pitch(0.8f).volume(0.8f)));
        // Metal: lava pops for a piece slumping into the melt, a poured bucket of lava, and fired clay
        // knocking or cracking.
        add(ModSounds.CRUCIBLE_MELT, definition().subtitle(subtitle("crucible.melt"))
                .with(sound("minecraft:liquid/lavapop").pitch(0.8f).volume(0.7f))
                .with(sound("minecraft:liquid/lavapop").pitch(1.1f).volume(0.6f)));
        SoundDefinition pour = definition().subtitle(subtitle("crucible.pour"));
        for (int i = 1; i <= 3; i++) pour.with(sound("minecraft:item/bucket/empty_lava" + i).pitch(1.2f).volume(0.8f));
        add(ModSounds.CRUCIBLE_POUR, pour);
        add(ModSounds.MOLD_KNOCK, stone("mold.knock", 1.4f, 0.8f));
        SoundDefinition crack = definition().subtitle(subtitle("mold.break"));
        for (int i = 1; i <= 3; i++) crack.with(sound("minecraft:random/glass" + i).pitch(0.6f).volume(0.7f));
        add(ModSounds.MOLD_BREAK, crack);
        // Quern: gravelly stone-on-stone for the grind, a soft scrape for loading and a crunch when done.
        SoundDefinition grind = definition().subtitle(subtitle("quern.grind"));
        for (int i = 1; i <= 4; i++) grind.with(sound("minecraft:step/gravel" + i).pitch(0.6f).volume(0.7f));
        for (int i = 1; i <= 4; i++) grind.with(sound("minecraft:dig/stone" + i).pitch(0.5f).volume(0.35f));
        add(ModSounds.QUERN_GRIND, grind);
        SoundDefinition load = definition().subtitle(subtitle("quern.load"));
        for (int i = 1; i <= 4; i++) load.with(sound("minecraft:step/gravel" + i).pitch(1.3f).volume(0.5f));
        add(ModSounds.QUERN_LOAD, load);
        SoundDefinition done = definition().subtitle(subtitle("quern.done"));
        for (int i = 1; i <= 4; i++) done.with(sound("minecraft:dig/gravel" + i).pitch(1.2f).volume(0.6f));
        add(ModSounds.QUERN_DONE, done);
        // Smithing: a bright ring for each blow, a heavier ring to finish, stone chips for dressing an anvil.
        add(ModSounds.SMITH_HIT, definition().subtitle(subtitle("anvil.hit"))
                .with(sound("minecraft:random/anvil_use").pitch(1.3f).volume(0.5f)));
        add(ModSounds.SMITH_DONE, definition().subtitle(subtitle("anvil.done"))
                .with(sound("minecraft:random/anvil_land").pitch(1.4f).volume(0.5f)));
        SoundDefinition dress = definition().subtitle(subtitle("anvil.dress"));
        for (int i = 1; i <= 4; i++) dress.with(sound("minecraft:dig/stone" + i).pitch(0.7f));
        add(ModSounds.ANVIL_DRESS, dress);
        add(ModSounds.PROSPECT, stone("prospect", 1.9f, 0.6f));
        // The journal: pages flipping open.
        SoundDefinition pages = definition().subtitle(subtitle("journal.open"));
        for (int i = 1; i <= 3; i++) pages.with(sound("minecraft:item/book/open_flip" + i).volume(0.8f));
        add(ModSounds.JOURNAL_OPEN, pages);
        journal();

        // Tier 3 bloomery (spec 20.6): a whoosh of catching charcoal, a low draught roar, ore on coals, and
        // a metallic crunch as a bloom comes out.
        add(ModSounds.BLOOMERY_LIGHT, definition().subtitle(subtitle("bloomery.light"))
                .with(sound("minecraft:mob/ghast/fireball4").pitch(0.8f).volume(0.6f))
                .with(sound("minecraft:fire/ignite").pitch(0.7f)));
        SoundDefinition roar = definition().subtitle(subtitle("bloomery.roar"));
        roar.with(sound("minecraft:fire/fire").pitch(0.6f).volume(0.9f));
        for (int i = 1; i <= 2; i++) roar.with(sound("minecraft:block/furnace/fire_crackle" + i).pitch(0.6f).volume(0.8f));
        add(ModSounds.BLOOMERY_ROAR, roar);
        SoundDefinition charge = definition().subtitle(subtitle("bloomery.charge"));
        for (int i = 1; i <= 3; i++) charge.with(sound("minecraft:dig/gravel" + i).pitch(0.9f).volume(0.8f));
        add(ModSounds.BLOOMERY_CHARGE, charge);
        add(ModSounds.BLOOMERY_DONE, definition().subtitle(subtitle("bloomery.done"))
                .with(sound("minecraft:liquid/lavapop").pitch(0.6f))
                .with(sound("minecraft:random/fizz").pitch(0.7f).volume(0.6f)));
        SoundDefinition extract = definition().subtitle(subtitle("bloomery.extract"));
        for (int i = 1; i <= 3; i++) extract.with(sound("minecraft:random/anvil_land").pitch(0.7f).volume(0.4f));
        extract.with(sound("minecraft:random/fizz").pitch(1.2f).volume(0.5f));
        add(ModSounds.BLOOMERY_EXTRACT, extract);
        SoundDefinition bloomHit = definition().subtitle(subtitle("raw_bloom.hit"));
        for (int i = 1; i <= 4; i++) bloomHit.with(sound("minecraft:random/anvil_use").pitch(0.6f).volume(0.6f));
        bloomHit.with(sound("minecraft:liquid/lavapop").pitch(1.4f).volume(0.4f));
        add(ModSounds.RAW_BLOOM_HIT, bloomHit);
        add(ModSounds.WROUGHT_IRON_HIT, definition().subtitle(subtitle("wrought_iron.hit"))
                .with(sound("minecraft:random/anvil_use").pitch(0.85f).volume(0.8f)));
        // Spec 9.4: a weld is two heavy blows close together with a hiss of flux; a refusal is a dull click.
        add(ModSounds.ANVIL_WELD, definition().subtitle(subtitle("anvil.weld"))
                .with(sound("minecraft:random/anvil_use").pitch(1.3f).volume(0.7f))
                .with(sound("minecraft:random/anvil_land").pitch(1.1f).volume(0.4f)));
        add(ModSounds.ANVIL_WELD_FAIL, definition().subtitle(subtitle("anvil.weld_fail"))
                .with(sound("minecraft:random/click").pitch(0.6f).volume(0.5f)));
        // Spec 20.6: mechanical power. These reuse vanilla events, retuned.
        add(ModSounds.HAND_CRANK_TURN, definition().subtitle(subtitle("hand_crank.turn"))
                .with(sound("minecraft:block.wooden_door.open", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.4f)));
        add(ModSounds.WATER_WHEEL_TURN, definition().subtitle(subtitle("water_wheel.turn"))
                .with(sound("minecraft:entity.boat.paddle_water", SoundDefinition.SoundType.EVENT).volume(0.7f)));
        add(ModSounds.KINETIC_OVERSTRESS, definition().subtitle(subtitle("kinetic.overstress"))
                .with(sound("minecraft:block.wood.break", SoundDefinition.SoundType.EVENT).pitch(0.5f)));
        add(ModSounds.MILLSTONE_GRIND, definition().subtitle(subtitle("millstone.grind"))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.6f)));
        add(ModSounds.BELLOWS_PUMP, definition().subtitle(subtitle("bellows.pump"))
                .with(sound("minecraft:entity.horse.breathe", SoundDefinition.SoundType.EVENT).pitch(0.7f)));
        add(ModSounds.SAW_MILL_SAW, definition().subtitle(subtitle("saw_mill.saw"))
                .with(sound("minecraft:entity.sheep.shear", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.6f)));
        add(ModSounds.SAW_MILL_BLADE_BREAK, definition().subtitle(subtitle("saw_mill.blade_break"))
                .with(sound("minecraft:entity.item.break", SoundDefinition.SoundType.EVENT)));
        add(ModSounds.CORE_SAMPLER_DRILL, definition().subtitle(subtitle("core_sampler.drill"))
                .with(sound("minecraft:block.stone.hit", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.7f))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.25f)));
        add(ModSounds.CORE_SAMPLER_DONE, definition().subtitle(subtitle("core_sampler.done"))
                .with(sound("minecraft:block.note_block.bell", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.4f)));
        add(ModSounds.WINDMILL_TURN, definition().subtitle(subtitle("windmill.turn"))
                .with(sound("minecraft:block.wood.step", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.6f))
                .with(sound("minecraft:block.wool.step", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.4f)));
        add(ModSounds.BELT_ATTACH, definition().subtitle(subtitle("belt.attach"))
                .with(sound("minecraft:entity.leash_knot.place", SoundDefinition.SoundType.EVENT)));
        add(ModSounds.SOAKING_BARREL_SEAL, definition().subtitle(subtitle("soaking_barrel.seal"))
                .with(sound("minecraft:block.barrel.close", SoundDefinition.SoundType.EVENT).pitch(0.85f)));
        add(ModSounds.SOAKING_BARREL_OPEN, definition().subtitle(subtitle("soaking_barrel.open"))
                .with(sound("minecraft:block.barrel.open", SoundDefinition.SoundType.EVENT).pitch(0.85f)));
        add(ModSounds.SOAKING_BARREL_FILL, definition().subtitle(subtitle("soaking_barrel.fill"))
                .with(sound("minecraft:item.bucket.empty", SoundDefinition.SoundType.EVENT).pitch(0.9f)));
        add(ModSounds.SOAKING_BARREL_DONE, definition().subtitle(subtitle("soaking_barrel.done"))
                .with(sound("minecraft:block.bubble_column.upwards_inside", SoundDefinition.SoundType.EVENT).volume(0.6f).pitch(0.8f)));
        add(ModSounds.HIDE_SCRAPE, definition().subtitle(subtitle("hide.scrape"))
                .with(sound("minecraft:entity.sheep.shear", SoundDefinition.SoundType.EVENT).pitch(1.2f)));
        add(ModSounds.SLUICE_WASH, definition().subtitle(subtitle("sluice.wash"))
                .with(sound("minecraft:block.water.ambient", SoundDefinition.SoundType.EVENT).volume(0.6f)));
        add(ModSounds.WASHING_PAN_SWIRL, definition().subtitle(subtitle("washing_pan.swirl"))
                .with(sound("minecraft:item.bucket.fill", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.5f))
                .with(sound("minecraft:block.gravel.step", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.4f)));
        add(ModSounds.WASHING_PAN_FIND, definition().subtitle(subtitle("washing_pan.find"))
                .with(sound("minecraft:entity.experience_orb.pickup", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.4f)));
        add(ModSounds.CORE_SAMPLE_OPEN, definition().subtitle(subtitle("core_sample.open"))
                .with(sound("minecraft:item.book.page_turn", SoundDefinition.SoundType.EVENT).pitch(0.8f)));
        add(ModSounds.KILN_FIRED, definition().subtitle(subtitle("pit_kiln.fired"))
                .with(sound("minecraft:random/fizz").pitch(0.6f).volume(0.7f)));

        // Structures: an old mine timber groaning, stiff paper, and a pencil note in the journal.
        SoundDefinition creak = definition().subtitle(subtitle("pit_prop.creak"));
        for (int i = 1; i <= 4; i++) creak.with(sound("minecraft:block/scaffold/place" + i).pitch(0.55f).volume(0.6f));
        creak.with(sound("minecraft:random/door_open").pitch(0.5f).volume(0.35f));
        add(StructureContent.PIT_PROP_CREAK, creak);
        SoundDefinition unfold = definition().subtitle(subtitle("survey_notes.open"));
        for (int i = 1; i <= 3; i++) unfold.with(sound("minecraft:item/book/open_flip" + i).pitch(1.25f).volume(0.7f));
        add(StructureContent.SURVEY_NOTES_OPEN, unfold);
        add(StructureContent.SURVEY_NOTES_FOUND, definition().subtitle(subtitle("survey_notes.found"))
                .with(sound("minecraft:note/icechime").pitch(0.8f).volume(0.35f))
                .with(sound("minecraft:item/book/open_flip1").pitch(1.1f).volume(0.6f)));
        SoundDefinition place = definition().subtitle(subtitle("journal.place"));
        for (int i = 1; i <= 3; i++) place.with(sound("minecraft:item/book/open_flip" + i).pitch(0.9f).volume(0.7f));
        add(StructureContent.JOURNAL_PLACE, place);

        // Ruins: heat-cracked brick crumbling, and slag that crunches like gravel with a glassy clink.
        add(StructureContent.CRACKED_FIRE_BRICKS_BREAK, definition().subtitle(subtitle("cracked_fire_bricks.break"))
                .with(sound("minecraft:block.decorated_pot.shatter", SoundDefinition.SoundType.EVENT).pitch(0.8f).weight(2))
                .with(sound("minecraft:block.deepslate_bricks.break", SoundDefinition.SoundType.EVENT).pitch(0.95f)));
        add(StructureContent.SLAG_HEAP_BREAK, definition().subtitle(subtitle("slag_heap.break"))
                .with(sound("minecraft:block.gravel.break", SoundDefinition.SoundType.EVENT).pitch(0.9f).weight(3))
                .with(sound("minecraft:block.glass.break", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.3f)));
        add(StructureContent.SLAG_HEAP_STEP, definition().subtitle("subtitles.block.generic.footsteps")
                .with(sound("minecraft:block.gravel.step", SoundDefinition.SoundType.EVENT).weight(3))
                .with(sound("minecraft:block.amethyst_block.step", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.3f)));

        // Tier 4 coke oven (spec 21): a low smouldering crackle, coke clinking down when a charge is done,
        // and thick oil glugging into a bucket.
        SoundDefinition smoulder = definition().subtitle(subtitle("coke_oven.working"));
        for (int i = 1; i <= 2; i++) smoulder.with(sound("minecraft:block/furnace/fire_crackle" + i).pitch(0.5f).volume(0.7f));
        smoulder.with(sound("minecraft:fire/fire").pitch(0.45f).volume(0.5f));
        add(Tier4Sounds.COKE_OVEN_WORKING, smoulder);
        add(Tier4Sounds.COKE_OVEN_DONE, definition().subtitle(subtitle("coke_oven.done"))
                .with(sound("minecraft:block.basalt.place", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.8f))
                .with(sound("minecraft:block.basalt.break", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.6f)));
        // Spare carbon flaring off a melt: a soft sputtering whoosh.
        add(Tier4Sounds.CARBON_BURN, definition().subtitle(subtitle("crucible.carbon_burn"))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.4f))
                .with(sound("minecraft:item.firecharge.use", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.3f)));
        add(Tier4Sounds.CREOSOTE_FILL, definition().subtitle(subtitle("creosote.fill"))
                .with(sound("minecraft:item.bucket.fill_lava", SoundDefinition.SoundType.EVENT).pitch(0.8f))
                .with(sound("minecraft:block.honey_block.slide", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.6f)));
        // Spec 21: ore roasting hisses and fizzes; the finished calcine settles with a dry crumble.
        add(Tier4Sounds.ROASTING_SIZZLE, definition().subtitle(subtitle("roasting.sizzle"))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.3f))
                .with(sound("minecraft:block.lava.pop", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.4f)));
        add(Tier4Sounds.ROASTING_DONE, definition().subtitle(subtitle("roasting.done"))
                .with(sound("minecraft:block.suspicious_gravel.break", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.7f))
                .with(sound("minecraft:block.tuff.break", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.6f)));
        // Calcine giving up its zinc: a bubbling, gassy hiss.
        add(Tier4Sounds.CALCINE_REDUCE, definition().subtitle(subtitle("crucible.calcine_reduce"))
                .with(sound("minecraft:block.bubble_column.upwards_inside", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.5f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.3f)));

        // Spec 21.7: the firebox flares and crackles; a refused pipe clanks and hisses.
        add(Tier4Sounds.FIREBOX_LIGHT, definition().subtitle(subtitle("firebox.light"))
                .with(sound("minecraft:item.firecharge.use", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.7f))
                .with(sound("minecraft:item.firecharge.use", SoundDefinition.SoundType.EVENT).pitch(1.1f).volume(0.6f)));
        SoundDefinition burn = definition().subtitle(subtitle("firebox.burn"));
        for (int i = 1; i <= 3; i++) burn.with(sound("minecraft:block/furnace/fire_crackle" + i).pitch(0.85f).volume(0.8f));
        burn.with(sound("minecraft:fire/fire").pitch(0.7f).volume(0.4f));
        add(Tier4Sounds.FIREBOX_BURN, burn);
        SoundDefinition kilnWork = definition().subtitle(subtitle("kiln.work"));
        for (int i = 1; i <= 3; i++) kilnWork.with(sound("minecraft:block/furnace/fire_crackle" + i).pitch(0.7f).volume(0.6f));
        add(Tier4Sounds.KILN_WORK, kilnWork);
        add(Tier4Sounds.KILN_DONE, definition().subtitle(subtitle("kiln.done"))
                .with(sound("minecraft:block.decorated_pot.hit", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.7f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.25f)));
        // Spec 21: the smelter's pour is thicker than a crucible's, and its pot bubbles while molten.
        add(Tier4Sounds.SMELTER_POUR, definition().subtitle(subtitle("smelter.pour"))
                .with(sound("minecraft:item.bucket.empty_lava", SoundDefinition.SoundType.EVENT).pitch(0.85f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.3f)));
        add(Tier4Sounds.SMELTER_BUBBLE, definition().subtitle(subtitle("smelter.bubble"))
                .with(sound("minecraft:block.lava.ambient", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.7f))
                .with(sound("minecraft:block.lava.pop", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.4f)));
        add(Tier4Sounds.HEAT_PIPE_TICK, definition().subtitle(subtitle("heat_pipe.tick"))
                .with(sound("minecraft:block.copper.step", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.35f))
                .with(sound("minecraft:block.chain.step", SoundDefinition.SoundType.EVENT).pitch(1.7f).volume(0.25f)));
        add(Tier4Sounds.FLUID_PIPE_REFUSE, definition().subtitle(subtitle("fluid_pipe.refuse"))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.3f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.3f)));
        // Spec 21.7: the boiler. Warming creaks, a deep rumble while it makes steam, the safety valve's
        // whistle, warnings, and the bang and split of a dry-fired shell.
        add(Tier4Sounds.BOILER_HEAT, definition().subtitle(subtitle("boiler.heat"))
                .with(sound("minecraft:block.fire.ambient", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.7f))
                .with(sound("minecraft:block.chain.step", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.4f)));
        add(Tier4Sounds.BOILER_RUN, definition().subtitle(subtitle("boiler.run"))
                .with(sound("minecraft:block.lava.ambient", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.8f))
                .with(sound("minecraft:block.lava.ambient", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.7f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.15f)));
        add(Tier4Sounds.BOILER_VENT, definition().subtitle(subtitle("boiler.vent"))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(0.5f))
                .with(sound("minecraft:block.note_block.flute", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.6f)));
        add(Tier4Sounds.BOILER_LOW_WATER, definition().subtitle(subtitle("boiler.low_water"))
                .with(sound("minecraft:block.note_block.didgeridoo", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.5f)));
        add(Tier4Sounds.BOILER_DRY_FIRE, definition().subtitle(subtitle("boiler.dry_fire"))
                .with(sound("minecraft:block.chain.step", SoundDefinition.SoundType.EVENT).pitch(0.5f))
                .with(sound("minecraft:block.chain.break", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.6f)));
        add(Tier4Sounds.BOILER_STEAM_BURST, definition().subtitle(subtitle("boiler.steam_burst"))
                .with(sound("minecraft:entity.generic.extinguish_fire", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(1.5f)));
        add(Tier4Sounds.BOILER_CRACK, definition().subtitle(subtitle("boiler.crack"))
                .with(sound("minecraft:entity.item.break", SoundDefinition.SoundType.EVENT).pitch(0.5f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(0.7f)));
        add(Tier4Sounds.BOILER_REPAIR, definition().subtitle(subtitle("boiler.repair"))
                .with(sound("minecraft:block.anvil.use", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.7f))
                .with(sound("minecraft:block.anvil.use", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.6f)));
        // Spec 21.7: the engine's chuff and knock, its start and stop, and the pump's suck and thump.
        add(Tier4Sounds.STEAM_ENGINE_CHUFF, definition().subtitle(subtitle("steam_engine.chuff"))
                .with(sound("minecraft:block.piston.contract", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.6f))
                .with(sound("minecraft:block.piston.contract", SoundDefinition.SoundType.EVENT).pitch(0.65f).volume(0.55f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.25f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.2f)));
        // Spec 21.7: the valve's wheel squeak and thunk, and liquid poured into a metal tank.
        add(Tier4Sounds.VALVE_OPEN, definition().subtitle(subtitle("valve.open"))
                .with(sound("minecraft:block.iron_trapdoor.open", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.7f))
                .with(sound("minecraft:block.iron_trapdoor.open", SoundDefinition.SoundType.EVENT).pitch(1.1f).volume(0.7f)));
        add(Tier4Sounds.VALVE_CLOSE, definition().subtitle(subtitle("valve.close"))
                .with(sound("minecraft:block.iron_trapdoor.close", SoundDefinition.SoundType.EVENT).pitch(1.0f).volume(0.7f))
                .with(sound("minecraft:block.iron_trapdoor.close", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.7f)));
        add(Tier4Sounds.FLUID_TANK_FILL, definition().subtitle(subtitle("fluid_tank.fill"))
                .with(sound("minecraft:item.bucket.empty", SoundDefinition.SoundType.EVENT).pitch(0.8f))
                .with(sound("minecraft:item.bucket.empty", SoundDefinition.SoundType.EVENT).pitch(0.7f)));
        add(Tier4Sounds.BLOWING_ENGINE_STROKE, definition().subtitle(subtitle("blowing_engine.stroke"))
                .with(sound("minecraft:block.piston.contract", SoundDefinition.SoundType.EVENT).pitch(0.55f).volume(0.6f))
                .with(sound("minecraft:block.piston.contract", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.55f))
                .with(sound("minecraft:entity.breeze.wind_burst", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.25f)));
        // Spec 21.7: a hiss, then the heavy ringing blow.
        add(Tier4Sounds.STEAM_HAMMER_STRIKE, definition().subtitle(subtitle("steam_hammer.strike"))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.6f))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(0.75f).volume(0.55f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.25f)));
        add(Tier4Sounds.STEAM_ENGINE_START, definition().subtitle(subtitle("steam_engine.start"))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.7f)));
        add(Tier4Sounds.STEAM_ENGINE_STOP, definition().subtitle(subtitle("steam_engine.stop"))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.6f)));
        add(Tier4Sounds.MECHANICAL_PUMP_RUN, definition().subtitle(subtitle("mechanical_pump.run"))
                .with(sound("minecraft:block.piston.extend", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.35f))
                .with(sound("minecraft:block.piston.extend", SoundDefinition.SoundType.EVENT).pitch(0.55f).volume(0.3f))
                .with(sound("minecraft:block.bubble_column.upwards_inside", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.3f)));
        // Spec 21.7: rock cracking between iron jaws.
        add(Tier4Sounds.CRUSHER_CRUSH, definition().subtitle(subtitle("crusher.crush"))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.6f))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.45f).volume(0.55f))
                .with(sound("minecraft:block.stone.break", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.5f)));
        // Spec 21.7: a sloshing drum of wet gravel.
        add(Tier4Sounds.WASHER_WASH, definition().subtitle(subtitle("washer.wash"))
                .with(sound("minecraft:block.water.ambient", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.5f))
                .with(sound("minecraft:block.water.ambient", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.45f))
                .with(sound("minecraft:block.gravel.hit", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.4f)));
        // Spec 21.7: the blower's fan, the blast furnace's roar and tap, and the clunk of a finished multiblock.
        add(Tier4Sounds.BLOWER_RUN, definition().subtitle(subtitle("blower.run"))
                .with(sound("minecraft:item.elytra.flying", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.12f))
                .with(sound("minecraft:entity.horse.breathe", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.3f)));
        SoundDefinition furnaceRoar = definition().subtitle(subtitle("blast_furnace.roar"));
        for (int i = 1; i <= 3; i++) furnaceRoar.with(sound("minecraft:block/furnace/fire_crackle" + i).pitch(0.4f).volume(1.0f));
        furnaceRoar.with(sound("minecraft:fire/fire").pitch(0.35f).volume(0.9f));
        add(Tier4Sounds.BLAST_FURNACE_ROAR, furnaceRoar);
        add(Tier4Sounds.BLAST_FURNACE_TAP, definition().subtitle(subtitle("blast_furnace.tap"))
                .with(sound("minecraft:item.bucket.empty_lava", SoundDefinition.SoundType.EVENT).pitch(0.8f))
                .with(sound("minecraft:item.bucket.empty_lava", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.9f)));
        add(Tier4Sounds.CONVERTER_BLOW, definition().subtitle(subtitle("converter.blow"))
                .with(sound("minecraft:entity.blaze.burn", SoundDefinition.SoundType.EVENT).pitch(0.5f))
                .with(sound("minecraft:entity.blaze.burn", SoundDefinition.SoundType.EVENT).pitch(0.45f).volume(0.9f))
                .with(sound("minecraft:item.firecharge.use", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.6f)));
        add(Tier4Sounds.CONVERTER_DONE, definition().subtitle(subtitle("converter.done"))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.8f)));
        // Spec 21.7: a metal clatter as an item leaves a chute, and a paper rustle as a filter is set.
        add(Tier4Sounds.CHUTE_DROP, definition().subtitle(subtitle("chute.drop"))
                .with(sound("minecraft:block.chain.hit", SoundDefinition.SoundType.EVENT).pitch(1.1f).volume(0.5f))
                .with(sound("minecraft:block.chain.hit", SoundDefinition.SoundType.EVENT).pitch(1.25f).volume(0.45f)));
        add(Tier4Sounds.INSERTER_SWING, definition().subtitle(subtitle("block.inserter.swing"))
                .with(sound("minecraft:block.dispenser.dispense", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.4f))
                .with(sound("minecraft:block.chain.hit", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.3f)));
        add(Tier4Sounds.CONVEYOR_RUN, definition().subtitle(subtitle("block.conveyor.run"))
                .with(sound("minecraft:block.wool.step", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.5f))
                .with(sound("minecraft:block.wool.step", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.45f))
                .with(sound("minecraft:block.wool.step", SoundDefinition.SoundType.EVENT).pitch(0.55f).volume(0.5f)));
        add(Tier4Sounds.BELT_DIVERTER_PUSH, definition().subtitle(subtitle("block.belt_diverter.push"))
                .with(sound("minecraft:block.wooden_button.click_on", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.6f))
                .with(sound("minecraft:block.wooden_button.click_on", SoundDefinition.SoundType.EVENT).pitch(0.95f).volume(0.55f))
                .with(sound("minecraft:block.chain.hit", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.25f)));
        add(Tier4Sounds.FILTER_CONFIGURE, definition().subtitle(subtitle("filter.configure"))
                .with(sound("minecraft:item.book.page_turn", SoundDefinition.SoundType.EVENT).pitch(1.1f))
                .with(sound("minecraft:item.book.page_turn", SoundDefinition.SoundType.EVENT).pitch(1.25f)));
        add(Tier4Sounds.MULTIBLOCK_FORM, definition().subtitle(subtitle("multiblock.form"))
                .with(sound("minecraft:block.iron_door.close", SoundDefinition.SoundType.EVENT).pitch(0.7f)));
    }

    /** Tier 6 spec 24.6: crude oil buckets use the lava bucket sounds, pitched down and thickened. */
    private void tier6() {
        add(Tier6Sounds.CRUDE_OIL_BUCKET_FILL, definition().subtitle(subtitle("item.bucket.fill_crude_oil"))
                .with(sound("minecraft:item.bucket.fill_lava", SoundDefinition.SoundType.EVENT).pitch(0.8f))
                .with(sound("minecraft:block.honey_block.slide", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.5f)));
        add(Tier6Sounds.CRUDE_OIL_BUCKET_EMPTY, definition().subtitle(subtitle("item.bucket.empty_crude_oil"))
                .with(sound("minecraft:item.bucket.empty_lava", SoundDefinition.SoundType.EVENT).pitch(0.8f))
                .with(sound("minecraft:block.mud.place", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.6f)));
        // A slow, thick bubble breaking on a seep.
        add(Tier6Sounds.CRUDE_OIL_BUBBLE, definition().subtitle(subtitle("block.crude_oil.bubble"))
                .with(sound("minecraft:block.bubble_column.bubble_pop", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.6f))
                .with(sound("minecraft:block.lava.pop", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.3f))
                .with(sound("minecraft:block.honey_block.slide", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.3f)));
    }

    /** The leads notebook: a pencil on paper, pages, and a tack into cork, all short and quiet. */
    private void journal() {
        add(JournalContent.WRITE, brush("journal.write", 1.5f, 0.4f));
        add(JournalContent.CROSS_OFF, brush("journal.cross_off", 1.7f, 0.5f));
        add(JournalContent.STUDY, brush("journal.study", 1.05f, 0.45f));
        SoundDefinition remember = definition().subtitle(subtitle("journal.remember"));
        for (int i = 1; i <= 3; i++) remember.with(sound("minecraft:item/book/open_flip" + i).pitch(0.75f).volume(0.8f));
        add(JournalContent.REMEMBER, remember);
        SoundDefinition page = definition().subtitle(subtitle("journal.page"));
        for (int i = 1; i <= 3; i++) page.with(sound("minecraft:item/book/open_flip" + i).pitch(1.05f).volume(0.7f));
        add(JournalContent.PAGE, page);
        SoundDefinition pin = definition().subtitle(subtitle("journal.pin"));
        for (int i = 1; i <= 4; i++) pin.with(sound("minecraft:dig/wood" + i).pitch(1.9f).volume(0.35f));
        add(JournalContent.PIN, pin);
    }

    /** A brush on paper reads as a pencil when pitched up. */
    private static SoundDefinition brush(String subtitle, float pitch, float volume) {
        SoundDefinition definition = definition().subtitle(subtitle(subtitle));
        for (int i = 1; i <= 4; i++) definition.with(sound("minecraft:item/brush/brushing_generic" + i).pitch(pitch).volume(volume));
        return definition;
    }

    private static String subtitle(String name) {
        return "subtitles." + StrataIndustria.MOD_ID + "." + name;
    }

    private static SoundDefinition stone(String subtitle, float pitch, float volume) {
        SoundDefinition definition = definition().subtitle("subtitles." + StrataIndustria.MOD_ID + "." + subtitle);
        for (int i = 1; i <= 4; i++) {
            definition.with(sound("minecraft:dig/stone" + i).pitch(pitch).volume(volume));
        }
        return definition;
    }

    // Tier 5 spec 23.6: electric sounds, built from vanilla samples.
    private void tier5() {
        add(Tier5Sounds.ELECTRIC_OVERVOLTAGE, definition().subtitle(subtitle("electric.overvoltage"))
                .with(sound("minecraft:entity.lightning_bolt.impact", SoundDefinition.SoundType.EVENT).pitch(2.0f).volume(0.3f))
                .with(sound("minecraft:entity.lightning_bolt.impact", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.25f)));
        add(Tier5Sounds.ELECTRIC_SPARK, definition().subtitle(subtitle("electric.spark"))
                .with(sound("minecraft:block.redstone_torch.burnout", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.3f))
                .with(sound("minecraft:block.redstone_torch.burnout", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.25f))
                .with(sound("minecraft:block.redstone_torch.burnout", SoundDefinition.SoundType.EVENT).pitch(1.45f).volume(0.3f)));
        add(Tier5Sounds.KINETIC_DYNAMO_RUN, definition().subtitle(subtitle("kinetic_dynamo.run"))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.4f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.35f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.35f)));
        add(Tier5Sounds.TREE_TAP_PLACE, definition().subtitle(subtitle("block.tree_tap.place"))
                .with(sound("minecraft:block.wood.place", SoundDefinition.SoundType.EVENT).pitch(1.3f))
                .with(sound("minecraft:block.copper.place", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.6f)));
        add(Tier5Sounds.TREE_TAP_DRIP, definition().subtitle(subtitle("block.tree_tap.drip"))
                .with(sound("minecraft:block.pointed_dripstone.drip_water", SoundDefinition.SoundType.EVENT).pitch(0.7f))
                .with(sound("minecraft:block.pointed_dripstone.drip_water", SoundDefinition.SoundType.EVENT).pitch(0.62f).volume(0.9f))
                .with(sound("minecraft:block.pointed_dripstone.drip_water_into_cauldron", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.7f)));
        add(Tier5Sounds.POLE_INSULATOR_CONNECT, definition().subtitle(subtitle("block.pole_insulator.connect"))
                .with(sound("minecraft:entity.fishing_bobber.throw", SoundDefinition.SoundType.EVENT).pitch(0.6f))
                .with(sound("minecraft:block.chain.place", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.7f)));
        add(Tier5Sounds.POLE_INSULATOR_DISCONNECT, definition().subtitle(subtitle("block.pole_insulator.disconnect"))
                .with(sound("minecraft:entity.leash_knot.break", SoundDefinition.SoundType.EVENT))
                .with(sound("minecraft:entity.leash_knot.break", SoundDefinition.SoundType.EVENT).pitch(0.85f)));
        add(Tier5Sounds.MACHINE_POWER_ON, definition().subtitle(subtitle("block.machine.power_on"))
                .with(sound("minecraft:block.lever.click", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.6f))
                .with(sound("minecraft:block.beacon.activate", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.25f)));
        add(Tier5Sounds.MACHINE_UPGRADE, definition().subtitle(subtitle("block.machine.upgrade"))
                .with(sound("minecraft:block.smithing_table.use", SoundDefinition.SoundType.EVENT).pitch(1.1f))
                .with(sound("minecraft:block.chain.place", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.6f))
                .with(sound("minecraft:block.beacon.power_select", SoundDefinition.SoundType.EVENT).pitch(1.9f).volume(0.3f)));
        add(Tier5Sounds.MACHINE_POWER_OFF, definition().subtitle(subtitle("block.machine.power_off"))
                .with(sound("minecraft:block.lever.click", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.6f))
                .with(sound("minecraft:block.beacon.deactivate", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.25f)));
        add(Tier5Sounds.MACHINE_LOW_POWER, definition().subtitle(subtitle("block.machine.low_power"))
                .with(sound("minecraft:block.note_block.bit", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.5f)));
        add(Tier5Sounds.ELECTRIC_FURNACE_RUN, definition().subtitle(subtitle("block.electric_furnace.run"))
                .with(sound("minecraft:block.furnace.fire_crackle", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.5f))
                .with(sound("minecraft:block.furnace.fire_crackle", SoundDefinition.SoundType.EVENT).pitch(1.1f).volume(0.45f)));
        add(Tier5Sounds.MACERATOR_GRIND, definition().subtitle(subtitle("block.macerator.grind"))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.5f))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.82f).volume(0.45f))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.98f).volume(0.45f)));
        add(Tier5Sounds.WIREMILL_DRAW, definition().subtitle(subtitle("block.wiremill.draw"))
                .with(sound("minecraft:block.chain.step", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.5f))
                .with(sound("minecraft:block.chain.step", SoundDefinition.SoundType.EVENT).pitch(1.55f).volume(0.45f))
                .with(sound("minecraft:block.chain.step", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.45f)));
        add(Tier5Sounds.BENDER_PRESS, definition().subtitle(subtitle("block.bender.press"))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.25f))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.22f))
                .with(sound("minecraft:block.chain.place", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.4f)));
        add(Tier5Sounds.LATHE_CUT, definition().subtitle(subtitle("block.lathe.cut"))
                .with(sound("minecraft:item.axe.strip", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.45f))
                .with(sound("minecraft:item.axe.strip", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.4f))
                .with(sound("minecraft:item.axe.strip", SoundDefinition.SoundType.EVENT).pitch(1.7f).volume(0.4f)));
        add(Tier5Sounds.MIXER_STIR, definition().subtitle(subtitle("block.mixer.stir"))
                .with(sound("minecraft:entity.generic.swim", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.4f))
                .with(sound("minecraft:entity.generic.swim", SoundDefinition.SoundType.EVENT).pitch(0.65f).volume(0.35f))
                .with(sound("minecraft:block.water.ambient", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.3f)));
        add(Tier5Sounds.ASSEMBLER_WORK, definition().subtitle(subtitle("block.assembler.work"))
                .with(sound("minecraft:block.dispenser.dispense", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.25f))
                .with(sound("minecraft:block.dispenser.dispense", SoundDefinition.SoundType.EVENT).pitch(1.7f).volume(0.2f))
                .with(sound("minecraft:block.piston.extend", SoundDefinition.SoundType.EVENT).pitch(2.0f).volume(0.12f))
                .with(sound("minecraft:block.comparator.click", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.3f)));
        add(Tier5Sounds.ELECTROLYSER_BUBBLE, definition().subtitle(subtitle("block.electrolyser.bubble"))
                .with(sound("minecraft:block.bubble_column.upwards_ambient", SoundDefinition.SoundType.EVENT).pitch(1.0f).volume(0.5f))
                .with(sound("minecraft:block.bubble_column.upwards_ambient", SoundDefinition.SoundType.EVENT).pitch(1.1f).volume(0.45f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.12f)));
        add(Tier5Sounds.STEAM_TURBINE_RUN, definition().subtitle(subtitle("block.steam_turbine.run"))
                .with(sound("minecraft:block.fire.ambient", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.5f))
                .with(sound("minecraft:entity.minecart.riding", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.3f))
                .with(sound("minecraft:block.fire.ambient", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.45f)));
        add(Tier5Sounds.STEAM_TURBINE_SPIN_DOWN, definition().subtitle(subtitle("block.steam_turbine.spin_down"))
                .with(sound("minecraft:block.beacon.deactivate", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.5f)));
        add(Tier5Sounds.COMBUSTION_GENERATOR_IGNITE, definition().subtitle(subtitle("block.combustion_generator.ignite"))
                .with(sound("minecraft:entity.generic.explode", SoundDefinition.SoundType.EVENT).pitch(1.9f).volume(0.25f))
                .with(sound("minecraft:block.fire.ambient", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.5f)));
        add(Tier5Sounds.COMBUSTION_GENERATOR_RUN, definition().subtitle(subtitle("block.combustion_generator.run"))
                .with(sound("minecraft:entity.minecart.riding", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.35f))
                .with(sound("minecraft:entity.minecart.riding", SoundDefinition.SoundType.EVENT).pitch(0.55f).volume(0.3f))
                .with(sound("minecraft:block.furnace.fire_crackle", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.3f)));
        add(Tier5Sounds.LIQUID_FUEL_BURNER_RUN, definition().subtitle(subtitle("block.liquid_fuel_burner.run"))
                .with(sound("minecraft:block.fire.ambient", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.6f))
                .with(sound("minecraft:block.fire.ambient", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.5f)));
        add(Tier5Sounds.ELECTRIC_HEATER_RUN, definition().subtitle(subtitle("block.electric_heater.run"))
                .with(sound("minecraft:block.furnace.fire_crackle", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.3f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.12f)));
        add(Tier5Sounds.ELECTRIC_PUMP_RUN, definition().subtitle(subtitle("block.electric_pump.run"))
                .with(sound("minecraft:item.bucket.fill", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.3f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.1f).volume(0.1f)));
        add(Tier5Sounds.KINETIC_MOTOR_RUN, definition().subtitle(subtitle("block.kinetic_motor.run"))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.2f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.25f).volume(0.15f)));
        add(Tier5Sounds.BATTERY_BOX_CHARGE, definition().subtitle(subtitle("battery_box.charge"))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(2.0f).volume(0.15f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.9f).volume(0.12f)));
        add(Tier5Sounds.TRANSFORMER_HUM, definition().subtitle(subtitle("block.transformer.hum"))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.18f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(0.52f).volume(0.15f)));
        add(Tier5Sounds.TRANSFORMER_SWITCH, definition().subtitle(subtitle("block.transformer.switch"))
                .with(sound("minecraft:block.lever.click", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.9f))
                .with(sound("minecraft:block.iron_trapdoor.close", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.5f)));
    }
}
