package dev.strataindustria.transport.ropeway;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.transport.rail.RailwayRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The tier 4 aerial ropeway (outposts spec 8): the drive terminal, the return station, wooden and steel towers, wire
 * rope and the buckets that hang on it.
 */
public final class RopewayRegistry {
    /** Columns a tower head may stand on: fence posts, logs, steel posts. */
    public static final TagKey<Block> TOWER_BASE = TagKey.create(Registries.BLOCK, StrataIndustria.id("ropeway_tower_base"));

    // ---------------------------------------------------------------- blocks
    public static final DeferredBlock<RopewayTerminalBlock> TERMINAL = ModBlocks.BLOCKS.registerBlock("ropeway_terminal",
            RopewayTerminalBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0f).noOcclusion().sound(SoundType.METAL));
    public static final DeferredBlock<RopewayReturnBlock> RETURN = ModBlocks.BLOCKS.registerBlock("ropeway_return",
            RopewayReturnBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0f).noOcclusion().sound(SoundType.METAL));
    public static final DeferredBlock<RopewayTowerBlock> WOODEN_TOWER = ModBlocks.BLOCKS.registerBlock("wooden_ropeway_tower",
            props -> new RopewayTowerBlock(props, false), p -> p.mapColor(MapColor.WOOD).strength(1.5f).noOcclusion().sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<RopewayTowerBlock> STEEL_TOWER = ModBlocks.BLOCKS.registerBlock("steel_ropeway_tower",
            props -> new RopewayTowerBlock(props, true), p -> p.mapColor(MapColor.METAL).strength(2.5f).noOcclusion().sound(SoundType.METAL));

    public static final DeferredBlock<RopewayAngleBlock> ANGLE_STATION = ModBlocks.BLOCKS.registerBlock("ropeway_angle_station",
            RopewayAngleBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0f).noOcclusion().sound(SoundType.METAL));

    // ---------------------------------------------------------------- items
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ModItems.ITEMS.registerSimpleBlockItem(TERMINAL);
    public static final DeferredItem<BlockItem> RETURN_ITEM = ModItems.ITEMS.registerSimpleBlockItem(RETURN);
    public static final DeferredItem<BlockItem> ANGLE_STATION_ITEM = ModItems.ITEMS.registerSimpleBlockItem(ANGLE_STATION);
    public static final DeferredItem<BlockItem> WOODEN_TOWER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(WOODEN_TOWER);
    public static final DeferredItem<BlockItem> STEEL_TOWER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(STEEL_TOWER);
    /** Haul rope: 1 for every 8 blocks of line, rounded up. */
    public static final DeferredItem<WireRopeItem> WIRE_ROPE = ModItems.ITEMS.registerItem("wire_rope", WireRopeItem::new);
    /** A bucket for the line. Put them in the drive terminal. */
    public static final DeferredItem<RopewayBucketItem> BUCKET = ModItems.ITEMS.registerItem("ropeway_bucket", RopewayBucketItem::new);

    // ---------------------------------------------------------------- block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RopewayTerminalBlockEntity>> TERMINAL_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("ropeway_terminal", () -> new BlockEntityType<>(RopewayTerminalBlockEntity::new, TERMINAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RopewayReturnBlockEntity>> RETURN_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("ropeway_return", () -> new BlockEntityType<>(RopewayReturnBlockEntity::new, RETURN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RopewayTowerBlockEntity>> TOWER_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("ropeway_tower", () -> new BlockEntityType<>(RopewayTowerBlockEntity::new, WOODEN_TOWER.get(), STEEL_TOWER.get(), ANGLE_STATION.get()));

    // ---------------------------------------------------------------- the seat
    public static final ResourceKey<EntityType<?>> SEAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, StrataIndustria.id("ropeway_seat"));
    /** The place a rider sits on a bucket's hanger; never saved ({@code shouldBeSaved}), but the type must stay serialisable or players cannot board it. Registered with the railway's entity types, which the mod bus already carries. */
    public static final DeferredHolder<EntityType<?>, EntityType<RopewaySeatEntity>> SEAT =
            RailwayRegistry.ENTITY_TYPES.register("ropeway_seat", () -> EntityType.Builder.<RopewaySeatEntity>of(RopewaySeatEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.2f).passengerAttachments(0.2f).noSummon().fireImmune().clientTrackingRange(16).updateInterval(1).build(SEAT_KEY));

    // ---------------------------------------------------------------- sounds
    /** The bull wheel turning under load; repeated while the line runs. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DRIVE = sound("ropeway.drive");
    /** A bucket's hanger clacking over a tower's sheaves. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEAVE = sound("ropeway.sheave");
    /** A full bucket swung out onto the line at the terminal. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BUCKET_HANG = sound("ropeway.bucket_hang");
    /** A bucket tipping its load out at the return. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BUCKET_TIP = sound("ropeway.bucket_tip");
    /** Wire rope pulled tight over a tower or fixed at a station. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROPE_TIE = sound("ropeway.rope_tie");
    /** The last span made fast: the line is strung. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LINE_STRUNG = sound("ropeway.line_strung");
    /** A line parted. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LINE_SNAP = sound("ropeway.snap");

    /** A bucket swinging round the wheel of an angle station. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANGLE_TURN = sound("ropeway.angle_turn");
    /** A seat clipped onto the line, with a rider in it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAT_CLIP = sound("ropeway.seat_clip");
    /** The rider steps off at a station. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAT_RELEASE = sound("ropeway.seat_release");
    /** Wind past a riding seat. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RIDE_WIND = sound("ropeway.ride_wind");
    /** A bucket topped up from the chest at an angle station. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TOP_UP = sound("ropeway.top_up");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private RopewayRegistry() {}
}
