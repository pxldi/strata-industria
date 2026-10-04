package dev.strataindustria.signs;

import dev.strataindustria.registry.ModSounds;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * A stain on the ground (redesign R7): a thin overlay in the air block above the normal ground, so grass,
 * dirt and rock keep their own look under it. It has no collision, drops nothing and gives way to anything
 * placed on it. Rubbing it with a bare hand scuffs up a puff of its colour and a gritty note that climbs with
 * every rub; the stain stays, because it is the sign.
 */
public class StainBlock extends Block {
    /** Which of the four patch drawings and, through the blockstate, which turn of it. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
    /** Rubs closer together than this count as one run, and the pitch keeps climbing. */
    private static final long RUN_TICKS = 30;
    /** Rubs closer together than this are a held button repeating. */
    private static final long GAP_TICKS = 5;
    private static final int RUN_STEPS = 6;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    private static final Map<UUID, long[]> RUNS = new ConcurrentHashMap<>();

    private final Stain stain;

    public StainBlock(Stain stain, Properties properties) {
        super(properties);
        this.stain = stain;
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    public Stain stain() {
        return stain;
    }

    /** A stain as worldgen places it; {@code pick} is any number that varies from block to block. */
    public BlockState with(int pick) {
        return defaultBlockState().setValue(VARIANT, Math.floorMod(pick, 4));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            rub(server, serverPlayer, pos, stain, hit.getLocation(), server.getGameTime());
        }
        return InteractionResult.SUCCESS;
    }

    /** One rub. {@code now} is explicit so tests can space their rubs. Returns the step of the run, 0 if ignored. */
    public static int rub(ServerLevel level, ServerPlayer player, BlockPos pos, Stain stain, Vec3 at, long now) {
        long[] run = RUNS.computeIfAbsent(player.getUUID(), id -> new long[] {-1000, 0});
        if (now - run[0] < GAP_TICKS) return 0;
        run[1] = now - run[0] <= RUN_TICKS ? Math.min(run[1] + 1, RUN_STEPS) : 1;
        run[0] = now;
        int step = (int) run[1];
        float pitch = Mth.clamp(0.8f + 0.1f * (step - 1) + level.getRandom().nextFloat() * 0.04f, 0.5f, 2.0f);
        level.playSound(null, at.x, at.y, at.z, ModSounds.STAIN_RUB.get(), SoundSource.BLOCKS, 0.7f, pitch);
        if (step == RUN_STEPS) {
            level.playSound(null, at.x, at.y, at.z, ModSounds.SHAPING_CHIME.get(), SoundSource.BLOCKS, 0.3f, 1.4f);
        }
        DustParticleOptions dust = new DustParticleOptions(stain.color(), 0.8f + 0.1f * step);
        level.sendParticles(dust, at.x, at.y + 0.05, at.z, 4 + 2 * step, 0.18, 0.04, 0.18, 0.02);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, at.x, at.y + 0.05, at.z, 1, 0.1, 0.02, 0.1, 0.01);
        if (!(player instanceof FakePlayer)) dev.strataindustria.journal.SignNotes.rubbed(player, stain);
        return step;
    }

    public static void forget(UUID player) {
        RUNS.remove(player);
    }
}
