package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, StrataIndustria.MOD_ID);

    /** One strike while knapping a rock. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KNAP_ROCK = register("knapping.rock");
    /** One strike while knapping flint: sharper and higher than rock. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KNAP_FLINT = register("knapping.flint");
    /** The finished head comes free of the stone. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KNAP_FINISH = register("knapping.finish");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    private ModSounds() {}
}
