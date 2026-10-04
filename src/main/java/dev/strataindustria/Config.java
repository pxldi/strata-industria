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
    public static final ModConfigSpec.BooleanValue BRANCH_SNAPPING = BUILDER
            .comment("Use leaves with an empty hand to snap a branch off for sticks and bark.")
            .define("branchSnapping", true);
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
            .defineInRange("burnTicks", 2400, 20, 72000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- heat
    static {
        BUILDER.comment("Item heat.").push("heat");
    }

    public static final ModConfigSpec.BooleanValue HEAT_BURN_PLAYER = BUILDER
            .comment("Off by default. When on, holding an item at 480 °C or hotter without tongs in the off hand burns the player.")
            .define("burnPlayer", false);

    public static final ModConfigSpec.IntValue HEAT_MAX_PIPE_LENGTH = BUILDER
            .comment("How many heat pipe blocks heat crosses at most on its way from a firebox to a consumer.")
            .defineInRange("maxPipeLength", 32, 1, 256);

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
    public static final ModConfigSpec.IntValue CLAY_CRUCIBLE_MAX = BUILDER
            .comment("Hottest a clay crucible gets, in degrees; above this it holds steady (tier 4 spec 3).")
            .defineInRange("clayMaxTemperature", 1400, 1000, 2000);

    static {
        BUILDER.pop();
        BUILDER.push("casting");
    }

    public static final ModConfigSpec.DoubleValue INGOT_MOLD_BREAK = BUILDER
            .comment("Chance an ingot mold breaks (0 = fired molds last) when the ingot is taken out.")
            .defineInRange("ingotMoldBreak", 0.0, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue TOOL_MOLD_BREAK = BUILDER
            .comment("Chance a tool mold breaks when the cast part is taken out.")
            .defineInRange("toolMoldBreak", 0.0, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue REFRACTORY_MOLD_BREAK = BUILDER
            .comment("Chance a refractory mold breaks when the casting is taken out.")
            .defineInRange("refractoryMoldBreak", 0.0, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- smithing
    static {
        BUILDER.comment("Anvil smithing.").push("smithing");
    }

    public static final ModConfigSpec.IntValue SMITHING_HIT_COOLING = BUILDER
            .comment("Degrees each blow takes off a tier 3 or higher workpiece, on top of normal cooling.")
            .defineInRange("hitCooling", 15, 0, 200);
    public static final ModConfigSpec.IntValue SMITHING_BRIGHT_MARGIN = BUILDER
            .comment("Percent above its working temperature a piece must be for a blow to count as bright.")
            .defineInRange("brightMargin", 12, 0, 100);
    public static final ModConfigSpec.IntValue SMITHING_BEAT_WINDOW = BUILDER
            .comment("Ticks either side of the hammer's rebound glint in which a strike is a true blow (counts twice).")
            .defineInRange("beatWindow", 3, 0, 8);
    public static final ModConfigSpec.IntValue SMITHING_SPARKS = BUILDER
            .comment("Percent of the normal number of sparks a blow throws. 0 turns them off.")
            .defineInRange("sparks", 100, 0, 300);
    public static final ModConfigSpec.BooleanValue SMITHING_SCREEN_NUDGE = BUILDER
            .comment("A small push of the camera on true blows and the last blow.")
            .define("screenNudge", true);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- bloomery
    static {
        BUILDER.comment("The bloomery.").push("bloomery");
    }

    public static final ModConfigSpec.IntValue BLOOMERY_BURN_TICKS = BUILDER
            .comment("Ticks a bloomery run takes at 1200 °C or hotter, before bellows shorten it.")
            .defineInRange("runTicks", 4800, 200, 240000);
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
            .comment("RPM above which wooden parts overspeed; the part of the network turning faster stops.")
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

    // ---------------------------------------------------------------- steam
    static {
        BUILDER.comment("Boilers and steam engines.").push("steam");
    }

    public static final ModConfigSpec.BooleanValue STEAM_BOILER_EXPLOSIONS = BUILDER
            .comment("Whether a boiler that cracks from dry firing also explodes.")
            .define("boilerExplosions", false);
    public static final ModConfigSpec.DoubleValue STEAM_DRY_FIRING_DAMAGE = BUILDER
            .comment("Integrity, in percent, a dry-fired boiler loses each second.")
            .defineInRange("dryFiringDamagePerSecond", 1.0, 0.0, 100.0);
    public static final ModConfigSpec.IntValue STEAM_ENGINE_CAPACITY = BUILDER
            .comment("Stress capacity, in SU, of one steam engine.")
            .defineInRange("engineCapacity", 1024, 1, 65536);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- blast furnace
    static {
        BUILDER.comment("The blast furnace.").push("blastFurnace");
    }

    public static final ModConfigSpec.IntValue BLAST_FURNACE_TICKS_PER_INGOT = BUILDER
            .comment("Ticks per pig iron ingot with one blower; two blowers or a blowing engine halve it.")
            .defineInRange("ticksPerIngot", 200, 1, 72000);
    public static final ModConfigSpec.IntValue BLAST_FURNACE_WARMUP = BUILDER
            .comment("Ticks a cold blast furnace needs to heat its hearth.")
            .defineInRange("warmupTicks", 1200, 0, 72000);

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
    public static final ModConfigSpec.IntValue JOURNAL_HINT_MINUTES = BUILDER
            .comment("Minutes of play an open lead waits before the journal remembers its hint. -1 never; studying still does.")
            .defineInRange("hintMinutes", 10, -1, 600);
    public static final ModConfigSpec.BooleanValue JOURNAL_CHECKLIST = BUILDER
            .comment("Show the Checklist tab in the journal: every goal of the tree as a list, for players who want the overview.")
            .define("checklist", false);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- electric (tier 5 spec 20)
    static {
        BUILDER.comment("Electric networks.").push("electric");
    }

    public static final ModConfigSpec.IntValue ELECTRIC_MAX_NETWORK = BUILDER
            .comment("Most blocks one electric network may have.")
            .defineInRange("maxNetworkSize", 2048, 16, 65536);
    public static final ModConfigSpec.DoubleValue ELECTRIC_MAX_PATH_LOSS = BUILDER
            .comment("A consumer whose path from the nearest source loses this fraction of the power or more is not served.")
            .defineInRange("maxPathLoss", 0.5, 0.01, 0.99);
    public static final ModConfigSpec.DoubleValue ELECTRIC_DYNAMO_JOULES_PER_RPM = BUILDER
            .comment("J/t a kinetic dynamo makes per RPM of its shaft.")
            .defineInRange("dynamoJoulesPerRpm", 0.25, 0.01, 4.0);
    public static final ModConfigSpec.DoubleValue ELECTRIC_TRANSFORMER_LOSS = BUILDER
            .comment("Fraction of the power a transformer loses on its way through.")
            .defineInRange("transformerLoss", 0.02, 0.0, 0.5);
    public static final ModConfigSpec.IntValue ELECTRIC_FE_RATIO = BUILDER
            .comment("FE for each J at the energy adapter.")
            .defineInRange("feRatio", 4, 1, 1000);
    public static final ModConfigSpec.IntValue ELECTRIC_FE_INPUT_MAX = BUILDER
            .comment("Most J/t an energy adapter accepts as FE from outside. 0 forbids FE input.")
            .defineInRange("feInputMax", 32, 0, 512);
    public static final ModConfigSpec.IntValue ELECTRIC_MAX_SPAN = BUILDER
            .comment("Longest overhead span between two pole insulators, in blocks.")
            .defineInRange("maxSpan", 24, 4, 64);

    static {
        BUILDER.pop();
        BUILDER.comment("Mechanical and electric pumps.").push("pumps");
    }

    public static final ModConfigSpec.BooleanValue PUMPS_OCEAN_BRINE = BUILDER
            .comment("A pump whose intake is in an ocean or beach biome pumps brine instead of water.")
            .define("oceanBrine", true);

    static {
        BUILDER.pop();
        BUILDER.comment("Tree taps and rubber.").push("rubber");
    }

    public static final ModConfigSpec.IntValue RUBBER_TAP_TICKS_PER_MB = BUILDER
            .comment("Ticks a tree tap takes for each step of the tappable data map's amount (1 mB for jungle logs).")
            .defineInRange("tapTicksPerMb", 10, 1, 1200);
    public static final ModConfigSpec.IntValue RUBBER_MAX_TAPS_PER_TREE = BUILDER
            .comment("Most taps that draw from one tree; the earliest placed draw first.")
            .defineInRange("maxTapsPerTree", 4, 1, 64);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Belts, chutes and inserters.").push("automation");
    }

    public static final ModConfigSpec.IntValue AUTOMATION_CHAIN_OPERATIONS = BUILDER
            .comment("Items a machine must finish in a row, with nobody opening it, to count towards the automated chain goal.")
            .defineInRange("chainOperations", 64, 1, 4096);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Item pipes and storage (tier 5 spec 12).").push("logistics");
    }

    public static final ModConfigSpec.IntValue LOGISTICS_MAX_PIPE_NETWORK = BUILDER
            .comment("Most pipe blocks one item pipe search looks through.")
            .defineInRange("maxPipeNetwork", 512, 16, 8192);
    public static final ModConfigSpec.IntValue LOGISTICS_MAX_STORAGE_INVENTORIES = BUILDER
            .comment("Most inventories one storage controller indexes.")
            .defineInRange("maxStorageInventories", 64, 1, 512);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Outposts and transport (outposts spec 3 and 13).").push("outposts");
    }

    public static final ModConfigSpec.IntValue OUTPOSTS_MIN_SPACING = BUILDER
            .comment("Blocks between two charters of any owner.")
            .defineInRange("minSpacing", 160, 0, 4096);
    public static final ModConfigSpec.IntValue OUTPOSTS_MAX_CHARTERS = BUILDER
            .comment("Charters one owner can have posted.")
            .defineInRange("maxCharters", 8, 1, 256);
    public static final ModConfigSpec.IntValue OUTPOSTS_MAX_DISTRICT = BUILDER
            .comment("Charters one district can hold.")
            .defineInRange("maxDistrict", 8, 2, 256);
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> OUTPOSTS_AREA_BY_TIER = BUILDER
            .comment("Side in chunks of a charter's loaded square for a link of tier 3, 4, 5 and 6. Odd numbers only.")
            .defineList("areaByTier", java.util.List.of(3, 5, 7, 9), () -> 3, o -> o instanceof Integer i && i >= 1 && i <= 15 && i % 2 == 1);
    public static final ModConfigSpec.IntValue OUTPOSTS_MAX_CHUNKS_PER_OWNER = BUILDER
            .comment("Chunks one owner's charters can keep loaded.")
            .defineInRange("maxChunksPerOwner", 200, 9, 4096);
    public static final ModConfigSpec.BooleanValue OUTPOSTS_REQUIRE_OWNER_ONLINE = BUILDER
            .comment("Charter areas load only while one of their owners is online. Turn off to keep them loaded always.")
            .define("requireOwnerOnline", true);
    public static final ModConfigSpec.IntValue TRAIL_STEP = BUILDER
            .comment("Most blocks between two trail marks (cairns and blazes) for them to count as one trail.")
            .defineInRange("trailStep", 64, 8, 512);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Rail vehicles (outposts spec 5, 6 and 13).").push("transport");
    }

    public static final ModConfigSpec.IntValue TRANSPORT_MAX_CONSIST_T3 = BUILDER
            .comment("Tubs that can follow the lead of a wooden tramway consist.")
            .defineInRange("maxConsistT3", 4, 1, 16);
    public static final ModConfigSpec.DoubleValue TRANSPORT_PONY_HAY_PER_TRIP = BUILDER
            .comment("Hay bales a pony eats per round trip.")
            .defineInRange("ponyHayPerTrip", 0.5, 0.0, 16.0);
    public static final ModConfigSpec.IntValue TRANSPORT_WINCH_REACH = BUILDER
            .comment("Track blocks an incline winch hauls.")
            .defineInRange("winchReach", 32, 4, 128);
    public static final ModConfigSpec.IntValue TRANSPORT_MAX_TICKETED_CONSISTS = BUILDER
            .comment("Driverless consists one owner can have moving at once. Each keeps nine chunks loaded around it.")
            .defineInRange("maxTicketedConsists", 4, 1, 32);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
