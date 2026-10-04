package dev.strataindustria.power;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * A block entity that is part of a kinetic network (tier 3 spec 7): axles, gearboxes, sources and
 * machines. Networks are flood-filled over faces that both neighbours connect.
 */
public interface Kinetic {
    /** Whether rotation passes through {@code side} of this block. */
    boolean connects(Direction side);

    /** The network state this block was last given, synced to the client for rendering. */
    KineticState kinetic();

    /**
     * How much faster the side {@code to} turns than the side {@code from}, or than this block itself
     * when {@code from} is null. Plain transmission is 1:1; a step-up gearbox is 2 one way and 0.5 the other.
     */
    default float ratio(@Nullable Direction from, Direction to) {
        return 1.0f;
    }

    /**
     * Fastest this part may turn before it stops with "Overspeed": the wooden limit (tier 3 spec 7.1)
     * unless the part is iron (tier 4 spec 11.7).
     */
    default int speedLimit() {
        return dev.strataindustria.Config.KINETIC_WOODEN_SPEED_LIMIT.getAsInt();
    }

    /** Kinetic blocks this one is joined to other than through its faces, such as by a belt. */
    default List<BlockPos> links() {
        return List.of();
    }
}
