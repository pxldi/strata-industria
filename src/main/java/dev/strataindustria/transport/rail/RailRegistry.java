package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The wooden tramway (outposts spec 5): wooden rail, tub stop, tipple, buffer and the mine tub. Blocks and items
 * go into the shared registers; the tub entity and the sounds have their own here.
 */
public final class RailRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StrataIndustria.MOD_ID);

    /** Every rail block this mod adds; routes only follow these (outposts spec 12). */
    public static final TagKey<Block> TRACK = TagKey.create(Registries.BLOCK, StrataIndustria.id("track"));

    // ---------------------------------------------------------------- blocks
    public static final DeferredBlock<WoodenRailBlock> WOODEN_RAIL = ModBlocks.BLOCKS.registerBlock("wooden_rail",
            WoodenRailBlock::new, p -> p.mapColor(MapColor.WOOD).noCollision().strength(0.7f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<TubStopBlock> TUB_STOP = ModBlocks.BLOCKS.registerBlock("tub_stop",
            TubStopBlock::new, p -> p.mapColor(MapColor.WOOD).noCollision().strength(0.9f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<TippleRailBlock> TIPPLE_RAIL = ModBlocks.BLOCKS.registerBlock("tipple_rail",
            TippleRailBlock::new, p -> p.mapColor(MapColor.WOOD).noCollision().strength(0.9f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<RailBufferBlock> RAIL_BUFFER = ModBlocks.BLOCKS.registerBlock("rail_buffer",
            RailBufferBlock::new, p -> p.mapColor(MapColor.WOOD).noCollision().strength(1.0f).sound(SoundType.WOOD).ignitedByLava());

    // ---------------------------------------------------------------- items
    public static final DeferredItem<BlockItem> WOODEN_RAIL_ITEM = ModItems.ITEMS.registerSimpleBlockItem(WOODEN_RAIL);
    public static final DeferredItem<BlockItem> TUB_STOP_ITEM = ModItems.ITEMS.registerSimpleBlockItem(TUB_STOP);
    public static final DeferredItem<BlockItem> TIPPLE_RAIL_ITEM = ModItems.ITEMS.registerSimpleBlockItem(TIPPLE_RAIL);
    public static final DeferredItem<BlockItem> RAIL_BUFFER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(RAIL_BUFFER);

    // ---------------------------------------------------------------- entity and block entity
    public static final ResourceKey<EntityType<?>> MINE_TUB_KEY = ResourceKey.create(Registries.ENTITY_TYPE, StrataIndustria.id("mine_tub"));
    public static final DeferredHolder<EntityType<?>, EntityType<MineTubEntity>> MINE_TUB_ENTITY =
            ENTITY_TYPES.register("mine_tub", () -> EntityType.Builder.<MineTubEntity>of(MineTubEntity::new, MobCategory.MISC)
                    .sized(0.9f, 0.7f).clientTrackingRange(8).build(MINE_TUB_KEY));

    public static final DeferredItem<MinecartItem> MINE_TUB = ModItems.ITEMS.registerItem("mine_tub",
            p -> new MinecartItem(MINE_TUB_ENTITY.get(), p), p -> p.stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubStopBlockEntity>> TUB_STOP_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("tub_stop", () -> new BlockEntityType<>(TubStopBlockEntity::new, TUB_STOP.get()));

    // ---------------------------------------------------------------- sounds
    /** A tub's wheels clacking over the sleepers. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TUB_ROLL = sound("tub.roll_wood");
    /** Two tubs joined with a chain. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TUB_COUPLE = sound("tub.couple");
    /** A tub at a stop; the brake block catches the wheels. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TUB_STOP_BRAKE = sound("tub_stop.brake");
    /** A tub tipping its load into the bin below. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TIPPLE_DUMP = sound("tipple.dump");
    /** A tub running into the buffer. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TUB_THUD = sound("tub.thud");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    private RailRegistry() {}
}
