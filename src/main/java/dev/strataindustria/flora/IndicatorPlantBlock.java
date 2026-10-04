package dev.strataindustria.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small wild plant. It grows on soil, and horsetail also on sand and gravel along rivers. */
public class IndicatorPlantBlock extends VegetationBlock {
    private final IndicatorPlant plant;
    private final VoxelShape shape;

    public IndicatorPlantBlock(IndicatorPlant plant, Properties properties) {
        super(properties);
        this.plant = plant;
        double height = switch (plant) {
            case HORSETAIL -> 14;
            case STUNTED_BIRCH -> 11;
            case COPPER_FLOWER -> 12;
            case LOCOWEED -> 13;
            case PINK_THRIFT -> 11;
        };
        this.shape = Shapes.box(0.2, 0, 0.2, 0.8, height / 16.0, 0.8);
    }

    public IndicatorPlant plant() {
        return plant;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState ground, BlockGetter level, BlockPos pos) {
        if (ground.is(BlockTags.SUPPORTS_VEGETATION)) return true;
        return plant == IndicatorPlant.HORSETAIL && (ground.is(BlockTags.SAND) || ground.is(net.minecraft.world.level.block.Blocks.GRAVEL));
    }
}
