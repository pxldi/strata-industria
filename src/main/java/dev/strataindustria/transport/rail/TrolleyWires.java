package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jspecify.annotations.Nullable;

/**
 * The rules for trolley wire (outposts spec 9.2) and the way a tram finds the wire above it. A span runs bracket to
 * bracket, up to 16 blocks, one wire for every two blocks of length, in a straight line with nothing solid between.
 * Brackets are indexed by chunk on the server so a tram only looks at the few near it.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class TrolleyWires {
    public static final double MAX_SPAN = 16.0;
    /** Blocks of span one wire makes. */
    public static final double BLOCKS_PER_WIRE = 2.0;
    /** Spans one bracket holds: a line through and a branch off it. */
    public static final int MAX_SPANS = 3;
    /** How steep a span may run: up to a block of rise for every four blocks along, and a block either way. */
    private static final double SLOPE = 0.25;
    /** The wire has to hang this far above a tram's feet to be picked up, and no more. */
    public static final double REACH_MIN = 2.6, REACH_MAX = 6.6;
    /** How far sideways of the wire the trolley pole still reaches it; a curve under a straight span stays in. */
    private static final double CATCH = 1.4;

    private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> INDEX = new HashMap<>();

    private TrolleyWires() {}

    // ---------------------------------------------------------------- stringing

    public static double length(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    /** Wire a span between {@code a} and {@code b} costs, and drops back when it is cut. */
    public static int wireFor(BlockPos a, BlockPos b) {
        return (int) Math.ceil(length(a, b) / BLOCKS_PER_WIRE - 1.0e-9);
    }

    /** Why a span cannot be strung, as a translation key suffix, or null if it can. */
    public static @Nullable String problem(Level level, BlockPos a, BlockPos b) {
        if (a.equals(b)) return "same";
        if (!(level.getBlockEntity(a) instanceof TrolleyBracketBlockEntity first) || !(level.getBlockEntity(b) instanceof TrolleyBracketBlockEntity second)) {
            return "not_bracket";
        }
        if (first.spans().contains(b)) return "already";
        Vec3 from = TrolleyBracketBlockEntity.tip(a, level.getBlockState(a)), to = TrolleyBracketBlockEntity.tip(b, level.getBlockState(b));
        double along = Math.hypot(to.x - from.x, to.z - from.z);
        if (from.distanceTo(to) > MAX_SPAN) return "too_far";
        if (Math.abs(to.y - from.y) > 1.0 + SLOPE * along) return "steep";
        if (first.spanCount() >= MAX_SPANS || second.spanCount() >= MAX_SPANS) return "full";
        if (blocked(level, a, b)) return "blocked";
        return null;
    }

    /** Whether a solid block lies between the two clamps; the blocks the brackets stand in do not count. */
    public static boolean blocked(Level level, BlockPos a, BlockPos b) {
        Vec3 from = TrolleyBracketBlockEntity.tip(a, level.getBlockState(a)), to = TrolleyBracketBlockEntity.tip(b, level.getBlockState(b));
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, level, (l, pos) -> {
            if (pos.equals(a) || pos.equals(b)) return null;
            BlockState state = l.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(l, pos);
            return shape.isEmpty() ? null : shape.clip(from, to, pos);
        }, l -> null);
        return hit != null;
    }

    /** Lists the span at both ends, with a run of sparks along it; {@link #problem} must have returned null. */
    public static void connect(Level level, BlockPos a, BlockPos b) {
        if (!(level.getBlockEntity(a) instanceof TrolleyBracketBlockEntity first) || !(level.getBlockEntity(b) instanceof TrolleyBracketBlockEntity second)) return;
        first.addSpan(b);
        second.addSpan(a);
        if (level instanceof ServerLevel server) {
            Vec3 from = TrolleyBracketBlockEntity.tip(a, level.getBlockState(a)), to = TrolleyBracketBlockEntity.tip(b, level.getBlockState(b));
            int steps = Math.max(2, (int) (length(a, b) * 2));
            for (int i = 0; i <= steps; i++) {
                Vec3 p = from.lerp(to, i / (double) steps);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            server.playSound(null, b, TramRegistry.WIRE_STRUNG.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        }
    }

    // ---------------------------------------------------------------- the index

    static void track(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        INDEX.computeIfAbsent(server.dimension(), k -> new HashMap<>()).computeIfAbsent(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4), k -> new HashSet<>())
                .add(pos.immutable());
    }

    static void untrack(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        Map<Long, Set<BlockPos>> chunks = INDEX.get(server.dimension());
        if (chunks == null) return;
        long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        Set<BlockPos> set = chunks.get(key);
        if (set == null) return;
        set.remove(pos);
        if (set.isEmpty()) chunks.remove(key);
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) INDEX.remove(level.dimension());
    }

    // ---------------------------------------------------------------- finding the wire

    /** The wire a trolley pole reaches: the bracket it draws from, where the pole meets the wire, and how high that is. */
    public record Contact(BlockPos bracket, Vec3 point, double height, double sideways) {}

    /** The nearest wire over {@code pos} that a tram standing there can reach, or null. */
    public static @Nullable Contact find(ServerLevel level, Vec3 pos) {
        Map<Long, Set<BlockPos>> chunks = INDEX.get(level.dimension());
        if (chunks == null || chunks.isEmpty()) return null;
        int cx = Math.floorDiv((int) Math.floor(pos.x), 16), cz = Math.floorDiv((int) Math.floor(pos.z), 16);
        Contact best = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Set<BlockPos> brackets = chunks.get(ChunkPos.pack(cx + dx, cz + dz));
                if (brackets == null) continue;
                for (BlockPos a : brackets) {
                    if (!(level.getBlockEntity(a) instanceof TrolleyBracketBlockEntity bracket) || bracket.isRemoved()) continue;
                    for (BlockPos b : bracket.spans()) {
                        // Each span is looked at once, from its lower end.
                        if (b.asLong() < a.asLong() || !level.isLoaded(b)) continue;
                        Contact contact = along(level, pos, a, b);
                        if (contact != null && (best == null || contact.sideways() < best.sideways())) best = contact;
                    }
                }
            }
        }
        return best;
    }

    private static @Nullable Contact along(ServerLevel level, Vec3 pos, BlockPos a, BlockPos b) {
        BlockState stateA = level.getBlockState(a), stateB = level.getBlockState(b);
        if (!(stateA.getBlock() instanceof TrolleyBracketBlock) || !(stateB.getBlock() instanceof TrolleyBracketBlock)) return null;
        Vec3 from = TrolleyBracketBlockEntity.tip(a, stateA), to = TrolleyBracketBlockEntity.tip(b, stateB);
        double ax = to.x - from.x, az = to.z - from.z;
        double flat = ax * ax + az * az;
        double t = flat < 1.0e-6 ? 0 : Math.max(0, Math.min(1, ((pos.x - from.x) * ax + (pos.z - from.z) * az) / flat));
        double px = from.x + ax * t, pz = from.z + az * t;
        double sideways = Math.hypot(pos.x - px, pos.z - pz);
        if (sideways > CATCH) return null;
        double y = from.y + (to.y - from.y) * t;
        double height = y - pos.y;
        if (height < REACH_MIN || height > REACH_MAX) return null;
        // The end the pole is nearer to is where the tram draws from.
        BlockPos near = t < 0.5 ? a : b;
        return new Contact(near, new Vec3(px, y, pz), height, sideways);
    }
}
