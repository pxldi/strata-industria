package dev.strataindustria.steam;

import dev.strataindustria.registry.Tier4Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A steel boiler's fluid port (tier 4 spec 10.3): a shell block with a flange. Right-click switches it
 * between taking water in (blue tab) and letting steam out (white tab). A water port also takes buckets.
 */
public class BoilerFluidPortBlock extends SteelBoilerShellBlock {
    public enum Mode implements StringRepresentable {
        WATER, STEAM;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public BoilerFluidPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.WATER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            Mode next = state.getValue(MODE) == Mode.WATER ? Mode.STEAM : Mode.WATER;
            level.setBlock(pos, state.setValue(MODE, next), Block.UPDATE_ALL);
            level.playSound(null, pos, next == Mode.STEAM ? Tier4Sounds.VALVE_OPEN.get() : Tier4Sounds.VALVE_CLOSE.get(),
                    SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.WATER_BUCKET) || state.getValue(MODE) != Mode.WATER) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof BoilerPartBlockEntity part)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        SteelBoilerControllerBlockEntity boiler = part.host();
        if (boiler == null || boiler.fillWater(Fluids.WATER, 1000, true) < 1000) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            boiler.addWater(1000);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }
}
