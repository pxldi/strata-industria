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

/** The tier 5 electric line (outposts spec 9.2 and 9.3): trolley bracket and wire, and the electric tram. */
public final class TramRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StrataIndustria.MOD_ID);

    public static final DeferredBlock<TrolleyBracketBlock> TROLLEY_BRACKET = ModBlocks.BLOCKS.registerBlock("trolley_bracket",
            TrolleyBracketBlock::new, p -> p.mapColor(MapColor.METAL).strength(1.5f).noOcclusion().sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> TROLLEY_BRACKET_ITEM = ModItems.ITEMS.registerSimpleBlockItem(TROLLEY_BRACKET);
    public static final DeferredItem<TrolleyWireItem> TROLLEY_WIRE = ModItems.ITEMS.registerItem("trolley_wire", TrolleyWireItem::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrolleyBracketBlockEntity>> TROLLEY_BRACKET_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("trolley_bracket",
                    () -> new BlockEntityType<>(TrolleyBracketBlockEntity::new, TROLLEY_BRACKET.get()));

    public static final ResourceKey<EntityType<?>> ELECTRIC_TRAM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, StrataIndustria.id("electric_tram"));
    public static final DeferredHolder<EntityType<?>, EntityType<ElectricTramEntity>> ELECTRIC_TRAM_ENTITY =
            ENTITY_TYPES.register("electric_tram", () -> EntityType.Builder.<ElectricTramEntity>of(ElectricTramEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.1f).clientTrackingRange(8).build(ELECTRIC_TRAM_KEY));
    public static final DeferredItem<MinecartItem> ELECTRIC_TRAM = ModItems.ITEMS.registerItem("electric_tram",
            p -> new MinecartItem(ELECTRIC_TRAM_ENTITY.get(), p), p -> p.stacksTo(1));

    // ---------------------------------------------------------------- sounds
    /** The motor's hum, pitch rising with speed. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MOTOR = sound("tram.motor");
    /** The trolley shoe crackling on a wire joint. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SPARK = sound("tram.spark");
    /** The bell: on the key, on arriving and on pulling away. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL = sound("tram.bell");
    /** The pole springing up and touching the wire. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTACT = sound("tram.contact");
    /** The shoe leaving the wire. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LOSE_WIRE = sound("tram.lose_wire");
    /** A trolley wire clamped between two brackets, and let go. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_STRUNG = sound("trolley_wire.strung");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_CUT = sound("trolley_wire.cut");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    private TramRegistry() {}
}
