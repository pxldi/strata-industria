package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricTier;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The extruder block: a fluid-machine casing that only exists as MV (spec 10.9), so it places and starts as MV. */
public class ExtruderBlock extends ChemicalMachineBlock<ExtruderBlockEntity> {
    public ExtruderBlock(Supplier<BlockEntityType<ExtruderBlockEntity>> type, BiFunction<BlockPos, BlockState, ExtruderBlockEntity> factory,
            Properties properties) {
        super(type, factory, properties);
        registerDefaultState(defaultBlockState().setValue(ElectricMachineBlock.TIER, ElectricTier.MV));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(ElectricMachineBlock.TIER, ElectricTier.MV);
    }
}
