package dev.strataindustria.branch;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Sound events for snapping branches off trees; their sounds and subtitles are in the sounds provider. */
public final class BranchSounds {
    /** The branch bends and creaks; pitch climbs with each shake. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAKE = register("branch.shake");
    /** The branch gives way with a crack. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SNAP = register("branch.snap");
    /** A branch that is already bare: dry leaves, nothing to take. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BARE = register("branch.bare");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private BranchSounds() {}
}
