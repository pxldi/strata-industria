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

    /** Coals catching in a firebox. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBOX_LIGHT = register("firebox.light");
    /** The steady draughty crackle of a burning firebox. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBOX_BURN = register("firebox.burn");
    /** Metal ticking as a heat pipe network heats up or cools. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAT_PIPE_TICK = register("heat_pipe.tick");
    /** The soft hollow roar of a firing kiln. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KILN_WORK = register("kiln.work");
    /** A batch comes out of the kiln fired: ceramic clinks as it cools. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KILN_DONE = register("kiln.done");
    /** The smelter pours into its mold: a thick pour with a hiss. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SMELTER_POUR = register("smelter.pour");
    /** Slow bubbling from a smelter's molten pot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SMELTER_BUBBLE = register("smelter.bubble");
    /** A pipe refuses fluid too hot for it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUID_PIPE_REFUSE = register("fluid_pipe.refuse");
    /** Water warming in a boiler: a rising hiss and creaking metal. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_HEAT = register("boiler.heat");
    /** A boiler making steam. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_RUN = register("boiler.run");
    /** The safety valve lets off steam. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_VENT = register("boiler.vent");
    /** Warning: the water is low. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_LOW_WATER = register("boiler.low_water");
    /** Metal groaning in a dry-fired boiler. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_DRY_FIRE = register("boiler.dry_fire");
    /** Water hits a dry-fired boiler. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_STEAM_BURST = register("boiler.steam_burst");
    /** The shell splits. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_CRACK = register("boiler.crack");
    /** A plate hammered over a weak spot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_REPAIR = register("boiler.repair");

    /** A stroke of the steam engine: a chuff with a piston knock. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_ENGINE_CHUFF = register("steam_engine.chuff");
    /** Spec 21.7: the valve's squeak and thunk, and liquid poured into a tank. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VALVE_OPEN = register("valve.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> VALVE_CLOSE = register("valve.close");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUID_TANK_FILL = register("fluid_tank.fill");
    /** Spec 21.7: the blowing engine's cylinder, a deep chuff with a rush of air. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOWING_ENGINE_STROKE = register("blowing_engine.stroke");
    /** Spec 21.7: the steam hammer's hiss and heavy ringing blow. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_HAMMER_STRIKE = register("steam_hammer.strike");
    /** The engine takes steam and its flywheel whirs up. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_ENGINE_START = register("steam_engine.start");
    /** The engine runs out of steam and winds down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_ENGINE_STOP = register("steam_engine.stop");
    /** The rhythmic suck and thump of a mechanical pump. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MECHANICAL_PUMP_RUN = register("mechanical_pump.run");

    /** Rock cracking between iron jaws. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUSHER_CRUSH = register("crusher.crush");

    /** A drum of wet gravel sloshing round. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WASHER_WASH = register("washer.wash");

    /** The fan's whoosh in a blower. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOWER_RUN = register("blower.run");
    /** The deep roar of a blast furnace's hearth. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLAST_FURNACE_ROAR = register("blast_furnace.roar");
    /** Pig iron runs out of the tap. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLAST_FURNACE_TAP = register("blast_furnace.tap");
    /** The fierce roaring jet of a converter blow. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONVERTER_BLOW = register("converter.blow");
    /** The flame drops and the blow is done. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONVERTER_DONE = register("converter.done");
    /** A multiblock is complete: a heavy clunk and a short chime. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MULTIBLOCK_FORM = register("multiblock.form");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private Tier4Sounds() {}
}
