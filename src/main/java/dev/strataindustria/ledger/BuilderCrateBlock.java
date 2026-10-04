package dev.strataindustria.ledger;

import dev.strataindustria.structure.NineSlotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A crate the builder's ledger takes its blocks from. Opens like a chest. */
public class BuilderCrateBlock extends NineSlotBlock {
    public BuilderCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BuilderCrateBlockEntity(pos, state);
    }
}
