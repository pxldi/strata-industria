package dev.strataindustria.transport.signal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** The tier 5 line control (outposts spec 9.4): the block signal, the timetable and the route switch. */
public final class SignalRegistry {
    // ---------------------------------------------------------------- blocks
    public static final DeferredBlock<SignalBlock> BLOCK_SIGNAL = ModBlocks.BLOCKS.registerBlock("block_signal",
            SignalBlock::new, p -> p.mapColor(MapColor.METAL).strength(1.5f).noOcclusion().sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(SignalBlock.CLEAR) ? 0 : 7));
    public static final DeferredBlock<RouteSwitchBlock> ROUTE_SWITCH = ModBlocks.BLOCKS.registerBlock("route_switch",
            RouteSwitchBlock::new, p -> p.mapColor(MapColor.METAL).noCollision().strength(1.2f).sound(SoundType.METAL));

    // ---------------------------------------------------------------- items
    public static final DeferredItem<BlockItem> BLOCK_SIGNAL_ITEM = ModItems.ITEMS.registerSimpleBlockItem(BLOCK_SIGNAL);
    public static final DeferredItem<BlockItem> ROUTE_SWITCH_ITEM = ModItems.ITEMS.registerSimpleBlockItem(ROUTE_SWITCH);
    public static final DeferredItem<TimetableItem> TIMETABLE = ModItems.ITEMS.registerItem("timetable", TimetableItem::new, p -> p.stacksTo(1));

    // ---------------------------------------------------------------- data
    /** The lines of a timetable item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TimetableStops>> TIMETABLE_STOPS =
            ModDataComponents.COMPONENTS.registerComponentType("timetable_stops", b -> b
                    .persistent(TimetableStops.CODEC)
                    .networkSynchronized(TimetableStops.STREAM_CODEC));

    // ---------------------------------------------------------------- block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RouteSwitchBlockEntity>> ROUTE_SWITCH_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("route_switch", () -> new BlockEntityType<>(RouteSwitchBlockEntity::new, ROUTE_SWITCH.get()));

    // ---------------------------------------------------------------- sounds
    /** The semaphore arm swings: a clunk of iron on its stop. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SIGNAL_ARM = sound("signal.arm");
    /** The switch blades are thrown. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SWITCH_THROW = sound("route_switch.throw");
    /** A timetable goes into a vehicle. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TIMETABLE_LOAD = sound("timetable.load");
    /** A vehicle on a timetable stands at its stop: a little chime, brighter when a round is done. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TIMETABLE_ARRIVE = sound("timetable.arrive");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private SignalRegistry() {}
}
