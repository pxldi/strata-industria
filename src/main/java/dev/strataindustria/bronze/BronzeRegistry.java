package dev.strataindustria.bronze;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Touches of the bronze age (uniqueness 4.2, 4.3): the fume hood, and later bells. Registered into the shared registers. */
public final class BronzeRegistry {
    public static final DeferredBlock<FumeHoodBlock> FUME_HOOD = ModBlocks.BLOCKS.registerBlock("fume_hood",
            FumeHoodBlock::new, p -> p.mapColor(MapColor.COLOR_ORANGE).strength(1.5f, 3.0f).requiresCorrectToolForDrops()
                    .noOcclusion().sound(SoundType.COPPER));
    public static final DeferredItem<BlockItem> FUME_HOOD_ITEM = ModItems.ITEMS.registerSimpleBlockItem(FUME_HOOD);

    /** White fumes rising off an arsenical pour. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FUMES = sound("crucible.fumes");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private BronzeRegistry() {}
}
