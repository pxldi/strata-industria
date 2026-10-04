package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.felling.FellingSounds;
import dev.strataindustria.journal.JournalContent;
import dev.strataindustria.grid.GridSounds;
import dev.strataindustria.listening.ListeningSounds;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.logistics.Tier5Logistics;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.registry.Tier6Sounds;
import dev.strataindustria.structure.SharedBlocks;
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
        grid();
        tier6();
        felling();
        oilStill();
        oilField();
        shared();
        prologue();
        patterns();
        listening();
        marks();
        ledger();
        foot();
        rail();
        railway();
        transport();
        bronze();
        branches();
        cord();
        bind();
        // A dull chip off a rock.
        add(ModSounds.KNAP_ROCK, stone("knapping.rock", 1.15f, 0.9f));
        // Flint rings sharper and higher.
        add(ModSounds.KNAP_FLINT, stone("knapping.flint", 1.7f, 0.8f));
        // Flint rings a glassy note over the blow; the true blow is a heavier double crack.
        add(ModSounds.SHAPING_CHIME, definition().subtitle(subtitle("shaping.chime"))
                .with(sound("minecraft:block.amethyst_block.chime", SoundDefinition.SoundType.EVENT).pitch(1.0f).volume(0.5f)));
        add(ModSounds.SHAPING_TRUE_BLOW, definition().subtitle(subtitle("shaping.true_blow"))
                .with(sound("minecraft:block.stone.break", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.35f))
                .with(sound("minecraft:block.amethyst_block.hit", SoundDefinition.SoundType.EVENT).pitch(1.7f).volume(0.35f)));
        add(ModSounds.SHAPING_GLINT, definition().subtitle(subtitle("shaping.glint"))
                .with(sound("minecraft:block.amethyst_block.chime", SoundDefinition.SoundType.EVENT).pitch(1.9f).volume(0.22f)));
        // Boulders: a heavy knock with a dry crack under it, a deep break, and shards skittering off.
        add(ModSounds.BOULDER_STRIKE, stone("boulder.strike", 0.8f, 1.0f));
        add(ModSounds.BOULDER_CRACK, definition().subtitle(subtitle("boulder.crack"))
                .with(sound("minecraft:block.stone.break", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.5f))
                .with(sound("minecraft:block.basalt.break", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.5f)));
        add(ModSounds.BOULDER_SPLIT, definition().subtitle(subtitle("boulder.split"))
                .with(sound("minecraft:block.stone.break", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(1.0f))
                .with(sound("minecraft:block.deepslate_bricks.break", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.9f)));
        SoundDefinition shards = definition().subtitle(subtitle("boulder.shards"));
        for (int i = 1; i <= 4; i++) shards.with(sound("minecraft:dig/gravel" + i).pitch(1.5f).volume(0.7f));
        add(ModSounds.BOULDER_SHARDS, shards);
        // The last flake falls and the head comes free: a deeper knock.
        add(ModSounds.KNAP_FINISH, stone("knapping.finish", 0.75f, 1.0f));

        // Fire: flint on rock is a sharp stone click over a bright chime (the code raises the pitch with each
        // strike), the catch a soft rush of air over the first crackle.
        SoundDefinition strike = definition().subtitle(subtitle("flint.strike"));
        for (int i = 1; i <= 4; i++) strike.with(sound("minecraft:step/stone" + i).pitch(1.7f).volume(0.7f));
        strike.with(sound("minecraft:block.amethyst_block.hit", SoundDefinition.SoundType.EVENT).pitch(1.9f).volume(0.45f));
        add(ModSounds.FLINT_STRIKE, strike);
        add(ModSounds.FLINT_CATCH, definition().subtitle(subtitle("flint.catch"))
                .with(sound("minecraft:fire/ignite").pitch(0.8f))
                .with(sound("minecraft:fire/fire").pitch(1.3f).volume(0.7f)));
        add(ModSounds.FIRE_PIT_IGNITE, definition().subtitle(subtitle("fire_pit.ignite"))
                .with(sound("minecraft:fire/ignite").pitch(0.9f))
                .with(sound("minecraft:fire/fire").pitch(1.2f).volume(0.6f)));
        add(ModSounds.FIRE_PIT_EXTINGUISH, definition().subtitle(subtitle("fire_pit.extinguish"))
                .with(sound("minecraft:random/fizz")));
        add(ModSounds.POTTERY_SET, definition().subtitle(subtitle("fire_pit.set"))
                .with(sound("minecraft:block.decorated_pot.place", SoundDefinition.SoundType.EVENT).pitch(1.1f))
                .with(sound("minecraft:block.gravel.place", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.5f)));
        add(ModSounds.POTTERY_RING, definition().subtitle(subtitle("fire_pit.ring"))
                .with(sound("minecraft:block.amethyst_block.chime", SoundDefinition.SoundType.EVENT).pitch(1.2f))
                .with(sound("minecraft:block.decorated_pot.insert", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.6f)));
        add(ModSounds.FIRE_PIT_TORCH, definition().subtitle(subtitle("fire_pit.torch"))
                .with(sound("minecraft:fire/ignite").pitch(1.3f).volume(0.7f)));

        // Clay: wet squelches for shaping, then the kiln's thatch, logs and the cooling crackle.
        SoundDefinition shape = definition().subtitle(subtitle("clay.shape"));
        for (int i = 1; i <= 5; i++) shape.with(sound("minecraft:mob/slime/small" + i).pitch(0.8f).volume(0.35f));
        add(ModSounds.CLAY_SHAPE, shape);
        SoundDefinition finish = definition().subtitle(subtitle("clay.finish"));
        for (int i = 1; i <= 3; i++) finish.with(sound("minecraft:mob/slime/big" + i).pitch(1.1f).volume(0.5f));
        add(ModSounds.CLAY_FINISH, finish);
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
        SoundDefinition.SoundType event = SoundDefinition.SoundType.EVENT;
        // Each metal sings its own note over the hammer: copper soft and round, bronze bell-like, iron dry, steel bright and long.
        add(ModSounds.ANVIL_VOICE_COPPER, definition().subtitle(subtitle("anvil.voice.copper"))
                .with(sound("minecraft:block.note_block.flute", event).volume(0.55f)));
        add(ModSounds.ANVIL_VOICE_BRONZE, definition().subtitle(subtitle("anvil.voice.bronze"))
                .with(sound("minecraft:block.note_block.bell", event).volume(0.6f)));
        add(ModSounds.ANVIL_VOICE_IRON, definition().subtitle(subtitle("anvil.voice.iron"))
                .with(sound("minecraft:block.note_block.iron_xylophone", event).volume(0.6f)));
        add(ModSounds.ANVIL_VOICE_STEEL, definition().subtitle(subtitle("anvil.voice.steel"))
                .with(sound("minecraft:block.note_block.chime", event).volume(0.65f)));
        add(ModSounds.ANVIL_TRUE_BLOW, definition().subtitle(subtitle("anvil.true_blow"))
                .with(sound("minecraft:block.anvil.land", event).pitch(1.5f).volume(0.35f))
                .with(sound("minecraft:block.anvil.land", event).pitch(1.65f).volume(0.35f)));
        add(ModSounds.ANVIL_COLD, definition().subtitle(subtitle("anvil.cold"))
                .with(sound("minecraft:block.netherite_block.hit", event).pitch(0.6f).volume(0.8f))
                .with(sound("minecraft:block.netherite_block.hit", event).pitch(0.7f).volume(0.8f)));
        add(ModSounds.ANVIL_GLINT, definition().subtitle(subtitle("anvil.glint"))
                .with(sound("minecraft:block.amethyst_block.chime", event).pitch(1.8f).volume(0.25f)));
        add(ModSounds.ANVIL_SET, definition().subtitle(subtitle("anvil.set"))
                .with(sound("minecraft:block.anvil.place", event).pitch(1.8f).volume(0.35f)));
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
        // A maker's mark struck into a finished piece: a sharp tick, a short ring and a puff of hiss.
        add(ModSounds.ANVIL_STAMP, definition().subtitle(subtitle("anvil.stamp"))
                .with(sound("minecraft:block.anvil.place", event).pitch(1.9f).volume(0.5f))
                .with(sound("minecraft:block.note_block.iron_xylophone", event).pitch(1.5f).volume(0.45f))
                .with(sound("minecraft:block.lava.extinguish", event).pitch(1.6f).volume(0.2f)));
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
        add(StructureContent.CRACKED_FIRE_BRICKS_SETTLE, definition().subtitle(subtitle("cracked_fire_bricks.settle"))
                .with(sound("minecraft:block.decorated_pot.hit", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.6f).weight(2))
                .with(sound("minecraft:block.calcite.hit", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.7f))
                .with(sound("minecraft:block.gravel.fall", SoundDefinition.SoundType.EVENT).pitch(0.8f).volume(0.35f)));
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

    /** Redesign R5: an axe in green wood, the trunk groaning, and the crash, from vanilla wood and ground samples. */
    private void felling() {
        var event = SoundDefinition.SoundType.EVENT;
        add(FellingSounds.NOTCH, definition().subtitle(subtitle("felling.notch"))
                .with(sound("minecraft:block.wood.hit", event).pitch(0.7f).volume(1.0f))
                .with(sound("minecraft:block.wood.break", event).pitch(1.5f).volume(0.35f)));
        add(FellingSounds.CREAK, definition().subtitle(subtitle("felling.creak"))
                .with(sound("minecraft:block.wooden_door.open", event).pitch(0.5f).volume(0.8f))
                .with(sound("minecraft:block.wooden_trapdoor.open", event).pitch(0.55f).volume(0.6f))
                .with(sound("minecraft:block.azalea_leaves.break", event).pitch(0.8f).volume(0.7f)));
        add(FellingSounds.CRASH, definition().subtitle(subtitle("felling.crash"))
                .with(sound("minecraft:block.wood.break", event).pitch(0.5f).volume(1.0f))
                .with(sound("minecraft:block.gravel.break", event).pitch(0.5f).volume(0.9f))
                .with(sound("minecraft:entity.generic.explode", event).pitch(0.5f).volume(0.3f))
                .with(sound("minecraft:block.azalea_leaves.break", event).pitch(0.6f).volume(1.0f)));
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

    /** Tier 6 spec 24.6: the oil field, heard from far off. */
    private void oilField() {
        add(Tier6Sounds.SEISMIC_FUSE, definition().subtitle(subtitle("block.seismic_charge.fuse"))
                .with(sound("minecraft:entity.tnt.primed", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.4f)));
        add(Tier6Sounds.SEISMIC_THUMP, definition().subtitle(subtitle("block.seismic_charge.thump"))
                .with(sound("minecraft:entity.generic.explode", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.5f).attenuationDistance(64))
                .with(sound("minecraft:block.piston.extend", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.6f).attenuationDistance(64)));
        add(Tier6Sounds.ORE_SCANNER_ECHO, definition().subtitle(subtitle("item.ore_scanner.echo"))
                .with(sound("minecraft:block.note_block.bit", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.8f))
                .with(sound("minecraft:block.note_block.bit", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.7f))
                .with(sound("minecraft:block.note_block.bit", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.6f)));
        add(Tier6Sounds.WELLHEAD_DRILL, definition().subtitle(subtitle("block.wellhead.drill"))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.6f).attenuationDistance(48))
                .with(sound("minecraft:block.grindstone.use", SoundDefinition.SoundType.EVENT).pitch(0.55f).volume(0.9f).attenuationDistance(48)));
        add(Tier6Sounds.WELLHEAD_CASING, definition().subtitle(subtitle("block.wellhead.casing"))
                .with(sound("minecraft:block.chain.place", SoundDefinition.SoundType.EVENT).pitch(0.7f))
                .with(sound("minecraft:block.anvil.place", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.4f)));
        add(Tier6Sounds.WELLHEAD_GUSHER, definition().subtitle(subtitle("block.wellhead.gusher"))
                .with(sound("minecraft:block.bubble_column.whirlpool_ambient", SoundDefinition.SoundType.EVENT).pitch(0.6f).attenuationDistance(48))
                .with(sound("minecraft:entity.generic.splash", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.7f).attenuationDistance(48)));
        add(Tier6Sounds.WELLHEAD_FLOW, definition().subtitle(subtitle("block.wellhead.flow"))
                .with(sound("minecraft:block.bubble_column.upwards_ambient", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.5f))
                .with(sound("minecraft:block.bubble_column.upwards_ambient", SoundDefinition.SoundType.EVENT).pitch(0.45f).volume(0.4f)));
        add(Tier6Sounds.PUMP_JACK_STROKE, definition().subtitle(subtitle("block.pump_jack.stroke"))
                .with(sound("minecraft:block.piston.contract", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.5f).attenuationDistance(48))
                .with(sound("minecraft:block.piston.extend", SoundDefinition.SoundType.EVENT).pitch(0.55f).volume(0.5f).attenuationDistance(48)));
    }

    /** Tier 6 spec 24.6: the still's thick bubbling in a copper pot and the gas that is let go. */
    private void oilStill() {
        add(Tier6Sounds.OIL_STILL_BOIL, definition().subtitle(subtitle("block.oil_still.boil"))
                .with(sound("minecraft:block.bubble_column.upwards_ambient", SoundDefinition.SoundType.EVENT).pitch(0.7f).volume(0.7f))
                .with(sound("minecraft:block.bubble_column.upwards_ambient", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.6f))
                .with(sound("minecraft:block.lava.pop", SoundDefinition.SoundType.EVENT).pitch(0.5f).volume(0.3f)));
        add(Tier6Sounds.OIL_STILL_VENT, definition().subtitle(subtitle("block.oil_still.vent"))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.4f))
                .with(sound("minecraft:entity.llama.spit", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.3f)));
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

    /** The telegraph (outposts spec 9.1): brass and wood clicks, a sounder that clacks down and ticks up, chalk on slate. */
    private void telegraphSounds(SoundDefinition.SoundType event) {
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.KEY_DOWN, definition().subtitle(subtitle("telegraph.key"))
                .with(sound("minecraft:block.lever.click", event).pitch(1.7f).volume(0.6f))
                .with(sound("minecraft:block.wooden_button.click_on", event).pitch(1.4f).volume(0.5f))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(2.0f).volume(0.25f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.KEY_UP, definition().subtitle(subtitle("telegraph.key_up"))
                .with(sound("minecraft:block.wooden_button.click_off", event).pitch(1.5f).volume(0.5f))
                .with(sound("minecraft:block.lever.click", event).pitch(1.3f).volume(0.3f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.SOUNDER_CLACK, definition().subtitle(subtitle("telegraph.sounder"))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(1.55f).volume(0.7f))
                .with(sound("minecraft:block.wooden_trapdoor.close", event).pitch(1.6f).volume(0.6f))
                .with(sound("minecraft:block.note_block.hat", event).pitch(1.0f).volume(0.35f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.SOUNDER_LIFT, definition().subtitle(subtitle("telegraph.sounder_lift"))
                .with(sound("minecraft:block.iron_trapdoor.open", event).pitch(1.9f).volume(0.35f))
                .with(sound("minecraft:block.wooden_button.click_off", event).pitch(1.9f).volume(0.3f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.WIRE_STRUNG, definition().subtitle(subtitle("telegraph.wire_strung"))
                .with(sound("minecraft:item.lead.tied", event).pitch(1.4f).volume(0.8f))
                .with(sound("minecraft:block.tripwire.attach", event).pitch(1.3f).volume(0.6f))
                .with(sound("minecraft:block.amethyst_block.chime", event).pitch(1.8f).volume(0.3f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.WIRE_SNAP, definition().subtitle(subtitle("telegraph.wire_snap"))
                .with(sound("minecraft:item.lead.break", event).pitch(1.2f).volume(0.8f))
                .with(sound("minecraft:block.tripwire.detach", event).pitch(1.0f).volume(0.6f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.DROP_HUNG, definition().subtitle(subtitle("telegraph.drop_hung"))
                .with(sound("minecraft:block.chain.place", event).pitch(1.6f).volume(0.5f))
                .with(sound("minecraft:block.tripwire.click_on", event).pitch(1.6f).volume(0.5f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.LINE_OPEN, definition().subtitle(subtitle("telegraph.line_open"))
                .with(sound("minecraft:block.note_block.bell", event).pitch(1.7f).volume(0.5f))
                .with(sound("minecraft:block.note_block.chime", event).pitch(1.2f).volume(0.4f))
                .with(sound("minecraft:block.lever.click", event).pitch(0.9f).volume(0.4f)));
        add(dev.strataindustria.transport.telegraph.TelegraphRegistry.CHALK, brush("dispatch_board.update", 1.5f, 0.5f));
    }

    private static String subtitle(String name) {
        return "subtitles." + StrataIndustria.MOD_ID + "." + name;
    }

    /** The listening kit (uniqueness 2.1), from vanilla metal, stone, wood and note block samples. */
    private void listening() {
        var event = SoundDefinition.SoundType.EVENT;
        add(ListeningSounds.TAP_RING, definition().subtitle(subtitle("tap.ring"))
                .with(sound("minecraft:block.amethyst_block.chime", event).pitch(1.3f).volume(0.7f))
                .with(sound("minecraft:block.copper.hit", event).pitch(1.5f).volume(0.6f)));
        add(ListeningSounds.TAP_KNOCK, definition().subtitle(subtitle("tap.knock"))
                .with(sound("minecraft:block.stone.hit", event).pitch(1.3f).volume(0.9f))
                .with(sound("minecraft:block.copper.hit", event).pitch(0.9f).volume(0.4f)));
        add(ListeningSounds.TAP_THUD, definition().subtitle(subtitle("tap.thud"))
                .with(sound("minecraft:block.stone.hit", event).pitch(0.55f).volume(1.0f))
                .with(sound("minecraft:block.gravel.hit", event).pitch(0.6f).volume(0.7f)));
        add(ListeningSounds.BELT_SQUEAL, definition().subtitle(subtitle("belt.squeal"))
                .with(sound("minecraft:block.wooden_trapdoor.open", event).pitch(1.8f).volume(0.6f))
                .with(sound("minecraft:block.ladder.step", event).pitch(1.9f).volume(0.5f)));
        add(ListeningSounds.GEAR_TICK, definition().subtitle(subtitle("gear.tick"))
                .with(sound("minecraft:block.comparator.click", event).pitch(1.4f).volume(0.8f))
                .with(sound("minecraft:block.wooden_button.click_on", event).pitch(1.7f).volume(0.5f)));
        add(ListeningSounds.WATER_WHEEL_GROAN, definition().subtitle(subtitle("water_wheel.groan"))
                .with(sound("minecraft:block.scaffolding.place", event).pitch(0.5f).volume(0.8f))
                .with(sound("minecraft:block.wooden_door.open", event).pitch(0.55f).volume(0.6f)));
        add(ListeningSounds.STEAM_KNOCK, definition().subtitle(subtitle("steam_engine.knock"))
                .with(sound("minecraft:block.netherite_block.hit", event).pitch(0.6f).volume(0.9f))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(0.6f).volume(0.7f)));
        add(ListeningSounds.BOILER_HISS, definition().subtitle(subtitle("boiler.hiss"))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(0.55f).volume(0.5f))
                .with(sound("minecraft:block.lava.extinguish", event).pitch(0.5f).volume(0.35f)));
        add(ListeningSounds.STEAM_WHISTLE, definition().subtitle(subtitle("steam_whistle.blow"))
                .with(sound("minecraft:block.note_block.flute", event).pitch(1.9f).volume(1.0f))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(1.7f).volume(0.4f)));
    }

    /** Maker's marks (uniqueness 2.2), from vanilla metal samples. */
    private void marks() {
        var event = SoundDefinition.SoundType.EVENT;
        add(dev.strataindustria.mark.MarkRegistry.STAMP_STRIKE, definition().subtitle(subtitle("mark.stamp"))
                .with(sound("minecraft:block.anvil.place", event).pitch(1.5f).volume(0.6f))
                .with(sound("minecraft:block.copper.hit", event).pitch(0.8f).volume(0.9f)));
        add(dev.strataindustria.mark.MarkRegistry.MARK_CUT, definition().subtitle(subtitle("mark.cut"))
                .with(sound("minecraft:block.copper.hit", event).pitch(1.7f).volume(0.5f))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.8f).volume(0.35f)));
    }

    /** Outposts and transport (outposts spec 14), from vanilla wood, paper and metal samples. */
    private void branches() {
        var event = SoundDefinition.SoundType.EVENT;
        // A green branch bending: wood groan under a rustle of leaves.
        add(dev.strataindustria.branch.BranchSounds.SHAKE, definition().subtitle(subtitle("branch.shake"))
                .with(sound("minecraft:block.wood.hit", event).pitch(1.3f).volume(0.35f))
                .with(sound("minecraft:block.grass.step", event).pitch(1.1f).volume(0.6f))
                .with(sound("minecraft:block.azalea_leaves.step", event).pitch(1.2f).volume(0.6f)));
        // A clean crack and a gust of leaves.
        add(dev.strataindustria.branch.BranchSounds.SNAP, definition().subtitle(subtitle("branch.snap"))
                .with(sound("minecraft:block.wood.break", event).pitch(1.5f).volume(0.9f))
                .with(sound("minecraft:block.bamboo.break", event).pitch(1.2f).volume(0.7f))
                .with(sound("minecraft:block.azalea_leaves.break", event).pitch(0.9f).volume(0.8f)));
        add(dev.strataindustria.branch.BranchSounds.BARE, definition().subtitle(subtitle("branch.bare"))
                .with(sound("minecraft:block.azalea_leaves.hit", event).pitch(0.8f).volume(0.4f)));
    }

    /** Twisting bark into cord and beating cord into cloth (redesign R3), from vanilla wool, bamboo and leather samples. */
    private void cord() {
        var event = SoundDefinition.SoundType.EVENT;
        // Fibres winding on each other: a dry creak over a soft scrape.
        add(dev.strataindustria.cord.CordSounds.TWIST, definition().subtitle(subtitle("cord.twist"))
                .with(sound("minecraft:block.bamboo.hit", event).pitch(1.4f).volume(0.35f))
                .with(sound("minecraft:block.wool.step", event).pitch(1.5f).volume(0.7f))
                .with(sound("minecraft:block.hanging_roots.step", event).pitch(1.3f).volume(0.6f)));
        // The cord pulls tight: a rope knot and a bright snap.
        add(dev.strataindustria.cord.CordSounds.TIGHT, definition().subtitle(subtitle("cord.tight"))
                .with(sound("minecraft:entity.leash_knot.place", event).pitch(1.2f).volume(0.9f))
                .with(sound("minecraft:block.bamboo.break", event).pitch(1.6f).volume(0.5f)));
        // A damp bundle on rock.
        add(dev.strataindustria.cord.CordSounds.BEAT, definition().subtitle(subtitle("cord.beat"))
                .with(sound("minecraft:block.wool.hit", event).pitch(0.8f).volume(0.9f))
                .with(sound("minecraft:block.stone.hit", event).pitch(0.7f).volume(0.7f)));
        // The flat cloth peels off the stone.
        add(dev.strataindustria.cord.CordSounds.CLOTH, definition().subtitle(subtitle("cord.cloth"))
                .with(sound("minecraft:item.armor.equip_leather", event).pitch(1.2f).volume(0.7f))
                .with(sound("minecraft:block.wool.place", event).pitch(1.3f).volume(0.7f)));
    }

    /** The finished stone tool is wrapped and knotted (redesign R4): a quick winding, a knot and a tug. */
    private void bind() {
        var event = SoundDefinition.SoundType.EVENT;
        add(dev.strataindustria.cord.CordSounds.BIND, definition().subtitle(subtitle("cord.bind"))
                .with(sound("minecraft:block.wool.step", event).pitch(1.7f).volume(0.7f))
                .with(sound("minecraft:entity.leash_knot.place", event).pitch(1.0f).volume(0.9f))
                .with(sound("minecraft:item.armor.equip_leather", event).pitch(1.5f).volume(0.6f))
                .with(sound("minecraft:block.bamboo.hit", event).pitch(1.9f).volume(0.4f)));
    }

    private void transport() {
        var event = SoundDefinition.SoundType.EVENT;
        add(dev.strataindustria.registry.TransportSounds.CHARTER_PLACE, definition().subtitle(subtitle("charter.place"))
                .with(sound("minecraft:block.wood.place", event).pitch(0.8f).volume(1.0f))
                .with(sound("minecraft:block.wood.hit", event).pitch(0.7f).volume(0.8f))
                .with(sound("minecraft:item.book.page_turn", event).pitch(1.2f).volume(0.5f)));
        add(dev.strataindustria.registry.TransportSounds.CHARTER_LINE_CUT, definition().subtitle(subtitle("charter.line_cut"))
                .with(sound("minecraft:block.chain.break", event).pitch(0.6f).volume(0.45f))
                .with(sound("minecraft:entity.leash_knot.break", event).pitch(0.7f).volume(0.5f)));
        add(dev.strataindustria.registry.TransportSounds.CHARTER_DEED, definition().subtitle(subtitle("charter.deed"))
                .with(sound("minecraft:item.book.page_turn", event).pitch(1.0f).volume(0.7f))
                .with(sound("minecraft:ui.cartography_table.take_result", event).pitch(1.5f).volume(0.3f)));
    }

    /** Bronze age touches (uniqueness 4.2, 4.3), from vanilla fire and fizz samples. */
    private void bronze() {
        var event = SoundDefinition.SoundType.EVENT;
        add(dev.strataindustria.cabinet.CabinetRegistry.SET, definition().subtitle(subtitle("cabinet.set"))
                .with(sound("minecraft:block.wooden_trapdoor.close", event).pitch(1.5f).volume(0.5f))
                .with(sound("minecraft:block.glass.hit", event).pitch(1.7f).volume(0.3f)));
        add(dev.strataindustria.cabinet.CabinetRegistry.SHELF, definition().subtitle(subtitle("cabinet.shelf"))
                .with(sound("minecraft:block.amethyst_block.chime", event).pitch(1.0f).volume(0.7f))
                .with(sound("minecraft:block.wood.hit", event).pitch(1.2f).volume(0.4f)));
        add(dev.strataindustria.bronze.BronzeRegistry.BELL_RING, definition().subtitle(subtitle("bell.ring"))
                .with(sound("minecraft:block.bell.use", event).volume(1.0f)));
        add(dev.strataindustria.bronze.BronzeRegistry.FUMES, definition().subtitle(subtitle("crucible.fumes"))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(1.5f).volume(0.5f))
                .with(sound("minecraft:block.lava.extinguish", event).pitch(1.8f).volume(0.4f)));
    }

    /** The builder's ledger (uniqueness 2.5), from vanilla book, wood and metal samples. */
    private void ledger() {
        var event = SoundDefinition.SoundType.EVENT;
        add(dev.strataindustria.ledger.LedgerRegistry.ENTRY, definition().subtitle(subtitle("ledger.entry"))
                .with(sound("minecraft:item.book.page_turn", event).pitch(0.9f).volume(0.8f))
                .with(sound("minecraft:ui.cartography_table.take_result", event).pitch(1.4f).volume(0.35f)));
        add(dev.strataindustria.ledger.LedgerRegistry.STAMP, definition().subtitle(subtitle("ledger.stamp"))
                .with(sound("minecraft:block.wood.hit", event).pitch(0.8f).volume(0.9f))
                .with(sound("minecraft:block.copper.hit", event).pitch(1.3f).volume(0.5f)));
        add(dev.strataindustria.ledger.LedgerRegistry.SHORT, definition().subtitle(subtitle("ledger.short"))
                .with(sound("minecraft:item.book.put", event).pitch(0.8f).volume(0.9f)));
    }

    /** Tier 2 on foot (outposts and transport spec 14), from vanilla wool, leather, stone and wood samples. */
    private void foot() {
        var event = SoundDefinition.SoundType.EVENT;
        SoundDefinition hang = definition().subtitle(subtitle("rope.hang"));
        for (int i = 1; i <= 4; i++) hang.with(sound("minecraft:dig/wool" + i).pitch(0.8f).volume(0.7f));
        add(dev.strataindustria.transport.foot.FootRegistry.ROPE_HANG, hang);
        add(dev.strataindustria.transport.foot.FootRegistry.PACK_OPEN, definition().subtitle(subtitle("pack.open"))
                .with(sound("minecraft:item.armor.equip_leather", event).pitch(0.9f).volume(0.7f))
                .with(sound("minecraft:block.wood.hit", event).pitch(1.4f).volume(0.3f)));
        SoundDefinition stack = definition().subtitle(subtitle("cairn.stack"));
        for (int i = 1; i <= 4; i++) stack.with(sound("minecraft:dig/stone" + i).pitch(0.9f).volume(0.9f));
        for (int i = 1; i <= 2; i++) stack.with(sound("minecraft:dig/gravel" + i).pitch(1.1f).volume(0.5f));
        add(dev.strataindustria.transport.foot.FootRegistry.CAIRN_STACK, stack);
        SoundDefinition cut = definition().subtitle(subtitle("blaze.cut"));
        for (int i = 1; i <= 4; i++) cut.with(sound("minecraft:dig/wood" + i).pitch(1.5f).volume(0.6f));
        add(dev.strataindustria.transport.foot.FootRegistry.BLAZE_CUT, cut);
        SoundDefinition roll = definition().subtitle(subtitle("handcart.roll"));
        for (int i = 1; i <= 4; i++) roll.with(sound("minecraft:step/wood" + i).pitch(0.7f).volume(0.6f));
        for (int i = 1; i <= 4; i++) roll.with(sound("minecraft:step/gravel" + i).pitch(0.8f).volume(0.4f));
        add(dev.strataindustria.transport.foot.FootRegistry.HANDCART_ROLL, roll);
        add(dev.strataindustria.transport.foot.FootRegistry.HANDCART_SHAFTS, definition().subtitle(subtitle("handcart.shafts"))
                .with(sound("minecraft:block.wood.place", event).pitch(0.8f).volume(0.8f))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.2f).volume(0.25f)));
    }

    /** The wooden tramway, from vanilla wood, chain, gravel and barrel samples. */
    private void rail() {
        var event = SoundDefinition.SoundType.EVENT;
        SoundDefinition roll = definition().subtitle(subtitle("tub.roll_wood"));
        for (int i = 1; i <= 4; i++) roll.with(sound("minecraft:step/wood" + i).pitch(0.6f).volume(0.6f));
        for (int i = 1; i <= 4; i++) roll.with(sound("minecraft:step/gravel" + i).pitch(0.7f).volume(0.35f));
        add(dev.strataindustria.transport.rail.RailRegistry.TUB_ROLL, roll);
        add(dev.strataindustria.transport.rail.RailRegistry.TUB_COUPLE, definition().subtitle(subtitle("tub.couple"))
                .with(sound("minecraft:block.chain.place", event).pitch(1.0f).volume(0.8f))
                .with(sound("minecraft:block.wood.hit", event).pitch(0.7f).volume(0.5f)));
        add(dev.strataindustria.transport.rail.RailRegistry.TUB_THUD, definition().subtitle(subtitle("tub.thud"))
                .with(sound("minecraft:block.wood.hit", event).pitch(0.55f).volume(1.0f))
                .with(sound("minecraft:block.wooden_trapdoor.close", event).pitch(0.6f).volume(0.6f)));
        add(dev.strataindustria.transport.rail.RailRegistry.TUB_STOP_BRAKE, definition().subtitle(subtitle("tub_stop.brake"))
                .with(sound("minecraft:block.barrel.close", event).pitch(0.8f).volume(0.7f))
                .with(sound("minecraft:block.chain.hit", event).pitch(0.9f).volume(0.4f)));
        SoundDefinition dump = definition().subtitle(subtitle("tipple.dump"));
        for (int i = 1; i <= 4; i++) dump.with(sound("minecraft:dig/gravel" + i).pitch(0.8f).volume(0.9f));
        for (int i = 1; i <= 4; i++) dump.with(sound("minecraft:dig/stone" + i).pitch(0.9f).volume(0.5f));
        add(dev.strataindustria.transport.rail.RailRegistry.TIPPLE_DUMP, dump);
        add(dev.strataindustria.transport.rail.RailRegistry.PONY_HARNESS, definition().subtitle(subtitle("pony.harness"))
                .with(sound("minecraft:item.armor.equip_leather", event).pitch(0.9f).volume(0.8f))
                .with(sound("minecraft:block.chain.place", event).pitch(1.3f).volume(0.5f))
                .with(sound("minecraft:entity.horse.saddle", event).pitch(1.0f).volume(0.6f)));
        add(dev.strataindustria.transport.rail.RailRegistry.PONY_STEP, definition().subtitle(subtitle("pony.step"))
                .with(sound("minecraft:entity.horse.step_wood", event).pitch(0.9f).volume(0.7f))
                .with(sound("minecraft:entity.horse.step", event).pitch(0.8f).volume(0.35f)));
        add(dev.strataindustria.transport.rail.RailRegistry.PONY_EAT, definition().subtitle(subtitle("pony.eat"))
                .with(sound("minecraft:entity.horse.eat", event).pitch(1.0f).volume(0.7f))
                .with(sound("minecraft:block.grass.break", event).pitch(1.2f).volume(0.4f)));
        add(dev.strataindustria.transport.rail.RailRegistry.PONY_SNORT, definition().subtitle(subtitle("pony.snort"))
                .with(sound("minecraft:entity.horse.breathe", event).pitch(0.8f).volume(0.9f))
                .with(sound("minecraft:entity.horse.angry", event).pitch(1.1f).volume(0.3f)));
        add(dev.strataindustria.transport.rail.RailRegistry.WINCH_HAUL, definition().subtitle(subtitle("winch.haul"))
                .with(sound("minecraft:block.wooden_door.open", event).pitch(0.5f).volume(0.45f))
                .with(sound("minecraft:block.chain.hit", event).pitch(0.7f).volume(0.4f))
                .with(sound("minecraft:block.wood.step", event).pitch(0.6f).volume(0.5f)));
    }

    /** The steel track and wagons, from vanilla iron, chain, anvil and water samples. */
    private void railway() {
        var event = SoundDefinition.SoundType.EVENT;
        add(dev.strataindustria.transport.rail.RailwayRegistry.CLATTER_STEEL, definition().subtitle(subtitle("rail.clatter_steel"))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.5f).volume(0.55f))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(1.7f).volume(0.35f))
                .with(sound("minecraft:block.anvil.step", event).pitch(1.6f).volume(0.4f))
                .with(sound("minecraft:block.metal.step", event).pitch(1.3f).volume(0.6f)));
        add(dev.strataindustria.transport.rail.RailwayRegistry.BUFFER_CLANG, definition().subtitle(subtitle("steel_buffer.clang"))
                .with(sound("minecraft:block.anvil.land", event).pitch(1.1f).volume(0.55f))
                .with(sound("minecraft:block.iron_door.close", event).pitch(0.7f).volume(0.7f))
                .with(sound("minecraft:block.chain.break", event).pitch(1.0f).volume(0.4f)));
        add(dev.strataindustria.transport.rail.RailwayRegistry.PORT_FLOW, definition().subtitle(subtitle("wagon_port.flow"))
                .with(sound("minecraft:block.water.ambient", event).pitch(1.2f).volume(0.7f))
                .with(sound("minecraft:item.bucket.fill", event).pitch(1.4f).volume(0.35f))
                .with(sound("minecraft:block.iron_trapdoor.open", event).pitch(1.6f).volume(0.2f)));
        add(dev.strataindustria.transport.rail.RailwayRegistry.LOCOMOTIVE_CHUFF, definition().subtitle(subtitle("locomotive.chuff"))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(1.9f).volume(0.55f))
                .with(sound("minecraft:block.piston.contract", event).pitch(1.9f).volume(0.35f))
                .with(sound("minecraft:block.lava.extinguish", event).pitch(1.7f).volume(0.3f)));
        add(dev.strataindustria.transport.rail.RailwayRegistry.LOCOMOTIVE_WHISTLE, definition().subtitle(subtitle("locomotive.whistle"))
                .with(sound("minecraft:block.note_block.flute", event).pitch(1.25f).volume(1.0f))
                .with(sound("minecraft:block.note_block.didgeridoo", event).pitch(1.5f).volume(0.35f))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(1.5f).volume(0.4f)));
        add(dev.strataindustria.transport.rail.RailwayRegistry.LOCOMOTIVE_BRAKE, definition().subtitle(subtitle("locomotive.brake"))
                .with(sound("minecraft:block.grindstone.use", event).pitch(1.7f).volume(0.5f))
                .with(sound("minecraft:item.axe.scrape", event).pitch(1.3f).volume(0.45f))
                .with(sound("minecraft:block.chain.hit", event).pitch(0.8f).volume(0.35f)));
        add(dev.strataindustria.transport.rail.RailwayRegistry.WATER_POUR, definition().subtitle(subtitle("water_tower.pour"))
                .with(sound("minecraft:block.water.ambient", event).pitch(1.0f).volume(0.8f))
                .with(sound("minecraft:item.bucket.empty", event).pitch(1.3f).volume(0.35f))
                .with(sound("minecraft:block.pointed_dripstone.drip_water", event).pitch(1.2f).volume(0.5f)));
        SoundDefinition coal = definition().subtitle(subtitle("coal_stage.load"));
        for (int i = 1; i <= 3; i++) coal.with(sound("minecraft:dig/gravel" + i).pitch(1.1f).volume(0.7f));
        coal.with(sound("minecraft:block.iron_trapdoor.open", event).pitch(1.8f).volume(0.15f));
        add(dev.strataindustria.transport.rail.RailwayRegistry.COAL_LOAD, coal);
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.DRIVE, definition().subtitle(subtitle("ropeway.drive"))
                .with(sound("minecraft:block.chain.hit", event).pitch(0.5f).volume(0.5f))
                .with(sound("minecraft:block.grindstone.use", event).pitch(0.55f).volume(0.3f))
                .with(sound("minecraft:block.wooden_trapdoor.open", event).pitch(0.6f).volume(0.35f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.SHEAVE, definition().subtitle(subtitle("ropeway.sheave"))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(1.5f).volume(0.45f))
                .with(sound("minecraft:block.chain.step", event).pitch(1.2f).volume(0.6f))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.4f).volume(0.35f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.BUCKET_HANG, definition().subtitle(subtitle("ropeway.bucket_hang"))
                .with(sound("minecraft:block.chain.place", event).pitch(1.1f).volume(0.7f))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(1.2f).volume(0.4f))
                .with(sound("minecraft:item.bucket.fill", event).pitch(1.6f).volume(0.2f)));
        SoundDefinition tip = definition().subtitle(subtitle("ropeway.bucket_tip"));
        for (int i = 1; i <= 4; i++) tip.with(sound("minecraft:dig/gravel" + i).pitch(0.9f).volume(0.8f));
        tip.with(sound("minecraft:block.chain.hit", event).pitch(0.9f).volume(0.4f));
        tip.with(sound("minecraft:item.bucket.empty", event).pitch(1.5f).volume(0.2f));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.BUCKET_TIP, tip);
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.ROPE_TIE, definition().subtitle(subtitle("ropeway.rope_tie"))
                .with(sound("minecraft:item.lead.tied", event).pitch(1.0f).volume(0.9f))
                .with(sound("minecraft:block.chain.place", event).pitch(0.9f).volume(0.5f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.LINE_STRUNG, definition().subtitle(subtitle("ropeway.line_strung"))
                .with(sound("minecraft:block.note_block.bell", event).pitch(1.6f).volume(0.6f))
                .with(sound("minecraft:item.lead.tied", event).pitch(0.8f).volume(0.8f))
                .with(sound("minecraft:block.chain.hit", event).pitch(0.7f).volume(0.6f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.LINE_SNAP, definition().subtitle(subtitle("ropeway.snap"))
                .with(sound("minecraft:item.lead.break", event).pitch(0.8f).volume(1.0f))
                .with(sound("minecraft:block.tripwire.detach", event).pitch(0.5f).volume(0.8f))
                .with(sound("minecraft:block.chain.break", event).pitch(0.7f).volume(0.6f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.ANGLE_TURN, definition().subtitle(subtitle("ropeway.angle_turn"))
                .with(sound("minecraft:block.chain.step", event).pitch(0.7f).volume(0.6f))
                .with(sound("minecraft:block.grindstone.use", event).pitch(0.9f).volume(0.25f))
                .with(sound("minecraft:block.iron_trapdoor.open", event).pitch(1.3f).volume(0.3f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.SEAT_CLIP, definition().subtitle(subtitle("ropeway.seat_clip"))
                .with(sound("minecraft:block.chain.place", event).pitch(1.2f).volume(0.8f))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(1.4f).volume(0.5f))
                .with(sound("minecraft:entity.horse.saddle", event).pitch(1.2f).volume(0.35f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.SEAT_RELEASE, definition().subtitle(subtitle("ropeway.seat_release"))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.1f).volume(0.6f))
                .with(sound("minecraft:block.iron_trapdoor.open", event).pitch(1.5f).volume(0.4f))
                .with(sound("minecraft:block.wood.place", event).pitch(1.3f).volume(0.4f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.RIDE_WIND, definition().subtitle(subtitle("ropeway.ride_wind"))
                .with(sound("minecraft:item.elytra.flying", event).pitch(0.7f).volume(0.35f))
                .with(sound("minecraft:weather.rain.above", event).pitch(0.6f).volume(0.2f)));
        add(dev.strataindustria.transport.ropeway.RopewayRegistry.TOP_UP, definition().subtitle(subtitle("ropeway.top_up"))
                .with(sound("minecraft:block.chain.place", event).pitch(0.9f).volume(0.5f))
                .with(sound("minecraft:entity.item.pickup", event).pitch(0.8f).volume(0.4f))
                .with(sound("minecraft:block.barrel.close", event).pitch(1.2f).volume(0.3f)));
        add(dev.strataindustria.transport.rail.TramRegistry.MOTOR, definition().subtitle(subtitle("tram.motor"))
                .with(sound("minecraft:block.beacon.ambient", event).pitch(1.6f).volume(0.3f))
                .with(sound("minecraft:block.beacon.ambient", event).pitch(1.9f).volume(0.22f))
                .with(sound("minecraft:block.piston.contract", event).pitch(2.0f).volume(0.08f)));
        add(dev.strataindustria.transport.rail.TramRegistry.SPARK, definition().subtitle(subtitle("tram.spark"))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(1.9f).volume(0.5f))
                .with(sound("minecraft:block.amethyst_block.hit", event).pitch(2.0f).volume(0.4f))
                .with(sound("minecraft:block.lava.extinguish", event).pitch(2.0f).volume(0.3f)));
        add(dev.strataindustria.transport.rail.TramRegistry.BELL, definition().subtitle(subtitle("tram.bell"))
                .with(sound("minecraft:block.bell.use", event).pitch(1.7f).volume(0.8f))
                .with(sound("minecraft:block.note_block.bell", event).pitch(1.0f).volume(0.5f))
                .with(sound("minecraft:block.amethyst_block.chime", event).pitch(1.2f).volume(0.3f)));
        add(dev.strataindustria.transport.rail.TramRegistry.CONTACT, definition().subtitle(subtitle("tram.contact"))
                .with(sound("minecraft:block.iron_trapdoor.open", event).pitch(1.8f).volume(0.6f))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.4f).volume(0.5f))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(2.0f).volume(0.25f)));
        add(dev.strataindustria.transport.rail.TramRegistry.LOSE_WIRE, definition().subtitle(subtitle("tram.lose_wire"))
                .with(sound("minecraft:block.chain.hit", event).pitch(0.8f).volume(0.6f))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(1.4f).volume(0.5f)));
        add(dev.strataindustria.transport.rail.TramRegistry.WIRE_STRUNG, definition().subtitle(subtitle("trolley_wire.strung"))
                .with(sound("minecraft:block.chain.place", event).pitch(1.4f).volume(0.6f))
                .with(sound("minecraft:block.iron_trapdoor.close", event).pitch(1.8f).volume(0.4f))
                .with(sound("minecraft:block.amethyst_block.hit", event).pitch(1.5f).volume(0.35f)));
        add(dev.strataindustria.transport.rail.TramRegistry.WIRE_CUT, definition().subtitle(subtitle("trolley_wire.cut"))
                .with(sound("minecraft:block.chain.break", event).pitch(1.2f).volume(0.6f))
                .with(sound("minecraft:block.tripwire.detach", event).pitch(1.2f).volume(0.7f)));
        telegraphSounds(event);
        add(dev.strataindustria.transport.rail.RailwayRegistry.FLAT_LOAD, definition().subtitle(subtitle("flat_wagon.load"))
                .with(sound("minecraft:block.anvil.place", event).pitch(1.3f).volume(0.6f))
                .with(sound("minecraft:block.wood.place", event).pitch(0.7f).volume(0.9f))
                .with(sound("minecraft:block.chain.place", event).pitch(1.1f).volume(0.5f)));
    }

    /** Pattern casting, from vanilla wood and sand samples. */
    private void patterns() {
        SoundDefinition carve = definition().subtitle(subtitle("pattern.carve"));
        for (int i = 1; i <= 4; i++) carve.with(sound("minecraft:dig/wood" + i).pitch(1.6f).volume(0.45f));
        add(dev.strataindustria.registry.PatternRegistry.PATTERN_CARVE, carve);
        SoundDefinition finish = definition().subtitle(subtitle("pattern.finish"));
        for (int i = 1; i <= 4; i++) finish.with(sound("minecraft:dig/wood" + i).pitch(1.0f).volume(0.7f));
        add(dev.strataindustria.registry.PatternRegistry.PATTERN_FINISH, finish);
        SoundDefinition press = definition().subtitle(subtitle("pattern.press"));
        for (int i = 1; i <= 4; i++) press.with(sound("minecraft:dig/sand" + i).pitch(0.8f).volume(0.8f));
        for (int i = 1; i <= 2; i++) press.with(sound("minecraft:dig/wood" + i).pitch(0.7f).volume(0.5f));
        add(dev.strataindustria.registry.PatternRegistry.PATTERN_PRESS, press);
    }

    /** The brick kiln and the casting table, from vanilla fire, stone and pottery samples. */
    private void prologue() {
        SoundDefinition work = definition().subtitle(subtitle("brick_kiln.work"));
        for (int i = 1; i <= 3; i++) work.with(sound("minecraft:block/furnace/fire_crackle" + i).pitch(0.95f).volume(0.5f));
        add(dev.strataindustria.registry.PrologueRegistry.BRICK_KILN_WORK, work);
        add(dev.strataindustria.registry.PrologueRegistry.BRICK_KILN_DONE, definition().subtitle(subtitle("brick_kiln.done"))
                .with(sound("minecraft:block.decorated_pot.hit", SoundDefinition.SoundType.EVENT).pitch(1.1f).volume(0.7f))
                .with(sound("minecraft:block.fire.extinguish", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.2f)));
        add(dev.strataindustria.registry.PrologueRegistry.TABLE_SET, stone("casting_table.set", 0.7f, 0.7f));
        SoundDefinition knock = definition().subtitle(subtitle("casting_table.knock"));
        for (int i = 1; i <= 4; i++) knock.with(sound("minecraft:dig/stone" + i).pitch(1.25f).volume(0.9f));
        for (int i = 1; i <= 2; i++) knock.with(sound("minecraft:dig/stone" + i).pitch(1.5f).volume(0.6f));
        add(dev.strataindustria.registry.PrologueRegistry.TABLE_KNOCK, knock);
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
        add(Tier5Logistics.PIPE_EXTRACT, definition().subtitle(subtitle("block.item_pipe.extract"))
                .with(sound("minecraft:block.dispenser.launch", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.2f))
                .with(sound("minecraft:block.dispenser.launch", SoundDefinition.SoundType.EVENT).pitch(1.65f).volume(0.18f)));
        add(Tier5Logistics.CONTROLLER_OPEN, definition().subtitle(subtitle("block.storage_controller.open"))
                .with(sound("minecraft:block.note_block.chime", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.6f)));
        add(Tier5Logistics.CONTROLLER_CLOSE, definition().subtitle(subtitle("block.storage_controller.close"))
                .with(sound("minecraft:block.lever.click", SoundDefinition.SoundType.EVENT).pitch(0.9f).volume(0.7f)));
        add(Tier5Logistics.CONTROLLER_STORE, definition().subtitle(subtitle("block.storage_controller.store"))
                .with(sound("minecraft:ui.button.click", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.25f)));
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
        add(Tier5Sounds.WRENCH_TURN, definition().subtitle(subtitle("item.wrench.turn"))
                .with(sound("minecraft:block.chain.hit", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.7f))
                .with(sound("minecraft:block.lever.click", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.5f)));
        add(Tier5Sounds.ORE_SCANNER_SCAN, definition().subtitle(subtitle("item.ore_scanner.scan"))
                .with(sound("minecraft:block.beacon.power_select", SoundDefinition.SoundType.EVENT).pitch(1.4f).volume(0.35f))
                .with(sound("minecraft:block.amethyst_block.resonate", SoundDefinition.SoundType.EVENT).pitch(1.6f).volume(0.4f)));
        add(Tier5Sounds.ORE_SCANNER_DONE, definition().subtitle(subtitle("item.ore_scanner.done"))
                .with(sound("minecraft:block.note_block.bell", SoundDefinition.SoundType.EVENT).pitch(1.5f).volume(0.6f))
                .with(sound("minecraft:block.note_block.bell", SoundDefinition.SoundType.EVENT).pitch(2.0f).volume(0.5f)));
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
        add(Tier5Sounds.EXTRUDER_PRESS, definition().subtitle(subtitle("block.extruder.press"))
                .with(sound("minecraft:block.piston.extend", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.6f))
                .with(sound("minecraft:block.piston.extend", SoundDefinition.SoundType.EVENT).pitch(0.55f).volume(0.55f))
                .with(sound("minecraft:block.slime_block.step", SoundDefinition.SoundType.EVENT).pitch(0.6f).volume(0.3f)));
        add(Tier5Sounds.POWER_HAMMER_STRIKE, definition().subtitle(subtitle("block.power_hammer.strike"))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(1.2f).volume(0.35f))
                .with(sound("minecraft:block.anvil.land", SoundDefinition.SoundType.EVENT).pitch(1.3f).volume(0.3f))
                .with(sound("minecraft:block.piston.contract", SoundDefinition.SoundType.EVENT).pitch(1.8f).volume(0.2f)));
        add(Tier5Sounds.POWER_HAMMER_INDUCTION, definition().subtitle(subtitle("block.power_hammer.induction"))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.9f).volume(0.2f))
                .with(sound("minecraft:block.beacon.ambient", SoundDefinition.SoundType.EVENT).pitch(1.7f).volume(0.18f)));
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

    /** Mains hum, the stethoscope and the Leyden jars (uniqueness 2.1, 7.1, 7.3), from vanilla beacon, warden, chain and lightning samples. */
    private void grid() {
        var event = SoundDefinition.SoundType.EVENT;
        add(GridSounds.MAINS_HUM, definition().subtitle(subtitle("grid.hum"))
                .with(sound("minecraft:block.beacon.ambient", event).pitch(0.55f).volume(0.5f))
                .with(sound("minecraft:block.beacon.ambient", event).pitch(0.58f).volume(0.45f)));
        add(GridSounds.MAINS_BUZZ, definition().subtitle(subtitle("grid.buzz"))
                .with(sound("minecraft:block.beacon.ambient", event).pitch(1.5f).volume(0.35f))
                .with(sound("minecraft:block.redstone_torch.burnout", event).pitch(0.5f).volume(0.3f))
                .with(sound("minecraft:block.beacon.ambient", event).pitch(1.4f).volume(0.3f)));
        add(GridSounds.LISTEN_STEADY, definition().subtitle(subtitle("stethoscope.steady"))
                .with(sound("minecraft:entity.warden.heartbeat", event).pitch(1.6f).volume(0.5f))
                .with(sound("minecraft:block.beacon.ambient", event).pitch(0.9f).volume(0.12f)));
        add(GridSounds.LISTEN_STRAINED, definition().subtitle(subtitle("stethoscope.strained"))
                .with(sound("minecraft:entity.warden.heartbeat", event).pitch(2.0f).volume(0.55f))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.8f).volume(0.3f)));
        add(GridSounds.LISTEN_SILENT, definition().subtitle(subtitle("stethoscope.silent"))
                .with(sound("minecraft:block.lever.click", event).pitch(1.6f).volume(0.4f)));
        add(GridSounds.LEYDEN_STRIKE, definition().subtitle(subtitle("leyden_jar.strike"))
                .with(sound("minecraft:entity.lightning_bolt.impact", event).pitch(1.6f).volume(0.5f))
                .with(sound("minecraft:block.copper_bulb.turn_on", event).pitch(0.6f).volume(0.7f))
                .with(sound("minecraft:block.beacon.power_select", event).pitch(2.0f).volume(0.3f)));
    }

    /** The shared structure blocks (structures v2 section 5), built from vanilla wood, fire, chain and gravel sounds. */
    private void shared() {
        var event = SoundDefinition.SoundType.EVENT;
        add(SharedBlocks.CRATE_OPEN, definition().subtitle(subtitle("crate.open"))
                .with(sound("minecraft:block.barrel.open", event).pitch(1.25f).volume(0.7f)));
        add(SharedBlocks.CRATE_CLOSE, definition().subtitle(subtitle("crate.close"))
                .with(sound("minecraft:block.barrel.close", event).pitch(1.3f).volume(0.7f)));
        add(SharedBlocks.CRATE_LOCKED, definition().subtitle(subtitle("crate.locked"))
                .with(sound("minecraft:block.wood.hit", event).pitch(0.7f).weight(2))
                .with(sound("minecraft:block.chest.locked", event).pitch(1.5f).volume(0.5f)));
        add(SharedBlocks.CRATE_UNLOCK, definition().subtitle(subtitle("crate.unlock"))
                .with(sound("minecraft:block.wooden_trapdoor.open", event).pitch(1.3f).volume(0.8f))
                .with(sound("minecraft:block.chain.place", event).pitch(1.1f).volume(0.6f)));
        add(SharedBlocks.MINERS_LAMP_LIGHT, definition().subtitle(subtitle("miners_lamp.light"))
                .with(sound("minecraft:item.flintandsteel.use", event).pitch(1.5f).volume(0.35f))
                .with(sound("minecraft:block.fire.ambient", event).pitch(1.6f).volume(0.5f)));
        add(SharedBlocks.MINERS_LAMP_SNUFF, definition().subtitle(subtitle("miners_lamp.snuff"))
                .with(sound("minecraft:block.fire.extinguish", event).pitch(1.7f).volume(0.35f)));
        add(SharedBlocks.MINERS_LAMP_FLUTTER, definition().subtitle(subtitle("miners_lamp.flutter"))
                .with(sound("minecraft:block.candle.ambient", event).pitch(1.0f).volume(0.5f))
                .with(sound("minecraft:block.fire.ambient", event).pitch(1.9f).volume(0.3f)));
        add(SharedBlocks.SLUICE_BOX_WATER, definition().subtitle(subtitle("sluice_box.water"))
                .with(sound("minecraft:block.water.ambient", event).pitch(1.3f).volume(0.5f).weight(2))
                .with(sound("minecraft:block.pointed_dripstone.drip_water", event).pitch(1.1f).volume(0.4f)));
        add(SharedBlocks.ORE_CART_RATTLE, definition().subtitle(subtitle("ore_cart.rattle"))
                .with(sound("minecraft:block.chain.place", event).pitch(0.7f).weight(2))
                .with(sound("minecraft:block.gravel.hit", event).pitch(0.8f).volume(0.7f)));
        add(SharedBlocks.TOOL_RACK_CLINK, definition().subtitle(subtitle("tool_rack.clink"))
                .with(sound("minecraft:block.chain.hit", event).pitch(1.4f).volume(0.6f))
                .with(sound("minecraft:item.armor.equip_iron", event).pitch(1.3f).volume(0.35f)));
        add(SharedBlocks.RUBBLE_BREAK, definition().subtitle(subtitle("rubble.break"))
                .with(sound("minecraft:block.gravel.break", event).pitch(0.8f).weight(3))
                .with(sound("minecraft:block.stone.break", event).pitch(1.4f).volume(0.5f)));
        add(SharedBlocks.RUBBLE_STEP, definition().subtitle("subtitles.block.generic.footsteps")
                .with(sound("minecraft:block.gravel.step", event).weight(3))
                .with(sound("minecraft:block.stone.step", event).pitch(1.3f).volume(0.4f)));
        add(SharedBlocks.CRACKED_ROCK_BREAK, definition().subtitle(subtitle("cracked_rock.break"))
                .with(sound("minecraft:block.stone.break", event).pitch(0.85f).weight(2))
                .with(sound("minecraft:block.gravel.break", event).pitch(1.1f).volume(0.5f)));
        add(SharedBlocks.SMOULDERING_CRACKLE, definition().subtitle(subtitle("smouldering_log_pile.crackle"))
                .with(sound("minecraft:block.campfire.crackle", event).pitch(0.6f).volume(0.7f))
                .with(sound("minecraft:block.fire.ambient", event).pitch(0.5f).volume(0.5f)));
        add(SharedBlocks.WINDLASS_CREAK, definition().subtitle(subtitle("windlass.creak"))
                .with(sound("minecraft:block.scaffolding.place", event).pitch(0.65f).volume(0.5f))
                .with(sound("minecraft:block.ladder.step", event).pitch(0.6f).volume(0.6f)));
    }
}
