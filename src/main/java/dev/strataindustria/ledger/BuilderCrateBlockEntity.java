package dev.strataindustria.ledger;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.structure.NineSlotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;

/** The builder's crate: twenty-seven slots of whatever the ledger is to build with. */
public class BuilderCrateBlockEntity extends NineSlotBlockEntity {
    public BuilderCrateBlockEntity(BlockPos pos, BlockState state) {
        super(LedgerRegistry.CRATE_ENTITY.get(), pos, state);
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected Component name() {
        return Component.translatable("block." + StrataIndustria.MOD_ID + ".builders_crate");
    }

    @Override
    protected SoundEvent openSound() {
        return dev.strataindustria.structure.SharedBlocks.CRATE_OPEN.get();
    }

    @Override
    protected SoundEvent closeSound() {
        return dev.strataindustria.structure.SharedBlocks.CRATE_CLOSE.get();
    }
}
