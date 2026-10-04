package dev.strataindustria;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_DEBUG_INFO = BUILDER
            .comment("Show extra diagnostic information in tooltips and logs.")
            .define("showDebugInfo", false);

    // ---------------------------------------------------------------- worldgen
    static {
        BUILDER.comment("Rock layers and ore veins. Changes apply to newly generated chunks.").push("worldgen");
    }

    public static final ModConfigSpec.IntValue PROVINCE_SCALE = BUILDER
            .comment("Province cell size in blocks.")
            .defineInRange("provinceScale", 768, 128, 8192);
    public static final ModConfigSpec.IntValue VEIN_ATTEMPTS = BUILDER
            .comment("Vein placement attempts per 64x64 cell.")
            .defineInRange("veinAttemptsPerCell", 3, 0, 16);
    public static final ModConfigSpec.DoubleValue SHALLOW_BIAS = BUILDER
            .comment("Chance that a vein attempt is placed close to the surface.")
            .defineInRange("shallowBias", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.IntValue EMPTY_WEIGHT = BUILDER
            .comment("Weight of 'no vein' when choosing a vein for an attempt.")
            .defineInRange("emptyWeight", 40, 0, 1000);
    public static final ModConfigSpec.DoubleValue VEIN_SIZE = BUILDER
            .comment("Multiplier for all vein radii.")
            .defineInRange("veinSizeMultiplier", 1.0, 0.25, 2.0);
    public static final ModConfigSpec.IntValue INDICATOR_DEPTH = BUILDER
            .comment("Veins whose top is within this many blocks of the surface get surface indicators.")
            .defineInRange("indicatorDepth", 48, 0, 256);
    public static final ModConfigSpec.DoubleValue INDICATOR_DENSITY = BUILDER
            .comment("Multiplier for the number of surface indicators per vein.")
            .defineInRange("indicatorDensity", 1.0, 0.0, 4.0);
    public static final ModConfigSpec.BooleanValue SPAWN_GUARANTEE = BUILDER
            .comment("Force a shallow stone-tier copper vein near the world origin.")
            .define("spawnGuarantee", true);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- vanilla changes
    static {
        BUILDER.comment("Changes to vanilla behaviour. Recipe switches apply on the next datapack reload.").push("vanilla");
    }

    public static final ModConfigSpec.BooleanValue LOGS_NEED_AXE = BUILDER
            .comment("Logs cannot be broken without an axe.")
            .define("logsNeedAxe", true);
    public static final ModConfigSpec.BooleanValue REMOVE_WOOD_TOOLS = BUILDER
            .comment("Remove the recipes of wooden tools.")
            .define("removeWoodTools", true);
    public static final ModConfigSpec.BooleanValue REMOVE_STONE_TOOLS = BUILDER
            .comment("Remove the recipes of cobblestone tools; stone tools come from knapping.")
            .define("removeStoneTools", true);
    public static final ModConfigSpec.BooleanValue PLANKS_NEED_TOOLS = BUILDER
            .comment("Planks need an axe (2 per log) or a saw (4 per log).")
            .define("planksNeedTools", true);
    public static final ModConfigSpec.BooleanValue REMOVE_CAMPFIRE = BUILDER
            .comment("Remove the campfire recipe; the fire pit replaces it.")
            .define("removeCampfire", true);
    public static final ModConfigSpec.BooleanValue GATE_FURNACE = BUILDER
            .comment("Remove the furnace recipe until tier 3.")
            .define("gateFurnace", true);
    public static final ModConfigSpec.BooleanValue REPLACE_COPPER_GEAR = BUILDER
            .comment("Replace the recipes of vanilla copper tools and armour.")
            .define("replaceCopperGear", true);
    public static final ModConfigSpec.BooleanValue LEAVES_DROP_STICKS = BUILDER
            .comment("Leaves broken by hand drop a stick 20% of the time.")
            .define("leavesDropSticks", true);
    public static final ModConfigSpec.BooleanValue GRAVEL_FLINT_CHANCE = BUILDER
            .comment("Raise the flint chance of gravel to 15%.")
            .define("gravelFlintChance", true);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- kiln
    static {
        BUILDER.comment("Pit kiln firing.").push("kiln");
    }

    public static final ModConfigSpec.IntValue KILN_BURN_TICKS = BUILDER
            .comment("Ticks a lit pit kiln burns before its pieces are fired.")
            .defineInRange("burnTicks", 6000, 20, 72000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- charcoal
    static {
        BUILDER.comment("Charcoal pits.").push("charcoal");
    }

    public static final ModConfigSpec.IntValue CHARCOAL_BURN_TICKS = BUILDER
            .comment("Ticks a covered log pile burns before it turns to charcoal.")
            .defineInRange("burnTicks", 12000, 20, 72000);
    public static final ModConfigSpec.DoubleValue CHARCOAL_PER_LOG = BUILDER
            .comment("Charcoal per log in the pile.")
            .defineInRange("perLog", 0.5, 0.0625, 1.0);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
