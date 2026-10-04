package dev.strataindustria.cord;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Sound events for twisting bark into cord and beating cord into cloth; their sounds and subtitles are in the sounds provider. */
public final class CordSounds {
    /** Fibres creak as the strips wind round each other; pitch climbs while you twist. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TWIST = register("cord.twist");
    /** The cord snaps tight and the loose ends spring free. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TIGHT = register("cord.tight");
    /** A dull thump of cord on stone. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BEAT = register("cord.beat");
    /** The cloth comes off the stone flat and pops free. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOTH = register("cord.cloth");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private CordSounds() {}
}
