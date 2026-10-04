package dev.strataindustria.registry;

import dev.strataindustria.oil.CrudeOilBlock;
import dev.strataindustria.oil.OilStillBlock;
import dev.strataindustria.oil.PumpJackBlock;
import dev.strataindustria.oil.SeismicChargeBlock;
import dev.strataindustria.oil.WellheadBlock;
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

    /** Spec 5.2: a survey charge, paper and clay; lit it thumps once and breaks nothing. */
    public static final DeferredBlock<SeismicChargeBlock> SEISMIC_CHARGE = ModBlocks.BLOCKS.registerBlock("seismic_charge", SeismicChargeBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_WHITE).strength(0.2f).sound(SoundType.CANDLE).noOcclusion().pushReaction(PushReaction.POPPED)
                    .lightLevel(state -> state.getValue(SeismicChargeBlock.LIT) ? 4 : 0));
    /** Spec 5.3: the drilling head over a reservoir. */
    public static final DeferredBlock<WellheadBlock> WELLHEAD = ModBlocks.BLOCKS.registerBlock("wellhead", WellheadBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(4.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).noOcclusion());
    /** Spec 5.4: the nodding donkey on a drilled wellhead. */
    public static final DeferredBlock<PumpJackBlock> PUMP_JACK = ModBlocks.BLOCKS.registerBlock("pump_jack", PumpJackBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(4.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).noOcclusion());

    public static void init() {}

    private Tier6Blocks() {}
}
