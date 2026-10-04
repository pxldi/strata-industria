package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Outposts and transport sound events; their sounds and subtitles are in the sounds provider. */
public final class TransportSounds {
    /** Hammer on wood and a sheet of paper pinned on: a charter is posted. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARTER_PLACE = register("charter.place");
    /** Told to the owner when a line is cut: a distant snap. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARTER_LINE_CUT = register("charter.line_cut");
    /** A pencil and a page: the charter takes a new name. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARTER_DEED = register("charter.deed");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private TransportSounds() {}
}
