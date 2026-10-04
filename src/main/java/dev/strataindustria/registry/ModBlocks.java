package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.GroundCoverBlock;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.ceramics.CrucibleBlock;
import dev.strataindustria.ceramics.LargeVesselBlock;
import dev.strataindustria.ceramics.PitKilnBlock;
import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
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

    static {
        for (Rock rock : Rock.values()) {
            RAW_ROCK.put(rock, BLOCKS.registerSimpleBlock(rock.id(), p -> p
                    .mapColor(rock.mapColor())
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(rock.hardness(), rock.hardness() * 4)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
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
            for (OreMineral mineral : OreMineral.values()) {
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
