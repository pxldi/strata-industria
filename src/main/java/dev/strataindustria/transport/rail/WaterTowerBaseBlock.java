package dev.strataindustria.transport.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The foot of a water tower (outposts spec 7.4): a timber trestle that takes water from pipes on any side and
 * passes it up into the fluid tank standing on it. Tanks above it, up to the spout, are the tower's reservoir.
 */
public class WaterTowerBaseBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 3, 14, 3), Block.box(13, 0, 0, 16, 14, 3),
            Block.box(0, 0, 13, 3, 14, 16), Block.box(13, 0, 13, 16, 14, 16),
            Block.box(0, 14, 0, 16, 16, 16), Block.box(3, 5, 3, 13, 7, 13));

    public WaterTowerBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** A hand on the trestle reads the tower: how much water stands in the tanks above it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof WaterTowerBaseBlockEntity base) player.sendOverlayMessage(base.readout());
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterTowerBaseBlockEntity(pos, state);
    }

    static Component none() {
        return Component.translatable(dev.strataindustria.StrataIndustria.MOD_ID + ".water_tower.no_tank");
    }
}
