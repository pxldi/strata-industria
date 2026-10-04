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

    /** Pressing a lump of clay into shape. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CLAY_SHAPE = register("clay.shape");
    /** The formed piece comes away from the leftover clay. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CLAY_FINISH = register("clay.finish");
    /** Straw thatch laid on a pit kiln. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KILN_STRAW = register("pit_kiln.straw");
    /** A log stacked on a pit kiln. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KILN_LOG = register("pit_kiln.log");
    /** The kiln burns out and the pots are fired. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KILN_FIRED = register("pit_kiln.fired");

    /** The spindle of a bow drill working against the hearth board. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRESTARTER_DRILL = register("firestarter.drill");
    /** Tinder catches in the fire pit. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PIT_IGNITE = register("fire_pit.ignite");
    /** The fire pit goes out: a fizz in the rain, a soft fade when the embers cool. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PIT_EXTINGUISH = register("fire_pit.extinguish");
    /** A stick held in the flames catches as a torch. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PIT_TORCH = register("fire_pit.torch");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    private ModSounds() {}
}
