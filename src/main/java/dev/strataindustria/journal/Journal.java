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
    public static final String PATTERN_PRESSED = "pattern_pressed";
    public static final String CRUCIBLE_MOLTEN = "crucible_molten";
    public static final String STONE_ANVIL = "stone_anvil";
    public static final String PROSPECT = "prospect";
    public static final String BRONZE_ARMOUR = "bronze_armour";
    public static final String BLOOMERY_BUILT = "bloomery_built";
    public static final String BLOOM_REFINED = "bloom_refined";
    public static final String PATTERN_RECORDED = "pattern_recorded";
    public static final String BRIGHT_STRIKE = "bright_strike";
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
    // Tier 5 (spec 15)
    public static final String LATEX_PIPED = "latex_piped";
    public static final String CINNABAR_MINED = "cinnabar_mined";
    public static final String DYNAMO = "dynamo";
    public static final String FIRST_MACHINE = "first_machine";
    public static final String TURBINE = "turbine";
    public static final String MACERATOR = "macerator";
    public static final String ASSEMBLER = "assembler";
    public static final String POWER_HAMMER = "power_hammer";
    public static final String BATTERY = "battery";
    public static final String ELECTRIC_HEAT = "electric_heat";
    public static final String SULFURIC_ACID = "sulfuric_acid";
    public static final String ELECTROLYSIS = "electrolysis";
    public static final String MV_UPGRADE = "mv_upgrade";
    public static final String TRANSFORMER = "transformer";
    public static final String POWER_LINE = "power_line";
    public static final String ITEM_PIPE = "item_pipe";
    public static final String STORAGE = "storage";
    public static final String ORE_SCAN = "ore_scan";
    public static final String ELECTRIC_CHAIN = "electric_chain";
    public static final String STETHOSCOPE = "stethoscope";
    public static final String LIGHTNING_BANK = "lightning_bank";
    // Outposts and transport spec 11, tier 2 on foot
    public static final String PACK_FRAME_WORN = "pack_frame_worn";
    public static final String HANDCART_HAUL = "handcart_haul";
    public static final String TRAIL_MARKED = "trail_marked";
    // Outposts and transport (outposts spec 11)
    public static final String CHARTER_LINKED = "charter_linked";
    public static final String TUB_ARRIVED = "tub_arrived";
    public static final String PONY_HARNESSED = "pony_harnessed";
    public static final String WINCH_HAULED = "winch_hauled";

    /** Blocks of overhead span a consumer must be served across for the power line goal. */
    public static final int POWER_LINE_SPAN = 64;

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

    /** For goals of a charter: every online owner of it, and the players on the owner's team. */
    public static void awardOwners(ServerLevel level, dev.strataindustria.transport.outpost.Charter charter, String event) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (dev.strataindustria.transport.outpost.OutpostPlan.isOwner(level, player, charter)) award(player, event);
        }
    }

    private Journal() {}
}
