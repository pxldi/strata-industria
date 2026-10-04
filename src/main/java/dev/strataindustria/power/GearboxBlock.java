package dev.strataindustria.power;

import dev.strataindustria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A wooden gearbox (spec 7.3): all six faces at 1:1, for corners and splits. */
public class GearboxBlock extends BaseEntityBlock implements KineticBlock {
    public GearboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean connects(BlockState state, Direction side) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Kinetics.report(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KineticBlockEntity(ModBlockEntities.KINETIC_TRANSMISSION.get(), pos, state);
    }
}
