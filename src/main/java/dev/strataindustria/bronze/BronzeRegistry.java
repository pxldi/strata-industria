package dev.strataindustria.bronze;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
    public static final DeferredItem<BlockItem> FUME_HOOD_ITEM = ModItems.ITEMS.registerItem("fume_hood",
            p -> new HintBlockItem(FUME_HOOD.get(), p, "item." + StrataIndustria.MOD_ID + ".fume_hood.hint", false));

    /** A cast bell, hung from the block above. */
    public static final DeferredBlock<BellBlock> BELL = ModBlocks.BLOCKS.registerBlock("bell",
            BellBlock::new, p -> p.mapColor(MapColor.GOLD).strength(2.0f, 4.0f).requiresCorrectToolForDrops()
                    .noOcclusion().sound(SoundType.COPPER));
    public static final DeferredItem<BlockItem> BELL_ITEM = ModItems.ITEMS.registerItem("bell",
            p -> new HintBlockItem(BELL.get(), p.stacksTo(1), "item." + StrataIndustria.MOD_ID + ".bell.hint", true));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BellBlockEntity>> BELL_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("bell", () -> new BlockEntityType<>(BellBlockEntity::new, BELL.get()));
    /** How a bell was cast, kept on the item and the block. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BellTone>> BELL_TONE =
            ModDataComponents.COMPONENTS.registerComponentType("bell_tone", b -> b
                    .persistent(BellTone.CODEC)
                    .networkSynchronized(BellTone.STREAM_CODEC));
    /** Metal units a bell takes: two ingots' worth, like a sword blade. */
    public static final int BELL_UNITS = 200;
    public static final DeferredItem<Item> UNFIRED_BELL_MOLD = ModItems.ITEMS.registerSimpleItem("unfired_bell_mold", p -> p.stacksTo(16));
    public static final DeferredItem<CastMoldItem> BELL_MOLD = ModItems.ITEMS.registerItem("bell_mold",
            p -> CastMoldItem.bell(p), p -> p.stacksTo(16));

    /** A bell ringing: the vanilla bell sample at the pitch the alloy gave it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL_RING = sound("bell.ring");
    /** White fumes rising off an arsenical pour. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FUMES = sound("crucible.fumes");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private BronzeRegistry() {}
}
