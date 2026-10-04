package dev.strataindustria.charcoal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * What a log pile becomes after the burn (spec 4.4). Dig it with a shovel for the charcoal and ash it
 * holds; its loot table reads the counts from the state.
 */
public class CharcoalPileBlock extends Block {
    public static final int MAX_CHARCOAL = 16;
    public static final int MAX_ASH = 2;
    public static final IntegerProperty CHARCOAL = IntegerProperty.create("charcoal", 1, MAX_CHARCOAL);
    public static final IntegerProperty ASH = IntegerProperty.create("ash", 0, MAX_ASH);

    public CharcoalPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARCOAL, 8).setValue(ASH, 2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARCOAL, ASH);
    }
}
