package dev.strataindustria.flora;

import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Blocks and items of the indicator plants, registered into the shared registers. */
public final class FloraBlocks {
    public static final Map<IndicatorPlant, DeferredBlock<IndicatorPlantBlock>> PLANTS = new EnumMap<>(IndicatorPlant.class);
    public static final Map<IndicatorPlant, DeferredItem<BlockItem>> ITEMS = new EnumMap<>(IndicatorPlant.class);

    static {
        for (IndicatorPlant plant : IndicatorPlant.values()) {
            var block = ModBlocks.BLOCKS.registerBlock(plant.id(), p -> new IndicatorPlantBlock(plant, p), p -> properties(p, plant));
            PLANTS.put(plant, block);
            ITEMS.put(plant, ModItems.ITEMS.registerSimpleBlockItem(block));
        }
    }

    private static BlockBehaviour.Properties properties(BlockBehaviour.Properties p, IndicatorPlant plant) {
        MapColor colour = switch (plant) {
            case COPPER_FLOWER -> MapColor.COLOR_BLUE;
            case HORSETAIL -> MapColor.COLOR_LIGHT_GREEN;
            case STUNTED_BIRCH -> MapColor.COLOR_YELLOW;
            case LOCOWEED -> MapColor.COLOR_PURPLE;
            case PINK_THRIFT -> MapColor.COLOR_PINK;
        };
        return p.mapColor(colour).noCollision().instabreak().sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED);
    }

    public static void init() {}

    private FloraBlocks() {}
}
