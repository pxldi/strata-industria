package dev.strataindustria.transport.rail;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * What a lead tub has rolled over since it last stood at a stop (outposts spec 3.3): the stop it left, the track
 * blocks of this mod it crossed, and the first vanilla rail it touched, which keeps the trip from proving a line.
 */
public final class TrackTrace {
    private static final int MAX_BLOCKS = 20000;

    private @Nullable BlockPos origin;
    private @Nullable BlockPos vanilla;
    private final LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>();

    public @Nullable BlockPos origin() {
        return origin;
    }

    public @Nullable BlockPos vanilla() {
        return vanilla;
    }

    /** The tub stands at a stop: the next trip starts here. */
    public void restart(BlockPos stop) {
        origin = stop.immutable();
        vanilla = null;
        blocks.clear();
        blocks.add(origin);
    }

    /** The tub left the track or lost its way: nothing it rolled counts. */
    public void lose() {
        origin = null;
        vanilla = null;
        blocks.clear();
    }

    /** The tub rolls over a rail block; {@code ours} is true for the rails of this mod. */
    public void ride(BlockPos pos, boolean ours) {
        if (origin == null) return;
        if (ours) {
            if (blocks.size() < MAX_BLOCKS) blocks.add(pos.immutable());
        } else if (vanilla == null) {
            vanilla = pos.immutable();
        }
    }

    /** The blocks of the trip in the order they were first crossed. */
    public List<BlockPos> route() {
        return new ArrayList<>(blocks);
    }
}
