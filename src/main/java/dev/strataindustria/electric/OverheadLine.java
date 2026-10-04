package dev.strataindustria.electric;

import dev.strataindustria.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The rules for stringing an overhead span between two pole insulators (tier 5 spec 8.4). */
public final class OverheadLine {
    /** Conductors one span uses per this many blocks of length. */
    public static final double BLOCKS_PER_CONDUCTOR = 4.0;

    private OverheadLine() {}

    public static double length(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    /** Conductors a span between {@code a} and {@code b} costs, and drops back when it is cut. */
    public static int conductorsFor(BlockPos a, BlockPos b) {
        return (int) Math.ceil(length(a, b) / BLOCKS_PER_CONDUCTOR - 1.0e-9);
    }

    /**
     * Why a span cannot be strung, as a translation key suffix, or null if it can. The line of sight is only
     * checked now: blocks placed later do not cut a span.
     */
    public static @Nullable String problem(Level level, BlockPos a, BlockPos b) {
        if (a.equals(b)) return "same";
        if (!(level.getBlockEntity(a) instanceof PoleInsulatorBlockEntity first) || !(level.getBlockEntity(b) instanceof PoleInsulatorBlockEntity second)) {
            return "not_insulator";
        }
        if (first.spans().contains(b)) return "already";
        if (length(a, b) > Config.ELECTRIC_MAX_SPAN.get()) return "too_far";
        if (first.spanCount() >= PoleInsulatorBlockEntity.MAX_SPANS || second.spanCount() >= PoleInsulatorBlockEntity.MAX_SPANS) return "full";
        if (blocked(level, a, b)) return "blocked";
        return null;
    }

    /** Whether a solid block lies between the two wire clamps; the blocks the insulators stand in do not count. */
    public static boolean blocked(Level level, BlockPos a, BlockPos b) {
        Vec3 from = PoleInsulatorBlockEntity.tip(a, level.getBlockState(a));
        Vec3 to = PoleInsulatorBlockEntity.tip(b, level.getBlockState(b));
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, level, (l, pos) -> {
            if (pos.equals(a) || pos.equals(b)) return null;
            BlockState state = l.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(l, pos);
            return shape.isEmpty() ? null : shape.clip(from, to, pos);
        }, l -> null);
        return hit != null;
    }

    /** Lists the span at both ends; {@link #problem} must have returned null. */
    public static void connect(Level level, BlockPos a, BlockPos b) {
        if (!(level.getBlockEntity(a) instanceof PoleInsulatorBlockEntity first) || !(level.getBlockEntity(b) instanceof PoleInsulatorBlockEntity second)) return;
        first.addSpan(b);
        second.addSpan(a);
        if (level instanceof ServerLevel server) {
            Vec3 from = PoleInsulatorBlockEntity.tip(a, level.getBlockState(a)), to = PoleInsulatorBlockEntity.tip(b, level.getBlockState(b));
            int steps = Math.max(2, (int) (length(a, b) * 1.5));
            for (int i = 0; i <= steps; i++) {
                Vec3 p = from.lerp(to, i / (double) steps);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }
}
