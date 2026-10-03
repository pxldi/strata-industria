package dev.strataindustria.block;

import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** An ore mineral in a host rock, with a {@code grade} blockstate that decides the drop. */
public class OreBlock extends Block {
    private final Rock rock;
    private final OreMineral mineral;

    public OreBlock(Rock rock, OreMineral mineral, Properties properties) {
        super(properties);
        this.rock = rock;
        this.mineral = mineral;
        registerDefaultState(stateDefinition.any().setValue(OreGrade.PROPERTY, OreGrade.NORMAL));
    }

    public Rock rock() {
        return rock;
    }

    public OreMineral mineral() {
        return mineral;
    }

    public BlockState withGrade(OreGrade grade) {
        return defaultBlockState().setValue(OreGrade.PROPERTY, grade);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OreGrade.PROPERTY);
    }
}
