package dev.strataindustria.electric.machine;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The lathe block: an electric machine whose front shows the gear or rod icon of its current mode. */
public class LatheBlock extends ElectricMachineBlock<LatheBlockEntity> {
    public static final BooleanProperty GEAR = BooleanProperty.create("gear");

    public LatheBlock(Supplier<BlockEntityType<LatheBlockEntity>> type, BiFunction<BlockPos, BlockState, LatheBlockEntity> factory, Properties properties) {
        super(type, factory, properties);
        registerDefaultState(defaultBlockState().setValue(GEAR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GEAR);
    }
}
