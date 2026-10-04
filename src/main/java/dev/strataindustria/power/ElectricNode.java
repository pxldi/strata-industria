package dev.strataindustria.power;

import net.minecraft.core.Direction;

/**
 * A block entity that is part of an electric network (tier 5 spec 6.1): cables, generators, storage
 * and machines. Networks are flood-filled over faces that both neighbours connect.
 */
public interface ElectricNode {
    /** Whether power passes through {@code side} of this block. */
    boolean connectsElectric(Direction side);
}
