package dev.strataindustria.power;

import net.minecraft.core.Direction;

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
     * How much faster the side {@code to} turns than the side {@code from}. Plain transmission is
     * 1:1; a step-up gearbox is 2 one way and 0.5 the other.
     */
    default float ratio(Direction from, Direction to) {
        return 1.0f;
    }
}
