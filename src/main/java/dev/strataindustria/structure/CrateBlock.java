package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import dev.strataindustria.StrataIndustria;

/**
 * Spruce slats with rope handles, nine slots (structures v2 section 5). A crate that a structure has locked
 * with a {@link PuzzleLock} is nailed shut until the puzzle is solved; breaking it always works.
 */
public class CrateBlock extends NineSlotBlock {
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    public CrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LOCKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOCKED);
    }

    @Override
    protected boolean refuses(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(LOCKED)) return false;
        if (!level.isClientSide()) {
            level.playSound(null, pos, SharedBlocks.CRATE_LOCKED.get(), SoundSource.BLOCKS, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            player.sendOverlayMessage(Component.translatable("block." + StrataIndustria.MOD_ID + ".crate.locked"));
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }
}
