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
            .comment("The furnace is built from fire bricks (tier 3) instead of cobblestone.")
            .define("gateFurnace", true);
    public static final ModConfigSpec.BooleanValue REMOVE_FURNACE_CHARCOAL = BUILDER
            .comment("Remove log to charcoal smelting, so the charcoal pit stays the charcoal source.")
            .define("removeFurnaceCharcoal", true);
    public static final ModConfigSpec.BooleanValue REMOVE_ORE_SMELTING = BUILDER
            .comment("Remove smelting and blasting of vanilla ores and raw iron, copper and gold.")
            .define("removeOreSmelting", true);
    public static final ModConfigSpec.BooleanValue REMOVE_BLAST_FURNACE = BUILDER
            .comment("Remove the vanilla blast furnace recipe.")
            .define("removeBlastFurnace", true);
    public static final ModConfigSpec.BooleanValue IRON_STAT_OVERRIDE = BUILDER
            .comment("Vanilla iron tools and armour get the wrought iron stats (520 durability, tougher armour). Needs a restart.")
            .define("ironStatOverride", true);
    public static final ModConfigSpec.BooleanValue STRIPPING_DROPS_BARK = BUILDER
            .comment("Stripping a log with an axe drops a piece of bark.")
            .define("strippingDropsBark", true);
    public static final ModConfigSpec.BooleanValue HIDES_INSTEAD_OF_LEATHER = BUILDER
            .comment("Animals drop raw hides instead of leather; leather is tanned in a soaking barrel.")
            .define("hidesInsteadOfLeather", true);
    public static final ModConfigSpec.BooleanValue REPLACE_IRON_GEAR = BUILDER
            .comment("Replace the recipes of vanilla iron tools and armour with smithing and plates.")
            .define("replaceIronGear", true);
    public static final ModConfigSpec.BooleanValue REPLACE_GOLD_GEAR = BUILDER
            .comment("Replace the recipes of vanilla gold tools and armour with casting, smithing and plates.")
            .define("replaceGoldGear", true);
    public static final ModConfigSpec.BooleanValue GOLEM_DROPS_NUGGETS = BUILDER
            .comment("Iron golems drop iron nuggets instead of ingots.")
            .define("golemDropsNuggets", true);
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

    // ---------------------------------------------------------------- heat
    static {
        BUILDER.comment("Item heat.").push("heat");
    }

    public static final ModConfigSpec.BooleanValue HEAT_BURN_PLAYER = BUILDER
            .comment("Holding an item at 480 °C or hotter without tongs in the off hand burns the player.")
            .define("burnPlayer", true);

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

    // ---------------------------------------------------------------- crucible and casting
    static {
        BUILDER.comment("Crucible melting and casting.").push("crucible");
    }

    public static final ModConfigSpec.IntValue CRUCIBLE_CAPACITY = BUILDER
            .comment("Metal units a crucible holds (100 units is one ingot).")
            .defineInRange("capacity", 400, 100, 4000);

    static {
        BUILDER.pop();
        BUILDER.push("casting");
    }

    public static final ModConfigSpec.DoubleValue INGOT_MOLD_BREAK = BUILDER
            .comment("Chance an ingot mold breaks when the ingot is taken out.")
            .defineInRange("ingotMoldBreak", 0.05, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue TOOL_MOLD_BREAK = BUILDER
            .comment("Chance a tool mold breaks when the cast part is taken out.")
            .defineInRange("toolMoldBreak", 0.10, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- smithing
    static {
        BUILDER.comment("Anvil smithing.").push("smithing");
    }

    public static final ModConfigSpec.BooleanValue SMITHING_RANDOM_TARGETS = BUILDER
            .comment("Each world gets its own smithing targets. Off uses each recipe's default target.")
            .define("randomTargets", true);
    public static final ModConfigSpec.IntValue SMITHING_HIT_COOLING = BUILDER
            .comment("Degrees each hit takes off a tier 3 or higher workpiece, on top of normal cooling.")
            .defineInRange("hitCooling", 20, 0, 200);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- bloomery
    static {
        BUILDER.comment("The bloomery.").push("bloomery");
    }

    public static final ModConfigSpec.IntValue BLOOMERY_BURN_TICKS = BUILDER
            .comment("Ticks a bloomery run takes at 1200 °C or hotter, before bellows shorten it.")
            .defineInRange("runTicks", 12000, 200, 240000);
    public static final ModConfigSpec.IntValue BLOOMERY_MIN_TEMPERATURE = BUILDER
            .comment("Lowest temperature, in °C, at which a bloomery makes a bloom.")
            .defineInRange("minTemperature", 1200, 800, 1600);
    public static final ModConfigSpec.IntValue BLOOMERY_FULL_YIELD_TEMPERATURE = BUILDER
            .comment("Temperature, in °C, from which a bloomery gives its full yield.")
            .defineInRange("fullYieldTemperature", 1300, 800, 1800);
    public static final ModConfigSpec.DoubleValue BLOOMERY_LOW_YIELD = BUILDER
            .comment("Share of the yield a bloomery gives below the full yield temperature.")
            .defineInRange("lowYield", 0.85, 0.1, 1.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- kinetics
    static {
        BUILDER.comment("Mechanical power.").push("kinetics");
    }

    public static final ModConfigSpec.IntValue KINETIC_MAX_NETWORK = BUILDER
            .comment("Most kinetic blocks one network may have; a larger network stops.")
            .defineInRange("maxNetworkSize", 512, 16, 4096);
    public static final ModConfigSpec.IntValue KINETIC_WOODEN_SPEED_LIMIT = BUILDER
            .comment("RPM above which wooden parts overspeed and the network stops.")
            .defineInRange("woodenSpeedLimit", 64, 8, 1024);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- sluice and barrel
    static {
        BUILDER.comment("The sluice.").push("sluice");
    }

    public static final ModConfigSpec.IntValue SLUICE_TICKS_PER_ITEM = BUILDER
            .comment("Ticks a sluice takes to wash one item.")
            .defineInRange("ticksPerItem", 60, 1, 6000);

    static {
        BUILDER.pop();
        BUILDER.comment("The soaking barrel.").push("barrel");
    }

    public static final ModConfigSpec.IntValue BARREL_RAIN_FILL = BUILDER
            .comment("Millibuckets of rain an open soaking barrel collects per tick. 0 turns rain filling off.")
            .defineInRange("rainFillPerTick", 1, 0, 100);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- journal
    static {
        BUILDER.comment("The field journal.").push("journal");
    }

    public static final ModConfigSpec.BooleanValue JOURNAL_GIVE_ON_JOIN = BUILDER
            .comment("Give each player a field journal the first time they join.")
            .define("giveOnJoin", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
