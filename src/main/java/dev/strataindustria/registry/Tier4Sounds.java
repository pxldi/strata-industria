package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 4 sound events; their sounds and subtitles are in the sounds provider. */
public final class Tier4Sounds {
    /** The low roar and tick of a coke oven baking its charge. */
    public static final DeferredHolder<SoundEvent, SoundEvent> COKE_OVEN_WORKING = register("coke_oven.working");
    /** A charge comes out baked. */
    public static final DeferredHolder<SoundEvent, SoundEvent> COKE_OVEN_DONE = register("coke_oven.done");
    /** Thick oil glugging into a bucket. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CREOSOTE_FILL = register("creosote.fill");
    /** Spare carbon flares off a melt with no iron to hold it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CARBON_BURN = register("crucible.carbon_burn");

    /** Hiss and sulfurous fizz of ore roasting in a forge slot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROASTING_SIZZLE = register("roasting.sizzle");
    /** A roasted piece settles into chalky calcine. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROASTING_DONE = register("roasting.done");
    /** Calcine gives up its zinc to the carbon in a melt. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CALCINE_REDUCE = register("crucible.calcine_reduce");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private Tier4Sounds() {}
}
