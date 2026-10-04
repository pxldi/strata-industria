package dev.strataindustria.transport.rail;

import dev.strataindustria.Config;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hauls every consist on the track in front of it (outposts spec 5.4): up toward the winch or down away from it at
 * 0.15 blocks a tick at 16 RPM, twice that at 32. A pony says which way it wants to go; a consist with no mind of its
 * own is hauled unless the winch has been set to let down. Stopped, too slow or overstressed, it holds what is on
 * the line where it stands.
 */
public class InclineWinchBlockEntity extends KineticBlockEntity implements KineticConsumer {
    public static final int IMPACT = 4, MIN_SPEED = 8;
    /** Blocks a tick at 16 RPM. */
    public static final double SPEED_AT_16 = 0.15;
    /** Blocks a consist must be hauled before the goal counts. */
    private static final double GOAL_DISTANCE = 4.0;
    private static final int CREAK_TICKS = 24;

    private boolean lowering;
    private List<BlockPos> line = List.of();
    private int ropeTarget = -1;
    private double hauled;
    private int creak;

    public InclineWinchBlockEntity(BlockPos pos, BlockState state) {
        super(RailRegistry.INCLINE_WINCH_ENTITY.get(), pos, state);
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    public boolean powered() {
        return kinetic().rpm() >= MIN_SPEED;
    }

    private Direction facing() {
        return getBlockState().getValue(InclineWinchBlock.FACING);
    }

    /** Whether a consist with no mind of its own is let down rather than hauled. */
    public boolean lowering() {
        return lowering;
    }

    public boolean toggleMode() {
        lowering = !lowering;
        setChanged();
        return lowering;
    }

    /** The entity id of the vehicle the rope is tied to, or -1 for none. */
    public int ropeTarget() {
        return ropeTarget;
    }

    /** Track blocks the winch reaches, nearest the winch first. */
    public List<BlockPos> line() {
        return line;
    }

    // ---------------------------------------------------------------- the line

    /** The blocks of track leading away from the winch: the rail in front, then wherever each rail goes on. */
    public static List<BlockPos> trace(Level level, BlockPos winch, Direction facing, int reach) {
        List<BlockPos> blocks = new ArrayList<>();
        BlockPos previous = winch;
        BlockPos current = winch.relative(facing);
        if (!level.getBlockState(current).is(RailRegistry.TRACK)) {
            current = current.below();
            if (!level.getBlockState(current).is(RailRegistry.TRACK)) {
                current = winch.relative(facing).above();
                if (!level.getBlockState(current).is(RailRegistry.TRACK)) return blocks;
            }
        }
        for (int i = 0; i < reach && current != null; i++) {
            blocks.add(current);
            BlockPos next = onward(level, current, previous);
            previous = current;
            current = next;
        }
        return blocks;
    }

    /** The track block after {@code at} that does not lead back to {@code from}, if there is one. */
    private static @Nullable BlockPos onward(Level level, BlockPos at, BlockPos from) {
        BlockState state = level.getBlockState(at);
        if (!(state.getBlock() instanceof BaseRailBlock block)) return null;
        var exits = AbstractMinecart.exits(state.getValue(block.getShapeProperty()));
        for (Vec3i exit : new Vec3i[] {exits.getFirst(), exits.getSecond()}) {
            BlockPos target = at.offset(exit);
            for (BlockPos candidate : new BlockPos[] {target, target.below(), target.above()}) {
                if (candidate.equals(from) || !level.hasChunkAt(candidate)) continue;
                if (!level.getBlockState(candidate).is(RailRegistry.TRACK)) continue;
                if (sameColumn(candidate, from)) continue;
                return candidate;
            }
        }
        return null;
    }

    private static boolean sameColumn(BlockPos a, BlockPos b) {
        return a.getX() == b.getX() && a.getZ() == b.getZ();
    }

    // ---------------------------------------------------------------- hauling

    private static Vec3 centre(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, 0, pos.getZ() + 0.5);
    }

    private static Vec3 toward(Vec3 from, BlockPos to) {
        Vec3 delta = centre(to).subtract(from.x, 0, from.z);
        return delta.lengthSqr() < 1.0E-6 ? Vec3.ZERO : delta.normalize();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InclineWinchBlockEntity winch) {
        ServerLevel server = (ServerLevel) level;
        if (winch.line.isEmpty() || server.getGameTime() % 10 == 0) winch.line = trace(level, pos, winch.facing(), Config.TRANSPORT_WINCH_REACH.getAsInt());
        List<BlockPos> line = winch.line;
        int rope = -1;
        if (line.size() > 1) {
            Map<BlockPos, Integer> index = new HashMap<>();
            AABB bounds = new AABB(line.get(0));
            for (int i = 0; i < line.size(); i++) {
                index.put(line.get(i), i);
                bounds = bounds.minmax(new AABB(line.get(i)));
            }
            double speed = winch.powered() ? SPEED_AT_16 * winch.kinetic().rpm() / 16.0 : 0;
            boolean moved = false;
            for (MineTubEntity lead : server.getEntitiesOfClass(MineTubEntity.class, bounds.inflate(1.5), MineTubEntity::isLead)) {
                Integer at = index.get(lead.getCurrentBlockPosOrRailBelow());
                if (at == null || at == 0 || lead.holding()) continue;
                Vec3 up = toward(lead.position(), line.get(at - 1));
                Vec3 down = at + 1 < line.size() ? toward(lead.position(), line.get(at + 1)) : up.scale(-1);
                Vec3 want = lead.intent();
                boolean haul = want.lengthSqr() > 1.0E-6 ? want.dot(up) > 0 : !winch.lowering;
                if (speed <= 0) {
                    lead.winchControl(Vec3.ZERO, true);
                } else {
                    lead.winchControl((haul ? up : down).scale(speed), false);
                    moved = true;
                    winch.hauled += speed;
                    if (winch.hauled >= GOAL_DISTANCE && (lead.consist().size() > 1 || !lead.isEmpty())) {
                        Journal.awardNear(level, pos, Journal.WINCH_HAULED);
                    }
                }
                if (rope < 0) rope = lead.getId();
            }
            if (moved && --winch.creak <= 0) {
                winch.creak = CREAK_TICKS;
                server.playSound(null, pos, RailRegistry.WINCH_HAUL.get(), SoundSource.BLOCKS, 0.6f, 0.9f + 0.2f * (float) speed / (float) SPEED_AT_16 * 0.5f);
            }
            if (!moved) winch.creak = 0;
        }
        if (rope != winch.ropeTarget) {
            winch.ropeTarget = rope;
            winch.setChanged();
            server.sendBlockUpdated(pos, state, state, 3);
        }
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        lowering = in.getBooleanOr("lowering", false);
        ropeTarget = in.getIntOr("rope", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("lowering", lowering);
        out.putInt("rope", ropeTarget);
    }
}
