package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricNetworks;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/** A 6 px post of treated wood (spec 8.3). Poles stack into one pole; insulators on it are the connection points. */
public class UtilityPoleBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public UtilityPoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ElectricNetworks.report(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UtilityPoleBlockEntity(pos, state);
    }
}
