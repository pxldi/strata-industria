package dev.strataindustria.journal;

import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
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
 * The field journal (spec 3.6 and 11): an advancement tab of goals from the first loose rock to the
 * bronze anvil. Goals that no vanilla trigger covers fire a named {@link JournalTrigger} event.
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
    public static final String CRUCIBLE_MOLTEN = "crucible_molten";
    public static final String STONE_ANVIL = "stone_anvil";
    public static final String PROSPECT = "prospect";
    public static final String BRONZE_ARMOUR = "bronze_armour";
    public static final String BLOOMERY_BUILT = "bloomery_built";
    public static final String BLOOM_REFINED = "bloom_refined";
    public static final String PATTERN_RECORDED = "pattern_recorded";
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

    /** Block events count for players this close: whoever lit the kiln is standing near it. */
    static final double NEARBY = 16;

    public static final Identifier ROOT = StrataIndustria.id("journal/root");

    /** Goals in the order the journal suggests them, for the reminder on login. */
    public static final List<String> GOALS = List.of(
            "t0/loose_rock", "t0/knap", "t0/twine", "t0/stone_axe", "t0/log", "t0/fire", "t0/crafting_table", "t0/clay",
            "t1/clay_forming", "t1/pit_kiln", "t1/charcoal", "t1/forge", "t1/nugget", "t1/crucible",
            "t2/melt", "t2/copper_ingot", "t2/copper_pickaxe", "t2/alloy_metal", "t2/quern", "t2/bronze",
            "t2/stone_anvil", "t2/smith", "t2/bronze_tools", "t2/bronze_armour", "t2/prospectors_pick", "t2/bronze_anvil",
            "t3/fire_clay", "t3/fire_brick", "t3/iron_ore", "t3/bloomery", "t3/bloom", "t3/refine", "t3/iron_pickaxe",
            "t3/bucket", "t3/furnace", "t3/weld",
            "t3/rotation", "t3/water_power", "t3/millstone", "t3/saw_mill", "t3/bellows", "t3/pattern",
            "t3/trip_hammer", "t3/core_sample", "t3/wash", "t3/hide", "t3/leather", "t3/iron_anvil",
            "t4/coal", "t4/coke_oven", "t4/coke", "t4/creosote", "t4/refractory_crucible", "t4/molten_iron", "t4/steel",
            "t4/sphalerite", "t4/roast", "t4/brass", "t4/solder", "t4/steel_anvil");

    public static Identifier goal(String path) {
        return StrataIndustria.id("journal/" + path);
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

    /** The first goal this player has not reached yet, or null once the bronze age is done. */
    public static AdvancementHolder nextGoal(ServerPlayer player) {
        var advancements = player.level().getServer().getAdvancements();
        for (String path : GOALS) {
            AdvancementHolder holder = advancements.get(goal(path));
            if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) return holder;
        }
        return null;
    }

    private Journal() {}
}
