package dev.strataindustria.ceramics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A fired clay crucible (spec 4.2). Melting and pouring come with copper smelting (spec 7). */
public class CrucibleBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.join(Block.box(2, 0, 2, 14, 12, 14), Block.box(4, 2, 4, 12, 12, 12), BooleanOp.ONLY_FIRST);

    public CrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
