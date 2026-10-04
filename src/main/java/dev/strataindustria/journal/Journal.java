package dev.strataindustria.journal;

import dev.strataindustria.StrataIndustria;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The field journal (spec 3.6 and 11): a tree of goal advancements from the first loose rock onwards, which the
 * leads notebook ({@link Leads}) reads as its leads. Goals that no vanilla trigger covers fire a named
 * {@link JournalTrigger} event.
 */
public final class Journal {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, StrataIndustria.MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, JournalTrigger> TRIGGER =
            TRIGGERS.register("journal", JournalTrigger::new);

    public static final String KNAP = "knap";
    public static final String CLAY_FORMING = "clay_forming";
    public static final String FIRE_PIT_LIT = "fire_pit_lit";
    public static final String PIT_KILN_FIRED = "pit_kiln_fired";
    public static final String BRICK_KILN_FIRED = "brick_kiln_fired";
    public static final String CASTING_TABLE_POURED = "casting_table_poured";
    public static final String CRUCIBLE_MOLTEN = "crucible_molten";
    public static final String STONE_ANVIL = "stone_anvil";
    public static final String PROSPECT = "prospect";
    public static final String BRONZE_ARMOUR = "bronze_armour";
    public static final String BLOOMERY_BUILT = "bloomery_built";
    public static final String BLOOM_REFINED = "bloom_refined";
    public static final String PATTERN_RECORDED = "pattern_recorded";
    public static final String QUICK_SMITH = "quick_smith";
    public static final String ROTATION = "rotation";
    public static final String WATER_POWER = "water_power";
    public static final String MILLSTONE = "millstone";
    public static final String BELLOWS = "bellows";
    public static final String SAW_MILL = "saw_mill";
    public static final String TRIP_HAMMER = "trip_hammer";
    public static final String WASH = "wash";
    public static final String LEATHER = "leather";
    public static final String COKE_OVEN_BUILT = "coke_oven_built";
    public static final String MOLTEN_IRON = "molten_iron";
    public static final String BOILER = "boiler";
    public static final String STEAM_ENGINE = "steam_engine";
    public static final String STEAM_HAMMER = "steam_hammer";
    public static final String CRUSHER = "crusher";
    public static final String BLAST_FURNACE = "blast_furnace";
    public static final String CONVERTER = "converter";
    public static final String HEAT_NETWORK = "heat_network";
    public static final String AUTOMATED_CHAIN = "automated_chain";

    /** Block events count for players this close: whoever lit the kiln is standing near it. */
    static final double NEARBY = 16;

    public static final Identifier ROOT = StrataIndustria.id("journal/root");

    /** Joins lang keys inside one note argument; the notebook shows them translated as a list. */
    public static final String ARG_LIST = "|";

    public static Identifier goal(String path) {
        return StrataIndustria.id("journal/" + path);
    }

    /** "t3/iron_ore" becomes the lang key base "journal.strataindustria.t3.iron_ore". */
    public static String key(String path) {
        return "journal." + StrataIndustria.MOD_ID + "." + path.replace('/', '.');
    }

    public static void award(ServerPlayer player, String event) {
        TRIGGER.get().trigger(player, event);
    }

    /** For events that happen to a block with nobody's hand on it, such as a kiln burning out. */
    public static void awardNear(Level level, BlockPos pos, String event) {
        if (!(level instanceof ServerLevel server)) return;
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(NEARBY))) {
            award(player, event);
        }
    }

    private Journal() {}
}
