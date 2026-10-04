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

    /** The seismic charge's fuse fizzing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SEISMIC_FUSE = register("block.seismic_charge.fuse");
    /** The deep, muffled thump of a charge going off underground. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SEISMIC_THUMP = register("block.seismic_charge.thump");
    /** Three falling pings when an ore scanner records a survey. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ORE_SCANNER_ECHO = register("item.ore_scanner.echo");
    /** The drill string grinding down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WELLHEAD_DRILL = register("block.wellhead.drill");
    /** A length of casing clanking down the bore. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WELLHEAD_CASING = register("block.wellhead.casing");
    /** Striking oil. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WELLHEAD_GUSHER = register("block.wellhead.gusher");
    /** Crude oil gurgling up a drilled well under its own pressure. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WELLHEAD_FLOW = register("block.wellhead.flow");
    /** One stroke of the pump jack. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PUMP_JACK_STROKE = register("block.pump_jack.stroke");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private Tier6Sounds() {}
}
