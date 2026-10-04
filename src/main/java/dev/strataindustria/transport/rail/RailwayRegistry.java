package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The tier 4 steel track and wagons (outposts spec 7.1 and 7.2): steel track, station track, steel buffer, the ore,
 * tank and flat wagons and the wagon fluid port. The locomotive, water tower and coal stage come after.
 */
public final class RailwayRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StrataIndustria.MOD_ID);

    // ---------------------------------------------------------------- blocks
    public static final DeferredBlock<SteelTrackBlock> STEEL_TRACK = ModBlocks.BLOCKS.registerBlock("steel_track",
            SteelTrackBlock::new, p -> p.mapColor(MapColor.METAL).noCollision().strength(1.0f).sound(SoundType.METAL));
    public static final DeferredBlock<StationTrackBlock> STATION_TRACK = ModBlocks.BLOCKS.registerBlock("station_track",
            StationTrackBlock::new, p -> p.mapColor(MapColor.METAL).noCollision().strength(1.2f).sound(SoundType.METAL));
    public static final DeferredBlock<SteelBufferBlock> STEEL_BUFFER = ModBlocks.BLOCKS.registerBlock("steel_buffer",
            SteelBufferBlock::new, p -> p.mapColor(MapColor.METAL).noCollision().strength(1.5f).sound(SoundType.METAL));
    public static final DeferredBlock<WagonFluidPortBlock> WAGON_FLUID_PORT = ModBlocks.BLOCKS.registerBlock("wagon_fluid_port",
            WagonFluidPortBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL));

    // ---------------------------------------------------------------- items
    public static final DeferredItem<BlockItem> STEEL_TRACK_ITEM = ModItems.ITEMS.registerSimpleBlockItem(STEEL_TRACK);
    public static final DeferredItem<BlockItem> STATION_TRACK_ITEM = ModItems.ITEMS.registerSimpleBlockItem(STATION_TRACK);
    public static final DeferredItem<BlockItem> STEEL_BUFFER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(STEEL_BUFFER);
    public static final DeferredItem<BlockItem> WAGON_FLUID_PORT_ITEM = ModItems.ITEMS.registerSimpleBlockItem(WAGON_FLUID_PORT);

    // ---------------------------------------------------------------- wagons
    public static final ResourceKey<EntityType<?>> ORE_WAGON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, StrataIndustria.id("ore_wagon"));
    public static final DeferredHolder<EntityType<?>, EntityType<OreWagonEntity>> ORE_WAGON_ENTITY =
            ENTITY_TYPES.register("ore_wagon", () -> EntityType.Builder.<OreWagonEntity>of(OreWagonEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.8f).clientTrackingRange(8).build(ORE_WAGON_KEY));
    public static final ResourceKey<EntityType<?>> TANK_WAGON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, StrataIndustria.id("tank_wagon"));
    public static final DeferredHolder<EntityType<?>, EntityType<TankWagonEntity>> TANK_WAGON_ENTITY =
            ENTITY_TYPES.register("tank_wagon", () -> EntityType.Builder.<TankWagonEntity>of(TankWagonEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.9f).clientTrackingRange(8).build(TANK_WAGON_KEY));
    public static final ResourceKey<EntityType<?>> FLAT_WAGON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, StrataIndustria.id("flat_wagon"));
    public static final DeferredHolder<EntityType<?>, EntityType<FlatWagonEntity>> FLAT_WAGON_ENTITY =
            ENTITY_TYPES.register("flat_wagon", () -> EntityType.Builder.<FlatWagonEntity>of(FlatWagonEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.5f).clientTrackingRange(8).build(FLAT_WAGON_KEY));

    public static final DeferredItem<MinecartItem> ORE_WAGON = ModItems.ITEMS.registerItem("ore_wagon",
            p -> new MinecartItem(ORE_WAGON_ENTITY.get(), p), p -> p.stacksTo(1));
    public static final DeferredItem<MinecartItem> TANK_WAGON = ModItems.ITEMS.registerItem("tank_wagon",
            p -> new MinecartItem(TANK_WAGON_ENTITY.get(), p), p -> p.stacksTo(1));
    public static final DeferredItem<MinecartItem> FLAT_WAGON = ModItems.ITEMS.registerItem("flat_wagon",
            p -> new MinecartItem(FLAT_WAGON_ENTITY.get(), p), p -> p.stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WagonFluidPortBlockEntity>> WAGON_FLUID_PORT_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("wagon_fluid_port",
                    () -> new BlockEntityType<>(WagonFluidPortBlockEntity::new, WAGON_FLUID_PORT.get()));

    // ---------------------------------------------------------------- sounds
    /** Steel wheels over rail joints. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CLATTER_STEEL = sound("rail.clatter_steel");
    /** A wagon running into the steel buffer. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BUFFER_CLANG = sound("steel_buffer.clang");
    /** Fluid running between a tank wagon and the pipes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PORT_FLOW = sound("wagon_port.flow");
    /** A flat wagon takes a block on its deck or sets it down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAT_LOAD = sound("flat_wagon.load");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    private RailwayRegistry() {}
}
