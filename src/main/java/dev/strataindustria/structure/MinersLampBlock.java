package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * A copper and glass oil lamp, standing or hanging (structures v2 section 5). A click lights it or puts it
 * out. In old ruins some are "guttering": a low flame that dips and flares, with a thread of smoke; a click
 * trims the wick and they burn steadily. Lighting and snuffing report to {@link PuzzleLock} for the lamp puzzles.
 */
public class MinersLampBlock extends Block {
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    /** Only used while guttering: the flame has dipped, so the light is lower. */
    public static final BooleanProperty DIM = BooleanProperty.create("dim");

    private static final VoxelShape STANDING = Block.column(6.0, 0.0, 10.0);
    private static final VoxelShape HANGING_SHAPE = Block.column(6.0, 3.0, 16.0);

    public enum Mode implements StringRepresentable {
        OFF("off", 0), LIT("lit", 12), GUTTERING("guttering", 6);

        private final String id;
        private final int light;

        Mode(String id, int light) {
            this.id = id;
            this.light = light;
        }

        public int light() {
            return light;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public MinersLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HANGING, false).setValue(MODE, Mode.LIT).setValue(DIM, false));
    }

    /** Light of a state: a dipped guttering flame gives half of its steady light. */
    public static int light(BlockState state) {
        Mode mode = state.getValue(MODE);
        return mode == Mode.GUTTERING && state.getValue(DIM) ? mode.light() / 2 : mode.light();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING, MODE, DIM);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() == Direction.Axis.Y) {
                BlockState state = defaultBlockState().setValue(HANGING, direction == Direction.UP);
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
            }
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction support = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        return Block.canSupportCenter(level, pos.relative(support), support.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction support = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        if (direction == support && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Mode mode = state.getValue(MODE);
        // Off: light it. Burning: put it out. Guttering: trim the wick so it burns steadily.
        Mode next = mode == Mode.LIT ? Mode.OFF : Mode.LIT;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(MODE, next).setValue(DIM, false), 3);
            level.playSound(null, pos, next == Mode.LIT ? SharedBlocks.MINERS_LAMP_LIGHT.get() : SharedBlocks.MINERS_LAMP_SNUFF.get(),
                    SoundSource.BLOCKS, 0.6f, 0.95f + level.getRandom().nextFloat() * 0.1f);
            if (level instanceof ServerLevel serverLevel) PuzzleLock.lampChanged(serverLevel, pos, next == Mode.LIT);
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- guttering flame

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (state.getValue(MODE) == Mode.GUTTERING && !level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, 4 + level.getRandom().nextInt(20));
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(MODE) == Mode.GUTTERING;
    }

    /** Structures place lamps without {@code onPlace}; the first random tick after the chunk loads starts the flicker. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) != Mode.GUTTERING) return;
        // Mostly stay as it is, now and then dip or flare.
        if (random.nextInt(3) == 0) level.setBlock(pos, state.setValue(DIM, !state.getValue(DIM)), 2);
        level.scheduleTick(pos, this, 4 + random.nextInt(30));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Mode mode = state.getValue(MODE);
        if (mode == Mode.OFF) return;
        double y = pos.getY() + (state.getValue(HANGING) ? 0.45 : 0.6);
        if (mode == Mode.GUTTERING) {
            if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, y + 0.1, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
            if (random.nextInt(50) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SharedBlocks.MINERS_LAMP_FLUTTER.get(),
                        SoundSource.BLOCKS, 0.4f, 0.8f + random.nextFloat() * 0.3f, false);
            }
        } else if (random.nextInt(160) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SharedBlocks.MINERS_LAMP_FLUTTER.get(),
                    SoundSource.BLOCKS, 0.25f, 1.0f + random.nextFloat() * 0.2f, false);
        }
    }
}
