package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 5 (electric) sound events; their sounds and subtitles are in the sounds provider. */
public final class Tier5Sounds {
    /** A device or cable goes into overvoltage: a sharp crack and a falling buzz. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_OVERVOLTAGE = register("electric.overvoltage");
    /** Tiny crackles at a cable carrying more than it is rated for. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_SPARK = register("electric.spark");
    /** The dynamo's whirring armature with a faint electric hum. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KINETIC_DYNAMO_RUN = register("kinetic_dynamo.run");
    /** A faint high hum while a battery box charges. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BATTERY_BOX_CHARGE = register("battery_box.charge");

    /** A tap hammered into a log: a wooden knock with a metal tick. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_TAP_PLACE = register("block.tree_tap.place");
    /** A thick, soft drip into the tap's bowl. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_TAP_DRIP = register("block.tree_tap.drip");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private Tier5Sounds() {}
}
