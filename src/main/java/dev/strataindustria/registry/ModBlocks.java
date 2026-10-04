package dev.strataindustria.registry;

import dev.strataindustria.machine.BellowsBlock;
import dev.strataindustria.machine.MillstoneBlock;
import dev.strataindustria.machine.SawMillBlock;
import dev.strataindustria.machine.TripHammerBlock;
import dev.strataindustria.power.AxleBlock;
import dev.strataindustria.power.GearboxBlock;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.WaterWheelBlock;
import dev.strataindustria.bloomery.BloomeryBlock;
import dev.strataindustria.quern.QuernBlock;
import dev.strataindustria.smithing.AnvilBlock;
import dev.strataindustria.StrataIndustria;
import net.minecraft.resources.Identifier;
import dev.strataindustria.block.BoulderBlock;
import dev.strataindustria.block.GroundCoverBlock;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.ceramics.CrucibleBlock;
import dev.strataindustria.charcoal.CharcoalPileBlock;
import dev.strataindustria.charcoal.LogPileBlock;
import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StrataIndustria.MOD_ID);

    /** Raw rock, the stone that replaces vanilla stone. */
    public static final Map<Rock, DeferredBlock<Block>> RAW_ROCK = new EnumMap<>(Rock.class);
    /** Stone anvils dressed from raw igneous rock (spec 9.1); only igneous rocks have one. */
    public static final Map<Rock, DeferredBlock<AnvilBlock>> STONE_ANVILS = new EnumMap<>(Rock.class);
    public static final Map<Rock, DeferredBlock<Block>> COBBLED_ROCK = new EnumMap<>(Rock.class);
    /** Ore blocks for every (rock, mineral) pair; the grade is a blockstate. */
    public static final Map<Rock, Map<OreMineral, DeferredBlock<OreBlock>>> ORES = new EnumMap<>(Rock.class);
    /** Surface indicators placed above veins. */
    public static final Map<OreMineral, DeferredBlock<GroundCoverBlock>> SMALL_ORES = new EnumMap<>(OreMineral.class);

    /** Weathered boulders of each rock (redesign R1): split by hand into rock shards. */
    public static final Map<Rock, DeferredBlock<BoulderBlock>> BOULDER = new EnumMap<>(Rock.class);
    static {
        BLOCKS.addAlias(StrataIndustria.id("loose_stick"), Identifier.withDefaultNamespace("air"));
        BLOCKS.addAlias(StrataIndustria.id("loose_flint"), Identifier.withDefaultNamespace("air"));
    }

    /** Tier 0 fire (spec 3.5). Light 15 while lit. */
    public static final DeferredBlock<FirePitBlock> FIRE_PIT = BLOCKS.registerBlock("fire_pit", FirePitBlock::new,
            p -> p.mapColor(MapColor.PODZOL)
                    .strength(0.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(FirePitBlock.LIT) ? 15 : 0)
                    .pushReaction(PushReaction.POPPED));

    public static final DeferredBlock<CrucibleBlock> CRUCIBLE = BLOCKS.registerBlock("crucible", CrucibleBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(1.0f)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    /** Hand quern (spec 10.1). */
    public static final DeferredBlock<QuernBlock> QUERN = BLOCKS.registerBlock("quern",
            QuernBlock::new,
            p -> p.mapColor(MapColor.STONE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    /** The iron anvil: works wrought iron and steel (anvil tier 4). Stone anvils work copper and bronze (tier 2). */
    public static final DeferredBlock<AnvilBlock> IRON_ANVIL = BLOCKS.registerBlock("iron_anvil", p -> new AnvilBlock(4, false, p),
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.ANVIL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));
    static {
        // The bronze and steel anvils are gone (two anvils: stone and iron); old ones in a world turn into iron anvils.
        for (String old : new String[] {"bronze_anvil", "wrought_iron_anvil", "steel_anvil"}) {
            BLOCKS.addAlias(StrataIndustria.id(old), StrataIndustria.id("iron_anvil"));
        }
    }

    /** Logs stacked for a charcoal pit (spec 4.4). */
    public static final DeferredBlock<LogPileBlock> LOG_PILE = BLOCKS.registerBlock("log_pile", LogPileBlock::new,
            p -> p.mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noLootTable()
                    .lightLevel(state -> state.getValue(LogPileBlock.LIT) ? 8 : 0)
                    .pushReaction(PushReaction.IMMOVEABLE));
    // Drops its charcoal to any tool: losing a whole burn to a bare-handed dig is no lesson. A shovel is just faster.
    public static final DeferredBlock<CharcoalPileBlock> CHARCOAL_PILE = BLOCKS.registerBlock("charcoal_pile", CharcoalPileBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(0.8f)
                    .sound(SoundType.GRAVEL));

    /** Brick forge (spec 4.5). Light 13 while the coals glow. */
    public static final DeferredBlock<ForgeBlock> FORGE = BLOCKS.registerBlock("forge", ForgeBlock::new,
            p -> p.mapColor(MapColor.COLOR_RED)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(ForgeBlock.HOT) ? 13 : state.getValue(ForgeBlock.LIT) ? 7 : 0));

    // Tier 3 spec 3: fire clay and fire bricks. Fire bricks ring a little higher than stone bricks.
    private static final SoundType FIRE_BRICK_SOUND = new SoundType(1.0f, 1.1f, SoundType.DEEPSLATE_BRICKS.getBreakSound(),
            SoundType.DEEPSLATE_BRICKS.getStepSound(), SoundType.DEEPSLATE_BRICKS.getPlaceSound(),
            SoundType.DEEPSLATE_BRICKS.getHitSound(), SoundType.DEEPSLATE_BRICKS.getFallSound());
    public static final DeferredBlock<Block> FIRE_CLAY = BLOCKS.registerSimpleBlock("fire_clay", p -> p
            .mapColor(MapColor.SAND)
            .strength(0.8f)
            .sound(SoundType.MUD));
    public static final DeferredBlock<Block> FIRE_BRICKS = BLOCKS.registerSimpleBlock("fire_bricks", ModBlocks::fireBrick);
    public static final DeferredBlock<SlabBlock> FIRE_BRICK_SLAB = BLOCKS.registerBlock("fire_brick_slab", SlabBlock::new,
            ModBlocks::fireBrick);
    public static final DeferredBlock<StairBlock> FIRE_BRICK_STAIRS = BLOCKS.registerBlock("fire_brick_stairs",
            p -> new StairBlock(FIRE_BRICKS.get().defaultBlockState(), p), ModBlocks::fireBrick);
    public static final DeferredBlock<WallBlock> FIRE_BRICK_WALL = BLOCKS.registerBlock("fire_brick_wall", WallBlock::new,
            p -> fireBrick(p).forceSolidOn());

    /** Bloomery controller (spec 5.1): a fire brick block with a door. Light 13 while it burns. */
    public static final DeferredBlock<BloomeryBlock> BLOOMERY = BLOCKS.registerBlock("bloomery", BloomeryBlock::new,
            p -> fireBrick(p).lightLevel(state -> state.getValue(BloomeryBlock.LIT) ? 13 : 0));

    // Tier 3 spec 7: mechanical power. Wooden parts sound and burn like wood.
    public static final DeferredBlock<AxleBlock> WOODEN_AXLE = BLOCKS.registerBlock("wooden_axle", AxleBlock::new,
            p -> kineticWood(p).noOcclusion());
    public static final DeferredBlock<GearboxBlock> WOODEN_GEARBOX = BLOCKS.registerBlock("wooden_gearbox", GearboxBlock::new,
            ModBlocks::kineticWood);
    public static final DeferredBlock<HandCrankBlock> HAND_CRANK = BLOCKS.registerBlock("hand_crank", HandCrankBlock::new,
            p -> kineticWood(p).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<WaterWheelBlock> WATER_WHEEL = BLOCKS.registerBlock("water_wheel", WaterWheelBlock::new,
            p -> kineticWood(p).noOcclusion());
    public static final DeferredBlock<MillstoneBlock> MILLSTONE = BLOCKS.registerBlock("millstone", MillstoneBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(2.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<BellowsBlock> BELLOWS = BLOCKS.registerBlock("bellows", BellowsBlock::new,
            p -> kineticWood(p).noOcclusion());
    public static final DeferredBlock<SawMillBlock> SAW_MILL = BLOCKS.registerBlock("saw_mill", SawMillBlock::new,
            p -> kineticWood(p).noOcclusion());
    public static final DeferredBlock<dev.strataindustria.machine.CoreSamplerBlock> CORE_SAMPLER = BLOCKS.registerBlock("core_sampler",
            dev.strataindustria.machine.CoreSamplerBlock::new, p -> kineticWood(p).strength(2.5f, 4.0f).noOcclusion());
    public static final DeferredBlock<dev.strataindustria.washing.SluiceBlock> SLUICE = BLOCKS.registerBlock("sluice",
            dev.strataindustria.washing.SluiceBlock::new, p -> kineticWood(p).noOcclusion());
    // Tier 3 spec 7.2 and 7.3: wind power and transmission.
    public static final DeferredBlock<dev.strataindustria.power.StepUpGearboxBlock> STEP_UP_GEARBOX = BLOCKS.registerBlock("step_up_gearbox",
            dev.strataindustria.power.StepUpGearboxBlock::new, ModBlocks::kineticWood);
    public static final DeferredBlock<dev.strataindustria.power.PulleyBlock> PULLEY = BLOCKS.registerBlock("pulley",
            dev.strataindustria.power.PulleyBlock::new, p -> kineticWood(p).noOcclusion());
    public static final DeferredBlock<dev.strataindustria.power.WindmillBearingBlock> WINDMILL_BEARING = BLOCKS.registerBlock("windmill_bearing",
            dev.strataindustria.power.WindmillBearingBlock::new, p -> kineticWood(p).strength(2.5f, 4.0f).noOcclusion());
    public static final DeferredBlock<dev.strataindustria.power.WindmillSailBlock> WINDMILL_SAIL = BLOCKS.registerBlock("windmill_sail",
            dev.strataindustria.power.WindmillSailBlock::new, p -> p.mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL)
                    .noOcclusion().ignitedByLava());
    // Tier 3 spec 12.1: tanning.
    public static final DeferredBlock<dev.strataindustria.tanning.SoakingBarrelBlock> SOAKING_BARREL = BLOCKS.registerBlock("soaking_barrel",
            dev.strataindustria.tanning.SoakingBarrelBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)
                    .noOcclusion().ignitedByLava());
    public static final DeferredBlock<TripHammerBlock> TRIP_HAMMER = BLOCKS.registerBlock("trip_hammer", TripHammerBlock::new,
            p -> kineticWood(p).strength(3.0f, 4.0f).noOcclusion());

    // Tier 3 spec 4.3: deposits that are a single block, whatever the rock around them.
    public static final DeferredBlock<Block> LIGNITE_SEAM = BLOCKS.registerSimpleBlock("lignite_seam", p -> p
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(1.0f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.TUFF));
    /** Bauxite (tier 6 spec 19.4): red-brown laterite in hot biomes, dug with a shovel. */
    public static final DeferredBlock<Block> BAUXITE_BED = BLOCKS.registerSimpleBlock("bauxite_bed", p -> p
            .mapColor(MapColor.TERRACOTTA_RED)
            .strength(0.9f)
            .sound(SoundType.GRAVEL));
    /** Limonite in wet soil (spec 4.2), with a grade like an ore block. */
    public static final DeferredBlock<OreBlock> BOG_IRON = BLOCKS.registerBlock("bog_iron", p -> new OreBlock(null, OreMineral.LIMONITE, p),
            p -> p.mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.9f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.MUD));
    public static final DeferredBlock<ColoredFallingBlock> PLACER_GRAVEL = BLOCKS.registerBlock("placer_gravel",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFF807C7B), p),
            p -> p.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.SNARE).strength(0.6f).sound(SoundType.GRAVEL));
    public static final DeferredBlock<ColoredFallingBlock> PLACER_SAND = BLOCKS.registerBlock("placer_sand",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFFDBD3A0), p),
            p -> p.mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.5f).sound(SoundType.SAND));

    static {
        for (Rock rock : Rock.values()) {
            RAW_ROCK.put(rock, BLOCKS.registerSimpleBlock(rock.id(), p -> p
                    .mapColor(rock.mapColor())
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(rock.hardness(), rock.hardness() * 4)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
            if (rock.category().isIgneous()) {
                STONE_ANVILS.put(rock, BLOCKS.registerBlock(rock.id() + "_anvil", p -> new AnvilBlock(2, true, p), p -> p
                        .mapColor(rock.mapColor())
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .strength(rock.hardness(), rock.hardness() * 4)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE)
                        .pushReaction(PushReaction.IMMOVEABLE)));
            }
            COBBLED_ROCK.put(rock, BLOCKS.registerSimpleBlock("cobbled_" + rock.id(), p -> p
                    .mapColor(rock.mapColor())
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(rock.hardness() + 0.4f, rock.hardness() * 4)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
            BOULDER.put(rock, BLOCKS.registerBlock(rock.id() + "_boulder",
                    p -> new BoulderBlock(rock, p),
                    p -> p.mapColor(rock.mapColor())
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .strength(2.5f, 6.0f)
                            .noOcclusion()
                            .sound(SoundType.STONE)));
            // Rocks, sticks and flint no longer lie about on their own blocks (redesign R1); old ones turn to air.
            BLOCKS.addAlias(StrataIndustria.id("loose_" + rock.id()), Identifier.withDefaultNamespace("air"));

            Map<OreMineral, DeferredBlock<OreBlock>> ores = new EnumMap<>(OreMineral.class);
            for (OreMineral mineral : OreMineral.inRockValues()) {
                ores.put(mineral, BLOCKS.registerBlock(rock.id() + "_" + mineral.id() + "_ore",
                        p -> new OreBlock(rock, mineral, p),
                        p -> p.mapColor(rock.mapColor())
                                .instrument(NoteBlockInstrument.BASEDRUM)
                                .strength(rock.hardness() + 1.0f, rock.hardness() * 4)
                                .requiresCorrectToolForDrops()
                                .sound(SoundType.STONE)));
            }
            ORES.put(rock, ores);
        }
        for (OreMineral mineral : OreMineral.values()) {
            SMALL_ORES.put(mineral, BLOCKS.registerBlock("small_" + mineral.id(),
                    p -> new GroundCoverBlock(Block.box(3, 0, 3, 13, 3, 13), p),
                    p -> groundCover(p).sound(SoundType.GRAVEL)));
        }
    }

    private static Block.Properties kineticWood(Block.Properties p) {
        return p.mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava();
    }

    private static Block.Properties fireBrick(Block.Properties p) {
        return p.mapColor(MapColor.SAND)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(2.5f, 8.0f)
                .requiresCorrectToolForDrops()
                .sound(FIRE_BRICK_SOUND);
    }

    private static Block.Properties groundCover(Block.Properties p) {
        return p.mapColor(MapColor.STONE)
                .noCollision()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.POPPED);
    }

    public static BlockState ore(Rock rock, OreMineral mineral, OreGrade grade) {
        return ORES.get(rock).get(mineral).get().withGrade(grade);
    }

    private ModBlocks() {}
}
