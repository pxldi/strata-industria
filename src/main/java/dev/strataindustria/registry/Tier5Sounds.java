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

    /** Spec 23.6: a relay clack and a rising whine when a machine gets power. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_POWER_ON = register("block.machine.power_on");
    /** A relay clack and a falling whine when a machine loses power. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_POWER_OFF = register("block.machine.power_off");
    /** Two soft descending beeps as a machine runs short of power. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_LOW_POWER = register("block.machine.low_power");
    /** The electric furnace's low even hum with a soft crackle. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_RUN = register("block.electric_furnace.run");
    /** The macerator's metallic grinding with grit. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MACERATOR_GRIND = register("block.macerator.grind");

    /** Spec 23.6: a rising jet whine over a steam rush; pitch and volume follow the spin. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_TURBINE_RUN = register("block.steam_turbine.run");
    /** A long falling whine when the steam is cut. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_TURBINE_SPIN_DOWN = register("block.steam_turbine.spin_down");
    /** A cough as the burner catches. */
    public static final DeferredHolder<SoundEvent, SoundEvent> COMBUSTION_GENERATOR_IGNITE = register("block.combustion_generator.ignite");
    /** A steady puttering thrum. */
    public static final DeferredHolder<SoundEvent, SoundEvent> COMBUSTION_GENERATOR_RUN = register("block.combustion_generator.run");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private Tier5Sounds() {}
}
