package dev.strataindustria.cabinet;

import dev.strataindustria.StrataIndustria;
import com.mojang.serialization.Codec;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** The specimen cabinet (uniqueness 9.5), registered into the shared registers. */
public final class CabinetRegistry {
    public static final DeferredBlock<SpecimenCabinetBlock> SPECIMEN_CABINET = ModBlocks.BLOCKS.registerBlock("specimen_cabinet",
            SpecimenCabinetBlock::new, p -> p.mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> SPECIMEN_CABINET_ITEM = ModItems.ITEMS.registerSimpleBlockItem(SPECIMEN_CABINET);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpecimenCabinetBlockEntity>> CABINET_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("specimen_cabinet",
                    () -> new BlockEntityType<>(SpecimenCabinetBlockEntity::new, SPECIMEN_CABINET.get()));

    /** The specimens in a cabinet, kept on the item when it is picked up. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> HELD =
            ModDataComponents.COMPONENTS.registerComponentType("cabinet_specimens", b -> b
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    /** A specimen set on its shelf. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SET = sound("cabinet.set");
    /** A shelf filled. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELF = sound("cabinet.shelf");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    public static void register(IEventBus bus) {
        Shelves.register(bus);
    }

    private CabinetRegistry() {}
}
