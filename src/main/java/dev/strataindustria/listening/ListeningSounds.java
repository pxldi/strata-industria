package dev.strataindustria.listening;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Sound events of the listening kit (uniqueness 2.1): what a machine or a rock tells you by ear. */
public final class ListeningSounds {
    /** A hammer tap that rings clean: rich ore, a sound casting. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TAP_RING = register("tap.ring");
    /** A flat knock: ordinary ore, a plain casting. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TAP_KNOCK = register("tap.knock");
    /** A dull thud: poor ore, a casting with a flaw in it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TAP_THUD = register("tap.thud");
    /** A belt near the end of what it can carry. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BELT_SQUEAL = register("belt.squeal");
    /** A gear train ticking over under load. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GEAR_TICK = register("gear.tick");
    /** A water wheel straining under too much work. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_WHEEL_GROAN = register("water_wheel.groan");
    /** An engine starved of steam knocking in its cylinder. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_KNOCK = register("steam_engine.knock");
    /** A boiler hissing harder as it nears its safety valve. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_HISS = register("boiler.hiss");
    /** The steam whistle blowing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_WHISTLE = register("steam_whistle.blow");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private ListeningSounds() {}
}
