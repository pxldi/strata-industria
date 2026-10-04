package dev.strataindustria.logistics;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidFilterBlock;
import dev.strataindustria.fluid.FluidFilterBlockEntity;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModMenus;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Tier 5 logistics (spec 12): item pipes, the extractors, the storage controller and the fluid filter, in one place. */
public final class Tier5Logistics {
    /** Spec 23.6: item pipes ring like glass a little high when broken and clink like copper when placed. */
    public static final SoundType ITEM_PIPE_SOUND = new DeferredSoundType(1.0f, 1.3f, () -> net.minecraft.sounds.SoundEvents.GLASS_BREAK,
            () -> net.minecraft.sounds.SoundEvents.GLASS_STEP, () -> net.minecraft.sounds.SoundEvents.COPPER_PLACE,
            () -> net.minecraft.sounds.SoundEvents.GLASS_HIT, () -> net.minecraft.sounds.SoundEvents.GLASS_FALL);

    // ------------------------------------------------------------------ blocks
    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE = ModBlocks.BLOCKS.registerBlock("item_pipe", ItemPipeBlock::new,
            p -> p.mapColor(MapColor.GOLD).strength(0.6f, 3.0f).sound(ITEM_PIPE_SOUND).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<PipeExtractorBlock> PIPE_EXTRACTOR = ModBlocks.BLOCKS.registerBlock("pipe_extractor",
            p -> new PipeExtractorBlock(false, p),
            p -> p.mapColor(MapColor.GOLD).strength(1.0f, 3.0f).sound(ITEM_PIPE_SOUND).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<PipeExtractorBlock> FAST_PIPE_EXTRACTOR = ModBlocks.BLOCKS.registerBlock("fast_pipe_extractor",
            p -> new PipeExtractorBlock(true, p),
            p -> p.mapColor(MapColor.GOLD).strength(1.0f, 3.0f).sound(ITEM_PIPE_SOUND).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<StorageControllerBlock> STORAGE_CONTROLLER = ModBlocks.BLOCKS.registerBlock("storage_controller",
            StorageControllerBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(4.0f, 6.0f).requiresCorrectToolForDrops().sound(dev.strataindustria.registry.Tier5Blocks.MACHINE_SOUND));
    public static final DeferredBlock<FluidFilterBlock> FLUID_FILTER = ModBlocks.BLOCKS.registerBlock("fluid_filter", FluidFilterBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER).noOcclusion());

    // ------------------------------------------------------------------ items
    public static final DeferredItem<BlockItem> ITEM_PIPE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(ITEM_PIPE);
    public static final DeferredItem<BlockItem> PIPE_EXTRACTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem(PIPE_EXTRACTOR);
    public static final DeferredItem<BlockItem> FAST_PIPE_EXTRACTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem(FAST_PIPE_EXTRACTOR);
    public static final DeferredItem<BlockItem> STORAGE_CONTROLLER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(STORAGE_CONTROLLER);
    public static final DeferredItem<BlockItem> FLUID_FILTER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(FLUID_FILTER);

    // ------------------------------------------------------------------ block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("item_pipe", () -> new BlockEntityType<>(ItemPipeBlockEntity::new, ITEM_PIPE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeExtractorBlockEntity>> PIPE_EXTRACTOR_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("pipe_extractor", () -> new BlockEntityType<>(PipeExtractorBlockEntity::new,
                    PIPE_EXTRACTOR.get(), FAST_PIPE_EXTRACTOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageControllerBlockEntity>> STORAGE_CONTROLLER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("storage_controller", () -> new BlockEntityType<>(StorageControllerBlockEntity::new,
                    STORAGE_CONTROLLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidFilterBlockEntity>> FLUID_FILTER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("fluid_filter", () -> new BlockEntityType<>(FluidFilterBlockEntity::new, FLUID_FILTER.get()));

    // ------------------------------------------------------------------ screen
    public static final DeferredHolder<MenuType<?>, MenuType<StorageControllerMenu>> STORAGE_CONTROLLER_MENU =
            ModMenus.MENUS.register("storage_controller", () -> IMenuTypeExtension.create(StorageControllerMenu::new));

    // ------------------------------------------------------------------ sounds
    /** A soft pneumatic puff when an extractor pulls. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE_EXTRACT = sound("block.item_pipe.extract");
    /** A relay clack and a soft boot chime. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTROLLER_OPEN = sound("block.storage_controller.open");
    /** A short power-down blip. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTROLLER_CLOSE = sound("block.storage_controller.close");
    /** A light tick when the screen stores or takes a stack. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTROLLER_STORE = sound("block.storage_controller.store");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private Tier5Logistics() {}
}
