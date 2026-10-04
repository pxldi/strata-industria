package dev.strataindustria.grid;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Sound events of the electric listening kit and the lightning bank (uniqueness 2.1, 7.1, 7.3). */
public final class GridSounds {
    /** The mains hum of a loaded line; pitch and volume follow the load. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MAINS_HUM = register("grid.hum");
    /** A line or transformer at its limit: the hum turns to a rough buzz. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MAINS_BUZZ = register("grid.buzz");
    /** The stethoscope on a machine that is working as it should. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LISTEN_STEADY = register("stethoscope.steady");
    /** The stethoscope on a machine that is short of power or straining. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LISTEN_STRAINED = register("stethoscope.strained");
    /** The stethoscope on something that is dead or has no power. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LISTEN_SILENT = register("stethoscope.silent");
    /** A bolt coming down the mast into the jars. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LEYDEN_STRIKE = register("leyden_jar.strike");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private GridSounds() {}
}
