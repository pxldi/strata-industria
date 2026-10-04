package dev.strataindustria.registry;

import dev.strataindustria.quern.QuernBlock;
import dev.strataindustria.smithing.AnvilBlock;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.GroundCoverBlock;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.ceramics.CrucibleBlock;
import dev.strataindustria.charcoal.CharcoalPileBlock;
import dev.strataindustria.charcoal.LogPileBlock;
import dev.strataindustria.ceramics.LargeVesselBlock;
import dev.strataindustria.ceramics.PitKilnBlock;
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
    public static final Map<Rock, DeferredBlock<GroundCoverBlock>> LOOSE_ROCK = new EnumMap<>(Rock.class);
    /** Ore blocks for every (rock, mineral) pair; the grade is a blockstate. */
    public static final Map<Rock, Map<OreMineral, DeferredBlock<OreBlock>>> ORES = new EnumMap<>(Rock.class);
    /** Surface indicators placed above veins. */
    public static final Map<OreMineral, DeferredBlock<GroundCoverBlock>> SMALL_ORES = new EnumMap<>(OreMineral.class);

    public static final DeferredBlock<GroundCoverBlock> LOOSE_STICK = BLOCKS.registerBlock("loose_stick",
            p -> new GroundCoverBlock(Block.box(1, 0, 1, 15, 1, 15), p), ModBlocks::groundCover);
    public static final DeferredBlock<GroundCoverBlock> LOOSE_FLINT = BLOCKS.registerBlock("loose_flint",
            p -> new GroundCoverBlock(Block.box(4, 0, 4, 12, 1, 12), p), ModBlocks::groundCover);

    /** Tier 0 fire (spec 3.5). Light 15 while lit. */
    public static final DeferredBlock<FirePitBlock> FIRE_PIT = BLOCKS.registerBlock("fire_pit", FirePitBlock::new,
            p -> p.mapColor(MapColor.PODZOL)
                    .strength(0.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(FirePitBlock.LIT) ? 15 : 0)
                    .pushReaction(PushReaction.POPPED));

    /** Clay pieces under straw and logs (spec 4.2). Light 15 while burning. */
    public static final DeferredBlock<PitKilnBlock> PIT_KILN = BLOCKS.registerBlock("pit_kiln", PitKilnBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.6f)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> state.getValue(PitKilnBlock.LIT) ? 15 : 0)
                    .pushReaction(PushReaction.IMMOVEABLE));
    /** Fired clay storage jar (spec 4.3). */
    public static final DeferredBlock<LargeVesselBlock> LARGE_VESSEL = BLOCKS.registerBlock("large_vessel", LargeVesselBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(1.0f)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()
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

    /** Bronze anvil (spec 9.1): tier 3, the tier 2 exit item. */
    public static final DeferredBlock<AnvilBlock> BRONZE_ANVIL = BLOCKS.registerBlock("bronze_anvil", p -> new AnvilBlock(3, false, p),
            p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.ANVIL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    /** Logs stacked for a charcoal pit (spec 4.4). */
    public static final DeferredBlock<LogPileBlock> LOG_PILE = BLOCKS.registerBlock("log_pile", LogPileBlock::new,
            p -> p.mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noLootTable()
                    .lightLevel(state -> state.getValue(LogPileBlock.LIT) ? 8 : 0)
                    .pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<CharcoalPileBlock> CHARCOAL_PILE = BLOCKS.registerBlock("charcoal_pile", CharcoalPileBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(0.8f)
                    .sound(SoundType.GRAVEL)
                    .requiresCorrectToolForDrops());

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

    // Tier 3 spec 4.3: deposits that are a single block, whatever the rock around them.
    public static final DeferredBlock<Block> LIGNITE_SEAM = BLOCKS.registerSimpleBlock("lignite_seam", p -> p
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(1.0f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.TUFF));
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
            LOOSE_ROCK.put(rock, BLOCKS.registerBlock("loose_" + rock.id(),
                    p -> new GroundCoverBlock(Block.box(4, 0, 4, 12, 2, 12), p),
                    p -> groundCover(p).mapColor(rock.mapColor()).sound(SoundType.STONE)));

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
