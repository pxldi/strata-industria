package dev.strataindustria.registry;

import dev.strataindustria.oil.CrudeOilBlock;
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

    public static void init() {}

    private Tier6Blocks() {}
}
