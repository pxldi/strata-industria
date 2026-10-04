package dev.strataindustria.registry;

import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.metal.CastMoldItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

/** Tier 4 (steel and steam) items, kept apart from the earlier tiers' items. */
public final class Tier4Items {
    /** Tier 4 spec 4.6: native sulfur ore drops this; with charcoal dust it makes gunpowder. */
    public static final DeferredItem<Item> SULFUR = ModItems.ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> SULFUR_DUST = ModItems.ITEMS.registerSimpleItem("sulfur_dust");
    /** Ground charcoal: carbon for a melt (spec 4.2). */
    public static final DeferredItem<Item> CHARCOAL_DUST = ModItems.ITEMS.registerSimpleItem("charcoal_dust");

    // Spec 5.1: coke and its oven.
    public static final DeferredItem<Item> COKE = ModItems.ITEMS.registerSimpleItem("coke");
    public static final DeferredItem<Item> COKE_DUST = ModItems.ITEMS.registerSimpleItem("coke_dust");
    public static final DeferredItem<BlockItem> COKE_BLOCK = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COKE_BLOCK);
    public static final DeferredItem<Item> UNFIRED_COKE_OVEN_BRICK = ModItems.ITEMS.registerSimpleItem("unfired_coke_oven_brick");
    public static final DeferredItem<Item> COKE_OVEN_BRICK = ModItems.ITEMS.registerSimpleItem("coke_oven_brick");
    public static final DeferredItem<BlockItem> COKE_OVEN_BRICKS = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COKE_OVEN_BRICKS);
    public static final DeferredItem<BlockItem> COKE_OVEN_DOOR = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COKE_OVEN_DOOR);
    /** Spec 5.2: a bucket of creosote from the oven's tank; the bucket comes back when it is used in a recipe. */
    public static final DeferredItem<Item> CREOSOTE_BUCKET = ModItems.ITEMS.registerSimpleItem("creosote_bucket",
            p -> p.stacksTo(1).craftRemainder(Items.BUCKET));

    // Spec 4.6: treated wood.
    public static final DeferredItem<BlockItem> TREATED_PLANKS = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_PLANKS);
    public static final DeferredItem<BlockItem> TREATED_SLAB = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_SLAB);
    public static final DeferredItem<BlockItem> TREATED_STAIRS = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_STAIRS);
    public static final DeferredItem<BlockItem> TREATED_FENCE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_FENCE);
    public static final DeferredItem<Item> TREATED_STICK = ModItems.ITEMS.registerSimpleItem("treated_stick");

    // Spec 6.1: refractory ceramics, and the clay gear mold.
    public static final DeferredItem<Item> UNFIRED_REFRACTORY_CRUCIBLE = ModItems.ITEMS.registerSimpleItem("unfired_refractory_crucible",
            p -> p.stacksTo(1));
    public static final DeferredItem<BlockItem> REFRACTORY_CRUCIBLE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.REFRACTORY_CRUCIBLE,
            p -> p.stacksTo(1));
    public static final DeferredItem<Item> UNFIRED_REFRACTORY_INGOT_MOLD = ModItems.ITEMS.registerSimpleItem("unfired_refractory_ingot_mold",
            p -> p.stacksTo(16));
    public static final DeferredItem<CastMoldItem> REFRACTORY_INGOT_MOLD = ModItems.ITEMS.registerItem("refractory_ingot_mold",
            p -> new CastMoldItem(null, false, true, p), p -> p.stacksTo(16));
    public static final DeferredItem<Item> UNFIRED_REFRACTORY_GEAR_MOLD = ModItems.ITEMS.registerSimpleItem("unfired_refractory_gear_mold",
            p -> p.stacksTo(16));
    public static final DeferredItem<CastMoldItem> REFRACTORY_GEAR_MOLD = ModItems.ITEMS.registerItem("refractory_gear_mold",
            p -> new CastMoldItem(null, true, true, p), p -> p.stacksTo(16));
    public static final DeferredItem<Item> UNFIRED_GEAR_MOLD = ModItems.ITEMS.registerSimpleItem("unfired_gear_mold", p -> p.stacksTo(16));
    public static final DeferredItem<CastMoldItem> GEAR_MOLD = ModItems.ITEMS.registerItem("gear_mold",
            p -> new CastMoldItem(null, true, false, p), p -> p.stacksTo(16));

    public static final DeferredItem<BlockItem> STEEL_ANVIL = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.STEEL_ANVIL);

    // Spec 11.7: iron transmission.
    public static final DeferredItem<BlockItem> IRON_AXLE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.IRON_AXLE);
    public static final DeferredItem<BlockItem> IRON_GEARBOX = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.IRON_GEARBOX);

    // Spec 8.1, 9.2, 9.3 and 10.2: steam.
    public static final DeferredItem<BlockItem> FIREBOX = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.FIREBOX);
    public static final DeferredItem<BlockItem> BRONZE_BOILER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.BRONZE_BOILER);
    public static final DeferredItem<BlockItem> CRACKED_BRONZE_BOILER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.CRACKED_BRONZE_BOILER);
    public static final DeferredItem<BlockItem> COPPER_FLUID_PIPE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COPPER_FLUID_PIPE);
    public static final DeferredItem<BlockItem> BRONZE_FLUID_PIPE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.BRONZE_FLUID_PIPE);
    public static final DeferredItem<BlockItem> STEEL_FLUID_PIPE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.STEEL_FLUID_PIPE);
    public static final DeferredItem<BlockItem> PRESSURE_GAUGE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.PRESSURE_GAUGE);
    public static final DeferredItem<BlockItem> MECHANICAL_PUMP = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.MECHANICAL_PUMP);
    public static final DeferredItem<BlockItem> STEAM_ENGINE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.STEAM_ENGINE);
    public static final DeferredItem<BlockItem> VALVE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.VALVE);
    public static final DeferredItem<BlockItem> FLUID_TANK = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.FLUID_TANK);
    public static final DeferredItem<BlockItem> STEAM_HAMMER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.STEAM_HAMMER);
    public static final DeferredItem<BlockItem> CRUSHER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.CRUSHER);
    public static final DeferredItem<BlockItem> WASHER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.WASHER);
    public static final DeferredItem<BlockItem> IRON_STEP_UP_GEARBOX = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.IRON_STEP_UP_GEARBOX);

    // Spec 12.1 and 11.6: the blast furnace, the blower, and the slag the furnace leaves.
    public static final DeferredItem<BlockItem> REFRACTORY_CASING = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.REFRACTORY_CASING);
    public static final DeferredItem<BlockItem> BLAST_FURNACE_CONTROLLER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.BLAST_FURNACE_CONTROLLER);
    public static final DeferredItem<BlockItem> TUYERE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TUYERE);
    public static final DeferredItem<BlockItem> CHARGING_HATCH = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.CHARGING_HATCH);
    public static final DeferredItem<BlockItem> TAP_HATCH = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TAP_HATCH);
    public static final DeferredItem<BlockItem> CONVERTER_CONTROLLER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.CONVERTER_CONTROLLER);
    public static final DeferredItem<BlockItem> BLOWER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.BLOWER);
    public static final DeferredItem<BlockItem> BLOWING_ENGINE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.BLOWING_ENGINE);
    public static final DeferredItem<Item> SLAG = ModItems.ITEMS.registerSimpleItem("slag");
    // Spec 8.2 and 8.4: heat pipes and the heat inlet.
    public static final DeferredItem<BlockItem> COPPER_HEAT_PIPE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COPPER_HEAT_PIPE);
    public static final DeferredItem<BlockItem> REFRACTORY_HEAT_DUCT = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.REFRACTORY_HEAT_DUCT);
    public static final DeferredItem<BlockItem> HEAT_INLET = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.HEAT_INLET);
    public static final DeferredItem<BlockItem> INSULATED_COPPER_HEAT_PIPE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.INSULATED_COPPER_HEAT_PIPE);
    public static final DeferredItem<BlockItem> INSULATED_REFRACTORY_HEAT_DUCT =
            ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.INSULATED_REFRACTORY_HEAT_DUCT);
    // Spec 8.6 and 4.6: the kiln, and the slag wool it makes for pipe insulation.
    public static final DeferredItem<BlockItem> KILN = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.KILN);
    // Spec 8.5: the roaster.
    public static final DeferredItem<BlockItem> ROASTER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.ROASTER);
    // Spec 8.7: the smelter.
    public static final DeferredItem<BlockItem> SMELTER = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.SMELTER);
    public static final DeferredItem<Item> SLAG_WOOL = ModItems.ITEMS.registerSimpleItem("slag_wool");
    /** Spec 4.6: ground slag, a fertiliser worth two bone meal. */
    public static final DeferredItem<dev.strataindustria.ironworks.SlagDustItem> SLAG_DUST = ModItems.ITEMS.registerItem("slag_dust",
            dev.strataindustria.ironworks.SlagDustItem::new);

    // Spec 4.4 and 5.3: roasted sphalerite, by grade, and the small piece from a surface indicator.
    public static final Map<OreGrade, DeferredItem<Item>> ZINC_CALCINES = new EnumMap<>(OreGrade.class);
    public static final DeferredItem<Item> SMALL_ZINC_CALCINE = ModItems.ITEMS.registerSimpleItem("small_zinc_calcine");

    static {
        for (OreGrade grade : OreGrade.values()) {
            ZINC_CALCINES.put(grade, ModItems.ITEMS.registerSimpleItem(grade.prefix() + "zinc_calcine"));
        }
    }

    public static Item zincCalcine(OreGrade grade) {
        return ZINC_CALCINES.get(grade).get();
    }

    /** Loads the class so its items join the register before it fires. */
    public static void init() {}

    private Tier4Items() {}
}
