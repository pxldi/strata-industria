package dev.strataindustria.felling;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Sound events of felling a tree (redesign R5); their sounds and subtitles are in the sounds provider. */
public final class FellingSounds {
    /** The axe biting into the trunk; the code deepens the pitch with each notch. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NOTCH = register("felling.notch");
    /** The trunk groaning as it starts to give. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CREAK = register("felling.creak");
    /** The tree hitting the ground. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRASH = register("felling.crash");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private FellingSounds() {}
}
