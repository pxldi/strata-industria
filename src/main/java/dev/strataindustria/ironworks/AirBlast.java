package dev.strataindustria.ironworks;

import net.minecraft.core.Direction;

/** Something that blows air through a face: a blower, later a blowing engine (tier 4 spec 11.6 and 10.5). */
public interface AirBlast {
    /** How many blowers' worth of air goes out through {@code out} right now. */
    int airOut(Direction out);
}
