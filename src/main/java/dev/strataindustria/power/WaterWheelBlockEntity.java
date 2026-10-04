package dev.strataindustria.power;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Each rim cell with flowing (not source) water counts +1 if the flow pushes the wheel one way and
 * -1 the other; the wheel turns at 8 RPM per net cell, up to 3 (spec 7.2).
 */
public class WaterWheelBlockEntity extends KineticBlockEntity implements KineticSource {
    public static final int CAPACITY = 256;
    public static final float RPM_PER_CELL = 8.0f;
    public static final int MAX_CELLS = 3;
    private static final int CHECK_INTERVAL = 20;

    private int cells;
    private boolean blocked;

    public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_WHEEL.get(), pos, state);
    }

    /** The 8 rim cells around the hub, in the plane across the axis. */
    public static BlockPos[] rim(BlockPos hub, Direction.Axis axis) {
        BlockPos[] cells = new BlockPos[8];
        int i = 0;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                cells[i++] = axis == Direction.Axis.X ? hub.offset(0, b, a) : hub.offset(a, b, 0);
            }
        }
        return cells;
    }

    /** Whether every rim cell is air or water, so the wheel can stand and turn there. */
    public static boolean rimClear(Level level, BlockPos hub, Direction.Axis axis) {
        for (BlockPos cell : rim(hub, axis)) {
            BlockState state = level.getBlockState(cell);
            if (!state.isAir() && !(state.getFluidState().is(FluidTags.WATER) && state.canBeReplaced())) return false;
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterWheelBlockEntity wheel) {
        if ((level.getGameTime() + pos.asLong()) % CHECK_INTERVAL != 0) return;
        Direction.Axis axis = state.getValue(WaterWheelBlock.AXIS);
        boolean blocked = !rimClear(level, pos, axis);
        int cells = blocked ? 0 : Math.min(MAX_CELLS, Math.abs(netFlow(level, pos, axis)));
        if (cells != wheel.cells || blocked != wheel.blocked) {
            boolean started = wheel.cells == 0 && cells > 0;
            wheel.cells = cells;
            wheel.blocked = blocked;
            KineticNetworks.markDirty(level, pos);
            if (started) Journal.awardNear(level, pos, Journal.WATER_POWER);
        }
        if (cells > 0 && level.getRandom().nextInt(3) == 0) {
            level.playSound(null, pos, ModSounds.WATER_WHEEL_TURN.get(), SoundSource.BLOCKS, 0.5f, 0.8f + cells * 0.1f);
        }
    }

    /** Sum of +1 / -1 per rim cell whose flowing water pushes along the rim one way or the other. */
    static int netFlow(Level level, BlockPos hub, Direction.Axis axis) {
        Vec3 axisVec = axis == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        int net = 0;
        for (BlockPos cell : rim(hub, axis)) {
            FluidState fluid = level.getFluidState(cell);
            if (!fluid.is(FluidTags.WATER) || fluid.isSource()) continue;
            Vec3 flow = fluid.getFlow(level, cell);
            Vec3 radius = new Vec3(cell.getX() - hub.getX(), cell.getY() - hub.getY(), cell.getZ() - hub.getZ());
            double push = flow.dot(axisVec.cross(radius));
            if (push > 1.0e-3) net++;
            else if (push < -1.0e-3) net--;
        }
        return net;
    }

    @Override
    public float sourceSpeed() {
        return cells * RPM_PER_CELL;
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }

    @Override
    public @Nullable Component idleReason() {
        if (blocked) return Component.translatable(StrataIndustria.MOD_ID + ".water_wheel.blocked");
        if (cells == 0) return Component.translatable(StrataIndustria.MOD_ID + ".water_wheel.no_flow");
        return null;
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        cells = in.getIntOr("cells", 0);
        blocked = in.getBooleanOr("blocked", false);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("cells", cells);
        out.putBoolean("blocked", blocked);
    }
}
