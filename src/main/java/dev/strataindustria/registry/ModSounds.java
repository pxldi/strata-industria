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

    /** Hot metal hissing in water. */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUENCH = register("heat.quench");
    /** Bare skin on hot metal. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAR = register("heat.sear");
    /** A piece slumps into the melt. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_MELT = register("crucible.melt");
    /** Molten metal runs into a mold. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_POUR = register("crucible.pour");
    /** A cast part knocked out of its mold. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MOLD_KNOCK = register("mold.knock");
    /** A mold cracks apart. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MOLD_BREAK = register("mold.break");
    /** Stone grinding on stone as the quern turns. */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUERN_GRIND = register("quern.grind");
    /** Something set on the quern stone. */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUERN_LOAD = register("quern.load");
    /** A ground item spills off the quern. */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUERN_DONE = register("quern.done");
    /** A hammer blow on the anvil. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SMITH_HIT = register("anvil.hit");
    /** The last blow that finishes a piece. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SMITH_DONE = register("anvil.done");
    /** Raw rock dressed into a stone anvil. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANVIL_DRESS = register("anvil.dress");
    /** A prospector's pick tapping the rock. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PROSPECT = register("prospect");
    /** Charcoal catching in the forge. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_IGNITE = register("forge.ignite");

    /** The spindle of a bow drill working against the hearth board. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRESTARTER_DRILL = register("firestarter.drill");
    /** Tinder catches in the fire pit. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PIT_IGNITE = register("fire_pit.ignite");
    /** The fire pit goes out: a fizz in the rain, a soft fade when the embers cool. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PIT_EXTINGUISH = register("fire_pit.extinguish");
    /** A stick held in the flames catches as a torch. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PIT_TORCH = register("fire_pit.torch");

    /** Tier 3 spec 20.6: the bloomery catching, roaring, taking a charge, finishing and giving up a bloom. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMERY_LIGHT = register("bloomery.light");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMERY_ROAR = register("bloomery.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMERY_CHARGE = register("bloomery.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMERY_DONE = register("bloomery.done");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMERY_EXTRACT = register("bloomery.extract");
    /** A hammer on a raw bloom: a dull thud as the slag squeezes out. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RAW_BLOOM_HIT = register("raw_bloom.hit");
    /** A hammer on wrought iron: heavier and lower than bronze. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WROUGHT_IRON_HIT = register("wrought_iron.hit");

    /** Tier 3 spec 9.4: a weld takes, or the button is pressed when it cannot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANVIL_WELD = register("anvil.weld");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANVIL_WELD_FAIL = register("anvil.weld_fail");

    /** Tier 3 spec 20.6: mechanical power and machines. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HAND_CRANK_TURN = register("hand_crank.turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_WHEEL_TURN = register("water_wheel.turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> KINETIC_OVERSTRESS = register("kinetic.overstress");
    public static final DeferredHolder<SoundEvent, SoundEvent> MILLSTONE_GRIND = register("millstone.grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELLOWS_PUMP = register("bellows.pump");

    /** Pages of the field journal turning. */
    public static final DeferredHolder<SoundEvent, SoundEvent> JOURNAL_OPEN = register("journal.open");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    private ModSounds() {}
}
