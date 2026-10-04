package dev.strataindustria.transport.rail;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What a piece of this mod's track is made of (outposts spec 5.1 and 7.1): wooden track holds a vehicle to 0.2 b/t
 * and drags at it, steel track runs to 0.5 b/t and lets it roll like a vanilla rail. The tipple is built for both and
 * keeps to the slow pace, being a place to stand. A line is a railway only when no wooden piece is in it.
 */
public enum RailGrade {
    WOOD(0.2, false),
    STEEL(0.5, true),
    NEUTRAL(0.2, true);

    private final double cap;
    private final boolean steelLine;

    RailGrade(double cap, boolean steelLine) {
        this.cap = cap;
        this.steelLine = steelLine;
    }

    /** Top speed in blocks a tick. */
    public double cap() {
        return cap;
    }

    /** Whether a route made of this still counts as steel track. */
    public boolean steelLine() {
        return steelLine;
    }

    /** The grade of the track block, or null for anything that is not this mod's. */
    public static @Nullable RailGrade of(BlockState state) {
        if (!state.is(RailRegistry.TRACK)) return null;
        if (state.is(RailRegistry.TIPPLE_RAIL.get())) return NEUTRAL;
        return state.is(RailwayRegistry.STEEL_TRACK.get()) || state.is(RailwayRegistry.STATION_TRACK.get())
                || state.is(RailwayRegistry.STEEL_BUFFER.get()) ? STEEL : WOOD;
    }

    /** True when every block of {@code route} that is loaded is steel track, so the line may count as a railway. */
    public static boolean steelRoute(ServerLevel level, List<BlockPos> route) {
        for (BlockPos pos : route) {
            if (!level.hasChunkAt(pos)) continue;
            RailGrade grade = of(level.getBlockState(pos));
            if (grade == null || !grade.steelLine()) return false;
        }
        return true;
    }
}
