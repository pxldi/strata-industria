package dev.strataindustria.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A short puzzle that keeps a {@link CrateBlockEntity} shut (structures v2 section 2a). Structures lock their
 * cache crate with one of two kinds; the crate can always be broken open with tools, so a puzzle never
 * takes loot away.
 *
 * <ul>
 *   <li><b>Lamps in order</b> ({@code ordered}): the steps are positions of {@code miners_lamp}s. Lighting
 *       them in the listed order opens the crate; lighting one out of turn snuffs them all again.</li>
 *   <li><b>Blocks in place</b>: each step names a block that has to stand at a position (loose rocks on a
 *       table, bricks set back into a stump). It opens when every step holds, checked whenever a block is
 *       placed nearby.</li>
 * </ul>
 */
public record PuzzleLock(List<Step> steps, boolean ordered, int progress) {
    /** How far from a changed block a locked crate still hears about it. */
    public static final int REACH = 32;

    public record Step(BlockPos pos, String block) {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Step::pos),
                Codec.STRING.fieldOf("block").forGetter(Step::block)).apply(i, Step::new));

        /** Whether the world currently shows this step done. Lamps count when burning steadily. */
        boolean holds(ServerLevel level) {
            BlockState state = level.getBlockState(pos);
            if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(block)) return false;
            return !(state.getBlock() instanceof MinersLampBlock) || state.getValue(MinersLampBlock.MODE) == MinersLampBlock.Mode.LIT;
        }
    }

    public static final Codec<PuzzleLock> CODEC = RecordCodecBuilder.create(i -> i.group(
            Step.CODEC.listOf().fieldOf("steps").forGetter(PuzzleLock::steps),
            Codec.BOOL.optionalFieldOf("ordered", false).forGetter(PuzzleLock::ordered),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(PuzzleLock::progress)).apply(i, PuzzleLock::new));

    /** The lamps of a lamp puzzle, to be lit in this order. */
    public static PuzzleLock lamps(List<BlockPos> order) {
        String lamp = BuiltInRegistries.BLOCK.getKey(SharedBlocks.MINERS_LAMP.get()).toString();
        return new PuzzleLock(order.stream().map(pos -> new Step(pos, lamp)).toList(), true, 0);
    }

    /** A puzzle that opens once each position holds its block. */
    public static PuzzleLock blocks(List<Step> steps) {
        return new PuzzleLock(steps, false, 0);
    }

    PuzzleLock withProgress(int next) {
        return new PuzzleLock(steps, ordered, next);
    }

    boolean solved(ServerLevel level) {
        return steps.stream().allMatch(step -> step.holds(level));
    }

    int indexOf(BlockPos pos) {
        for (int i = 0; i < steps.size(); i++) if (steps.get(i).pos().equals(pos)) return i;
        return -1;
    }

    // ---------------------------------------------------------------- events

    /** A player lit or snuffed the lamp at {@code pos}. */
    public static void lampChanged(ServerLevel level, BlockPos pos, boolean lit) {
        for (CrateBlockEntity crate : nearby(level, pos)) crate.lampChanged(level, pos, lit);
    }

    /** A block was placed at or near {@code pos}. */
    public static void blockPlaced(ServerLevel level, BlockPos pos) {
        for (CrateBlockEntity crate : nearby(level, pos)) crate.recheck(level);
    }

    private static List<CrateBlockEntity> nearby(ServerLevel level, BlockPos pos) {
        ChunkPos centre = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        int chunks = REACH / 16 + 1;
        java.util.ArrayList<CrateBlockEntity> found = new java.util.ArrayList<>();
        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) {
                var chunk = level.getChunkSource().getChunkNow(centre.x() + dx, centre.z() + dz);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof CrateBlockEntity crate && crate.isLocked() && crate.getBlockPos().distSqr(pos) <= REACH * REACH) {
                        found.add(crate);
                    }
                }
            }
        }
        return found;
    }
}
