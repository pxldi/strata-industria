package dev.strataindustria.transport.signal;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The block signal (outposts spec 9.4): a post with a semaphore arm, set on the right of the track and looking the
 * way the traffic it guards goes. The arm stands out red and white and the lamp burns while a vehicle is in the block
 * ahead; it drops when the block is clear. No lead passes a signal at stop: it waits at the line with "Waiting at signal".
 */
public class SignalBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** True when the arm is down: the block ahead is empty (or there is no track to watch). */
    public static final BooleanProperty CLEAR = BooleanProperty.create("clear");
    private static final int RECHECK = 4;
    private static final VoxelShape POST = Block.box(5, 0, 5, 11, 16, 11);

    public SignalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CLEAR, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CLEAR);
    }

    /** It looks the way the player looks if there is track on its left that way; otherwise whichever way finds some. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction look = context.getHorizontalDirection();
        Direction facing = look;
        for (Direction direction : new Direction[] {look, look.getClockWise(), look.getCounterClockWise(), look.getOpposite()}) {
            if (Signals.watchesTrack(context.getLevel(), context.getClickedPos(), direction)) {
                facing = direction;
                break;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide()) return;
        level.playSound(null, pos, SignalRegistry.SIGNAL_ARM.get(), SoundSource.BLOCKS, 0.7f, 0.8f);
        if (placer instanceof Player player && !Signals.watchesTrack(level, pos, state.getValue(FACING))) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".signal.no_track"));
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(this)) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        BlockPos rail = Signals.railOf(level, pos, facing);
        boolean clear = rail == null || !level.hasChunkAt(rail) || !Signals.blockOccupied(level, rail, facing);
        if (clear != state.getValue(CLEAR)) {
            level.setBlock(pos, state.setValue(CLEAR, clear), Block.UPDATE_ALL);
            arm(level, pos, facing, clear);
        }
        level.scheduleTick(pos, this, RECHECK);
    }

    /** The arm swings: a clunk, low as it drops and high as it comes up, and a puff of sparks off the pivot. */
    private static void arm(ServerLevel level, BlockPos pos, Direction facing, boolean clear) {
        level.playSound(null, pos, SignalRegistry.SIGNAL_ARM.get(), SoundSource.BLOCKS, clear ? 0.7f : 0.9f, clear ? 0.8f : 1.15f);
        Direction toward = facing.getCounterClockWise();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + toward.getStepX() * 0.3, pos.getY() + 0.8, pos.getZ() + 0.5 + toward.getStepZ() * 0.3,
                clear ? 4 : 8, 0.12, 0.1, 0.12, 0.04);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        String key;
        if (!Signals.watchesTrack(level, pos, state.getValue(FACING))) key = ".signal.no_track";
        else key = state.getValue(CLEAR) ? ".signal.clear" : ".signal.occupied";
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + key));
        level.playSound(null, pos, SignalRegistry.SIGNAL_ARM.get(), SoundSource.BLOCKS, 0.3f, 1.4f);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POST;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
