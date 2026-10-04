package dev.strataindustria.transport.telegraph;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The tier 5 telegraph (outposts spec 9.1): wire on the pole insulators, a key to send, a sounder that clacks, and the
 * dispatch board that lists every outpost that reports in.
 */
public final class TelegraphRegistry {
    // ---------------------------------------------------------------- blocks
    public static final DeferredBlock<TelegraphKeyBlock> KEY = ModBlocks.BLOCKS.registerBlock("telegraph_key",
            TelegraphKeyBlock::new, p -> p.mapColor(MapColor.WOOD).strength(1.0f).noOcclusion().sound(SoundType.WOOD));
    public static final DeferredBlock<TelegraphSounderBlock> SOUNDER = ModBlocks.BLOCKS.registerBlock("telegraph_sounder",
            TelegraphSounderBlock::new, p -> p.mapColor(MapColor.WOOD).strength(1.0f).noOcclusion().sound(SoundType.WOOD));
    public static final DeferredBlock<DispatchBoardBlock> BOARD = ModBlocks.BLOCKS.registerBlock("dispatch_board",
            DispatchBoardBlock::new, p -> p.mapColor(MapColor.COLOR_BLACK).strength(1.5f).noOcclusion().sound(SoundType.WOOD));

    // ---------------------------------------------------------------- items
    public static final DeferredItem<BlockItem> KEY_ITEM = ModItems.ITEMS.registerSimpleBlockItem(KEY);
    public static final DeferredItem<BlockItem> SOUNDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(SOUNDER);
    public static final DeferredItem<BlockItem> BOARD_ITEM = ModItems.ITEMS.registerSimpleBlockItem(BOARD);
    /** Wire for the poles: one for every 8 blocks of span. */
    public static final DeferredItem<TelegraphWireItem> WIRE = ModItems.ITEMS.registerItem("telegraph_wire", TelegraphWireItem::new);

    // ---------------------------------------------------------------- block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TelegraphKeyBlockEntity>> KEY_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("telegraph_key", () -> new BlockEntityType<>(TelegraphKeyBlockEntity::new, KEY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DispatchBoardBlockEntity>> BOARD_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("dispatch_board", () -> new BlockEntityType<>(DispatchBoardBlockEntity::new, BOARD.get()));

    // ---------------------------------------------------------------- sounds
    /** The key going down: a tight click of brass on a wooden base. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KEY_DOWN = sound("telegraph.key");
    /** The key springing back. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KEY_UP = sound("telegraph.key_up");
    /** A signal arrives: the sounder's armature slaps down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUNDER_CLACK = sound("telegraph.sounder");
    /** The armature lifts again, a lighter tick than the clack. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUNDER_LIFT = sound("telegraph.sounder_lift");
    /** A span of wire made fast between two insulators. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_STRUNG = sound("telegraph.wire_strung");
    /** A wire parted when its insulator went. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_SNAP = sound("telegraph.wire_snap");
    /** A drop wire finds its pole. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DROP_HUNG = sound("telegraph.drop_hung");
    /** The first call between two charters gets through. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LINE_OPEN = sound("telegraph.line_open");
    /** Chalk on slate when the board takes a report. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHALK = sound("dispatch_board.update");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private TelegraphRegistry() {}
}
