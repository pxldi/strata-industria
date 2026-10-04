package dev.strataindustria.registry;

import dev.strataindustria.oil.CrudeOilBlock;
import dev.strataindustria.oil.OilStillBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Tier 6 (industrial) blocks, kept apart from the earlier tiers' blocks. */
public final class Tier6Blocks {
    /** Spec 5.1: crude oil in the world, found in seep pools. Slows whatever wades through it like honey. */
    public static final DeferredBlock<CrudeOilBlock> CRUDE_OIL = ModBlocks.BLOCKS.registerBlock("crude_oil",
            p -> new CrudeOilBlock(Tier6Fluids.CRUDE_OIL.source().get(), p),
            p -> p.mapColor(MapColor.COLOR_BLACK).replaceable().noCollision().strength(100.0f).speedFactor(0.4f)
                    .pushReaction(PushReaction.POPPED).noLootTable().liquid().sound(SoundType.EMPTY));

    /** Spec 5.5: the hand-scale refinery, a copper pot on a fire brick base. */
    public static final DeferredBlock<OilStillBlock> OIL_STILL = ModBlocks.BLOCKS.registerBlock("oil_still", OilStillBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER)
                    .lightLevel(state -> state.getValue(OilStillBlock.LIT) ? 6 : 0));

    public static void init() {}

    private Tier6Blocks() {}
}
