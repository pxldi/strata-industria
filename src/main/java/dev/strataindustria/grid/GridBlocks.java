package dev.strataindustria.grid;

import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Tier 5 extras (uniqueness 2.1, 7.1, 7.3): the stethoscope, the electric lamp and the Leyden jar, registered into the shared registers. */
public final class GridBlocks {
    public static final DeferredBlock<ElectricLampBlock> ELECTRIC_LAMP = ModBlocks.BLOCKS.registerBlock("electric_lamp", ElectricLampBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.GLASS).lightLevel(state -> state.getValue(ElectricLampBlock.LEVEL)));
    public static final DeferredItem<BlockItem> ELECTRIC_LAMP_ITEM = ModItems.ITEMS.registerSimpleBlockItem(ELECTRIC_LAMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricLampBlockEntity>> ELECTRIC_LAMP_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("electric_lamp", () -> new BlockEntityType<>(ElectricLampBlockEntity::new, ELECTRIC_LAMP.get()));

    public static final DeferredBlock<LeydenJarBlock> LEYDEN_JAR = ModBlocks.BLOCKS.registerBlock("leyden_jar", LeydenJarBlock::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(state -> state.getValue(LeydenJarBlock.CHARGE) >= 3 ? 3 : 0));
    public static final DeferredItem<BlockItem> LEYDEN_JAR_ITEM = ModItems.ITEMS.registerSimpleBlockItem(LEYDEN_JAR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeydenJarBlockEntity>> LEYDEN_JAR_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("leyden_jar", () -> new BlockEntityType<>(LeydenJarBlockEntity::new, LEYDEN_JAR.get()));

    /** Uniqueness 2.1: hold it to a machine and it tells you in a line how the machine is doing, with a sound cue. */
    public static final DeferredItem<StethoscopeItem> STETHOSCOPE = ModItems.ITEMS.registerItem("stethoscope", StethoscopeItem::new, p -> p.stacksTo(1));

    public static void init() {
        GridSounds.init();
    }

    private GridBlocks() {}
}
