package dev.strataindustria.heat;

import net.minecraft.core.Direction;

/** A block entity that heat pipes join on some faces: a firebox, a heat inlet, a boiler (tier 4 spec 8.2). */
public interface HeatPort {
    boolean connectsHeat(Direction side);
}
