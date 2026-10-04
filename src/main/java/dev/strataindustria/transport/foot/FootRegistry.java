package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tier 2 on foot (outposts and transport spec 4): rope and the rope ladder, the pack frame, the handcart, and the
 * cairns and blazes that mark a trail. Blocks and items go into the shared registers; the handcart entity and its
 * sounds have registers here.
 */
public final class FootRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StrataIndustria.MOD_ID);

    // ---------------------------------------------------------------- items
    public static final DeferredItem<RopeItem> ROPE = ModItems.ITEMS.registerItem("rope", RopeItem::new);

    public static final ResourceKey<EquipmentAsset> PACK_FRAME_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, StrataIndustria.id("pack_frame"));
    public static final DeferredItem<PackFrameItem> PACK_FRAME = ModItems.ITEMS.registerItem("pack_frame",
            PackFrameItem::new, p -> p.stacksTo(1).component(DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.CHEST).setAsset(PACK_FRAME_ASSET).build()));

    public static final DeferredItem<HandcartItem> HANDCART = ModItems.ITEMS.registerItem("handcart",
            HandcartItem::new, p -> p.stacksTo(1));

    // ---------------------------------------------------------------- blocks
    public static final DeferredBlock<RopeLadderBlock> ROPE_LADDER = ModBlocks.BLOCKS.registerBlock("rope_ladder",
            RopeLadderBlock::new, p -> p.mapColor(MapColor.COLOR_BROWN).strength(0.3f).sound(SoundType.WOOL).noCollision()
                    .noOcclusion().pushReaction(PushReaction.POPPED).ignitedByLava());

    public static final DeferredBlock<CairnBlock> CAIRN = ModBlocks.BLOCKS.registerBlock("cairn",
            CairnBlock::new, p -> p.mapColor(MapColor.STONE).strength(1.0f).sound(SoundType.STONE).noOcclusion());

    public static final DeferredBlock<BlazeMarkBlock> BLAZE_MARK = ModBlocks.BLOCKS.registerBlock("blaze_mark",
            BlazeMarkBlock::new, p -> p.mapColor(MapColor.WOOD).instabreak().sound(SoundType.WOOD).noCollision().noOcclusion()
                    .noLootTable().pushReaction(PushReaction.POPPED));

    // ---------------------------------------------------------------- data
    /** The nine stacks of a pack frame (spec 4.2). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> PACK_CONTENTS =
            ModDataComponents.COMPONENTS.registerComponentType("pack_contents", b -> b
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    // ---------------------------------------------------------------- entity
    public static final ResourceKey<EntityType<?>> HANDCART_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, StrataIndustria.id("handcart"));
    public static final DeferredHolder<EntityType<?>, EntityType<HandcartEntity>> HANDCART_ENTITY =
            ENTITY_TYPES.register("handcart", () -> EntityType.Builder.<HandcartEntity>of(HandcartEntity::new, MobCategory.MISC)
                    .sized(1.3f, 0.9f).clientTrackingRange(8).updateInterval(2).build(HANDCART_KEY));

    // ---------------------------------------------------------------- sounds
    /** Rope paid out down the underside of a block. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROPE_HANG = sound("rope.hang");
    /** A pack frame opened or its straps worked. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_OPEN = sound("pack.open");
    /** Rocks stacked into a cairn. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CAIRN_STACK = sound("cairn.stack");
    /** A knife cutting a blaze in bark. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLAZE_CUT = sound("blaze.cut");
    /** A handcart rattling along behind its puller. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCART_ROLL = sound("handcart.roll");
    /** The shafts taken up or let down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCART_SHAFTS = sound("handcart.shafts");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    /** The loose rock item that stacks into a cairn of this rock. */
    public static Supplier<? extends Item> looseRock(Rock rock) {
        return ModItems.LOOSE_ROCK.get(rock);
    }

    public static void init() {}

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    private FootRegistry() {}
}
