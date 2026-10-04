package dev.strataindustria.registry;

import dev.strataindustria.ceramics.RefractoryCrucibleBlock;
import dev.strataindustria.coking.CokeOvenBlock;
import dev.strataindustria.smithing.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Tier 4 (steel and steam) blocks, kept apart from the earlier tiers' blocks. */
public final class Tier4Blocks {
    // Spec 21.7: the fire brick sound, a touch deeper for the heavier brick.
    private static final SoundType COKE_OVEN_SOUND = new SoundType(1.0f, 1.0f, SoundType.DEEPSLATE_BRICKS.getBreakSound(),
            SoundType.DEEPSLATE_BRICKS.getStepSound(), SoundType.DEEPSLATE_BRICKS.getPlaceSound(),
            SoundType.DEEPSLATE_BRICKS.getHitSound(), SoundType.DEEPSLATE_BRICKS.getFallSound());
    private static final SoundType COKE_SOUND = new SoundType(1.0f, 1.2f, SoundType.BASALT.getBreakSound(),
            SoundType.BASALT.getStepSound(), SoundType.BASALT.getPlaceSound(),
            SoundType.BASALT.getHitSound(), SoundType.BASALT.getFallSound());

    // Spec 5.1: the coke oven.
    public static final DeferredBlock<Block> COKE_OVEN_BRICKS = ModBlocks.BLOCKS.registerSimpleBlock("coke_oven_bricks", Tier4Blocks::cokeOven);
    public static final DeferredBlock<CokeOvenBlock> COKE_OVEN_DOOR = ModBlocks.BLOCKS.registerBlock("coke_oven_door", CokeOvenBlock::new,
            p -> cokeOven(p).lightLevel(state -> state.getValue(CokeOvenBlock.LIT) ? 10 : 0));
    /** Nine coke, a compact fuel. */
    public static final DeferredBlock<Block> COKE_BLOCK = ModBlocks.BLOCKS.registerSimpleBlock("coke_block", p -> p
            .mapColor(MapColor.COLOR_GRAY)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(4.0f, 6.0f)
            .requiresCorrectToolForDrops()
            .sound(COKE_SOUND));

    // Spec 6.1: the refractory crucible, rated to 1700 degrees; it melts iron.
    public static final DeferredBlock<RefractoryCrucibleBlock> REFRACTORY_CRUCIBLE = ModBlocks.BLOCKS.registerBlock("refractory_crucible",
            RefractoryCrucibleBlock::new, p -> p.mapColor(MapColor.SAND)
                    .strength(1.5f)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    // Spec 4.6: creosote-treated wood, which never rots.
    public static final DeferredBlock<Block> TREATED_PLANKS = ModBlocks.BLOCKS.registerSimpleBlock("treated_planks", Tier4Blocks::treated);
    public static final DeferredBlock<SlabBlock> TREATED_SLAB = ModBlocks.BLOCKS.registerBlock("treated_slab", SlabBlock::new, Tier4Blocks::treated);
    public static final DeferredBlock<StairBlock> TREATED_STAIRS = ModBlocks.BLOCKS.registerBlock("treated_stairs",
            p -> new StairBlock(TREATED_PLANKS.get().defaultBlockState(), p), Tier4Blocks::treated);
    public static final DeferredBlock<FenceBlock> TREATED_FENCE = ModBlocks.BLOCKS.registerBlock("treated_fence", FenceBlock::new,
            p -> treated(p).forceSolidOn());

    /** Tier 4 spec 14.4: the steel anvil, anvil tier 5 and the tier 4 exit item. */
    public static final DeferredBlock<AnvilBlock> STEEL_ANVIL = ModBlocks.BLOCKS.registerBlock("steel_anvil", p -> new AnvilBlock(5, false, p),
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.ANVIL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    private static Block.Properties cokeOven(Block.Properties p) {
        return p.mapColor(MapColor.TERRACOTTA_BROWN)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(2.5f, 8.0f)
                .requiresCorrectToolForDrops()
                .sound(COKE_OVEN_SOUND);
    }

    /** Treated wood is tougher than plain planks and does not catch fire from lava. */
    private static Block.Properties treated(Block.Properties p) {
        return p.mapColor(MapColor.COLOR_BROWN)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.5f, 4.0f)
                .sound(SoundType.WOOD);
    }

    public static void init() {}

    private Tier4Blocks() {}
}
