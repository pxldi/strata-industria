package dev.strataindustria.oil;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fire.Ignitable;
import dev.strataindustria.prospecting.ScannerPayloads;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.registry.Tier6DataComponents;
import dev.strataindustria.registry.Tier6Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The seismic charge (tier 6 spec 5.2): a survey tool, not an explosive. Light it with a firestarter, flint and
 * steel or a fire charge, or give it a redstone signal; after a 40 tick fuse it thumps once. It breaks nothing and
 * hurts nobody, but every ore scanner held within listening range records the reservoirs under the surrounding
 * chunks. It only works on solid ground.
 */
public class SeismicChargeBlock extends Block implements Ignitable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int FUSE_TICKS = 40;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 2, 12), Block.box(5, 2, 5, 11, 11, 11), Block.box(7.5, 11, 7.5, 8.5, 14, 8.5));

    public SeismicChargeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Stands on anything that is there; whether the ground is solid enough is found out when it is lit. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return !below.isAir() && !below.canBeReplaced() && below.getFluidState().isEmpty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** Spec 5.2: the thump does not travel through air or a part block. */
    public static boolean solidGround(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    // ---------------------------------------------------------------- lighting

    @Override
    public boolean canIgnite(Level level, BlockPos pos, BlockState state) {
        return !state.getValue(LIT);
    }

    @Override
    public boolean ignite(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(LIT)) return false;
        if (!solidGround(level, pos)) {
            fizzle((ServerLevel) level, pos);
            return false;
        }
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, FUSE_TICKS);
        level.playSound(null, pos, Tier6Sounds.SEISMIC_FUSE.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        return true;
    }

    private static void fizzle(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.01);
        level.playSound(null, pos, Tier6Sounds.SEISMIC_FUSE.get(), SoundSource.BLOCKS, 0.4f, 0.6f);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".seismic_charge.fizzle"));
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        boolean flint = stack.is(Items.FLINT_AND_STEEL), charge = stack.is(Items.FIRE_CHARGE);
        if (!flint && !charge) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(LIT)) return InteractionResult.PASS;
        if (!level.isClientSide() && ignite(level, pos, state)) {
            if (flint) stack.hurtAndBreak(1, player, hand);
            else stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.isClientSide() || state.getValue(LIT) || !level.hasNeighborSignal(pos)) return;
        ignite(level, pos, state);
    }

    // ---------------------------------------------------------------- the thump

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        BlockState below = level.getBlockState(pos.below());
        level.removeBlock(pos, false);
        if (!solidGround(level, pos)) {
            fizzle(level, pos);
            return;
        }
        detonate(level, pos, below);
    }

    /** The survey part of the charge, also used by game tests: records the survey on every ore scanner within listening range. */
    public static int detonate(ServerLevel level, BlockPos pos, BlockState ground) {
        level.playSound(null, pos, Tier6Sounds.SEISMIC_THUMP.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
        // A ring of the ground's own dust and a low plume, no explosion.
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, ground.isAir() ? Blocks.DIRT.defaultBlockState() : ground);
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8.0;
            level.sendParticles(dust, pos.getX() + 0.5 + Math.cos(angle) * 0.9, pos.getY() + 0.1, pos.getZ() + 0.5 + Math.sin(angle) * 0.9, 2,
                    0.05, 0.02, 0.05, 0.03);
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 4, 0.5, 0.05, 0.5, 0.005);

        double range = Config.SEISMIC_LISTEN_RANGE.getAsInt();
        SeismicSurvey survey = null;
        int recorded = 0;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > range * range) continue;
            ItemStack scanner = heldScanner(player);
            if (scanner.isEmpty()) continue;
            if (survey == null) survey = SeismicSurvey.take(level, pos);
            scanner.set(Tier6DataComponents.SEISMIC_RESULT.get(), survey);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), Tier6Sounds.ORE_SCANNER_ECHO.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            if (player.connection.hasChannel(ScannerPayloads.Open.TYPE)) PacketDistributor.sendToPlayer(player, new ScannerPayloads.Open(true));
            recorded++;
        }
        return recorded;
    }

    private static ItemStack heldScanner(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Tier5Items.ORE_SCANNER.get())) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0.02, 0);
        level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0, 0);
    }
}
