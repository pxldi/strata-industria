package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 6 sound events; their sounds and subtitles are in the sounds provider (spec 24.6). */
public final class Tier6Sounds {
    /** Thick crude glugging into a bucket. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUDE_OIL_BUCKET_FILL = register("item.bucket.fill_crude_oil");
    /** Crude oil slopping out of a bucket. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUDE_OIL_BUCKET_EMPTY = register("item.bucket.empty_crude_oil");
    /** A slow bubble breaking on a seep pool. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUDE_OIL_BUBBLE = register("block.crude_oil.bubble");

    /** Thick bubbling in the copper pot while a batch distils. */
    public static final DeferredHolder<SoundEvent, SoundEvent> OIL_STILL_BOIL = register("block.oil_still.boil");
    /** The gas that is not kept leaving the gooseneck. */
    public static final DeferredHolder<SoundEvent, SoundEvent> OIL_STILL_VENT = register("block.oil_still.vent");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private Tier6Sounds() {}
}
