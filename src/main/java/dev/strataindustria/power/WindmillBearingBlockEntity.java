package dev.strataindustria.power;

import com.mojang.serialization.Codec;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A windmill (spec 7.2). Sails are counted in the plane in front of the bearing, joined to it through
 * other sails and within 7 blocks of the hub. 24 SU per sail, 8 to 32 sails; 8 RPM at y 64 or lower,
 * rising to 16 RPM at y 140, a quarter more in rain and half again in a thunderstorm. Anything solid
 * in the 15 x 15 swept area stops it.
 */
public class WindmillBearingBlockEntity extends KineticBlockEntity implements KineticSource {
    public static final int REACH = 7, MIN_SAILS = 8, MAX_SAILS = 32, SU_PER_SAIL = 24;
    public static final float LOW_RPM = 8.0f, HIGH_RPM = 16.0f;
    public static final int LOW_Y = 64, HIGH_Y = 140;
    private static final int CHECK_INTERVAL = 40;

    /** Sail positions relative to the bearing, synced so the renderer can turn them. */
    private List<BlockPos> sails = List.of();
    private boolean blocked;
    private float speed;

    public WindmillBearingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WINDMILL_BEARING.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(WindmillBearingBlock.FACING);
    }

    public List<BlockPos> sails() {
        return sails;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WindmillBearingBlockEntity bearing) {
        if ((level.getGameTime() + pos.asLong()) % CHECK_INTERVAL != 0) return;
        bearing.survey((ServerLevel) level);
        if (bearing.speed > 0 && level.getRandom().nextInt(3) == 0) {
            level.playSound(null, pos, ModSounds.WINDMILL_TURN.get(), SoundSource.BLOCKS, 0.6f, 0.8f + bearing.speed / 40.0f);
        }
    }

    /** Recounts the sails, checks the swept area and updates the speed. */
    public void survey(ServerLevel level) {
        Direction facing = facing();
        Direction.Axis axis = facing.getAxis();
        BlockPos centre = worldPosition.relative(facing);
        // Sails joined through faces, starting from the hub cell and its four neighbours in the plane.
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(centre);
        for (Direction side : Direction.values()) if (side.getAxis() != axis) queue.add(centre.relative(side));
        while (!queue.isEmpty() && found.size() < MAX_SAILS * 2) {
            BlockPos pos = queue.poll();
            if (found.contains(pos) || !inReach(centre, pos, axis) || !isSail(level.getBlockState(pos), axis)) continue;
            found.add(pos);
            for (Direction side : Direction.values()) if (side.getAxis() != axis) queue.add(pos.relative(side));
        }
        boolean nowBlocked = false;
        for (int a = -REACH; a <= REACH && !nowBlocked; a++) {
            for (int b = -REACH; b <= REACH; b++) {
                BlockPos cell = axis == Direction.Axis.X ? centre.offset(0, b, a) : centre.offset(a, b, 0);
                BlockState state = level.getBlockState(cell);
                if (!state.isAir() && !isSail(state, axis) && !(state.canBeReplaced() && state.getFluidState().isEmpty())) {
                    nowBlocked = true;
                    break;
                }
            }
        }

        List<BlockPos> relative = new ArrayList<>();
        for (BlockPos pos : found) relative.add(pos.subtract(worldPosition));
        relative.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        boolean changed = !relative.equals(sails) || nowBlocked != blocked;
        if (!relative.equals(sails)) attach(level, sails, relative);
        sails = List.copyOf(relative);
        blocked = nowBlocked;

        float nowSpeed = turningSpeed(level);
        if (nowSpeed != speed) {
            boolean started = speed == 0 && nowSpeed > 0;
            speed = nowSpeed;
            changed = true;
            if (started) Journal.awardNear(level, worldPosition, Journal.WATER_POWER);
        }
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            KineticNetworks.markDirty(level, worldPosition);
        }
    }

    private float turningSpeed(Level level) {
        if (blocked || sails.size() < MIN_SAILS) return 0.0f;
        float height = Mth.clamp((worldPosition.getY() - LOW_Y) / (float) (HIGH_Y - LOW_Y), 0.0f, 1.0f);
        float rpm = Mth.lerp(height, LOW_RPM, HIGH_RPM);
        if (level.isThundering()) rpm *= 1.5f;
        else if (level.isRaining()) rpm *= 1.25f;
        return rpm;
    }

    private static boolean inReach(BlockPos centre, BlockPos pos, Direction.Axis axis) {
        int a = axis == Direction.Axis.X ? pos.getZ() - centre.getZ() : pos.getX() - centre.getX();
        int b = pos.getY() - centre.getY();
        return Math.abs(a) <= REACH && Math.abs(b) <= REACH && pos.get(axis) == centre.get(axis);
    }

    private static boolean isSail(BlockState state, Direction.Axis axis) {
        return state.getBlock() instanceof WindmillSailBlock && state.getValue(WindmillSailBlock.AXIS) == axis;
    }

    /** Hides the sails this bearing now turns, and shows again the ones it no longer does. */
    private void attach(Level level, List<BlockPos> before, List<BlockPos> after) {
        Set<BlockPos> keep = new HashSet<>(after);
        for (BlockPos rel : before) if (!keep.contains(rel)) setAttached(level, worldPosition.offset(rel), false);
        for (BlockPos rel : after) setAttached(level, worldPosition.offset(rel), true);
    }

    private static void setAttached(Level level, BlockPos pos, boolean attached) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WindmillSailBlock && state.getValue(WindmillSailBlock.ATTACHED) != attached) {
            level.setBlock(pos, state.setValue(WindmillSailBlock.ATTACHED, attached), Block.UPDATE_CLIENTS);
        }
    }

    /** A broken bearing lets go of its sails. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) attach(level, sails, List.of());
    }

    @Override
    public float sourceSpeed() {
        return speed;
    }

    @Override
    public int capacity() {
        return Math.min(sails.size(), MAX_SAILS) * SU_PER_SAIL;
    }

    @Override
    public @Nullable Component idleReason() {
        if (blocked) return Component.translatable(StrataIndustria.MOD_ID + ".windmill.blocked");
        if (sails.size() < MIN_SAILS) return Component.translatable(StrataIndustria.MOD_ID + ".windmill.few_sails", MIN_SAILS, sails.size());
        return null;
    }

    private static final Codec<List<BlockPos>> SAILS_CODEC = BlockPos.CODEC.listOf();

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        sails = List.copyOf(in.read("sails", SAILS_CODEC).orElse(List.of()));
        blocked = in.getBooleanOr("blocked", false);
        speed = in.getFloatOr("speed", 0.0f);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("sails", SAILS_CODEC, sails);
        out.putBoolean("blocked", blocked);
        out.putFloat("speed", speed);
    }
}
