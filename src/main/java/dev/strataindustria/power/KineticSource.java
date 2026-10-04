package dev.strataindustria.power;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Something that turns a network (spec 7.2). Tier 4 steam engines plug in here unchanged. */
public interface KineticSource extends Kinetic {
    /** RPM this source turns at right now, 0 while idle. */
    float sourceSpeed();

    /** Stress capacity in SU while turning. */
    int capacity();

    /** Why this source is not turning, shown when its network is checked; null when it turns. */
    default @Nullable Component idleReason() {
        return null;
    }
}
