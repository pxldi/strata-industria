package dev.strataindustria.registry;

import dev.strataindustria.electric.BatteryBoxItem;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.Melt;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

/** Tier 5 (electric) items, kept apart from the earlier tiers' items. */
public final class Tier5Items {
    /** Tier 4 spec 4.6: a steel rod magnetised by rich magnetite; the dynamo and motors need one. */
    public static final DeferredItem<Item> MAGNET = ModItems.ITEMS.registerSimpleItem("magnet");

    // Spec 4.1: new forms of existing metals.
    public static final DeferredItem<Item> COPPER_ROD = ModItems.ITEMS.registerSimpleItem("copper_rod");
    public static final DeferredItem<Item> COPPER_WIRE = ModItems.ITEMS.registerSimpleItem("copper_wire");
    public static final DeferredItem<Item> LEAD_PLATE = ModItems.ITEMS.registerSimpleItem("lead_plate");
    public static final DeferredItem<Item> RED_ALLOY_ROD = ModItems.ITEMS.registerSimpleItem("red_alloy_rod");
    public static final DeferredItem<Item> RED_ALLOY_WIRE = ModItems.ITEMS.registerSimpleItem("red_alloy_wire");
    /** Spec 9.1: the steel die the wiremill and extruder pull metal through. */
    public static final DeferredItem<Item> DRAW_PLATE = ModItems.ITEMS.registerSimpleItem("draw_plate", p -> p.stacksTo(1));

    // Spec 4.5 and 9.2 to 9.3: the components every machine is built from.
    public static final DeferredItem<Item> CIRCUIT_BOARD = ModItems.ITEMS.registerSimpleItem("circuit_board");
    public static final DeferredItem<Item> BASIC_CIRCUIT = ModItems.ITEMS.registerSimpleItem("basic_circuit");
    public static final DeferredItem<Item> ELECTRIC_MOTOR = ModItems.ITEMS.registerSimpleItem("electric_motor");

    // Spec 5.2 and 5.3: rubber, which insulates every cable.
    public static final DeferredItem<Item> RAW_RUBBER = ModItems.ITEMS.registerSimpleItem("raw_rubber");
    public static final DeferredItem<Item> COMPOUNDED_RUBBER = ModItems.ITEMS.registerSimpleItem("compounded_rubber");
    public static final DeferredItem<Item> RUBBER = ModItems.ITEMS.registerSimpleItem("rubber");

    /** Spec 5.2: a bucket of latex from a full tap cup or a barrel; the bucket comes back when it is poured. */
    public static final DeferredItem<Item> LATEX_BUCKET = ModItems.ITEMS.registerSimpleItem("latex_bucket",
            p -> p.stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET));
    public static final DeferredItem<BlockItem> TREE_TAP = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.TREE_TAP);

    public static final DeferredItem<BlockItem> LV_CABLE = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.LV_CABLE);
    public static final DeferredItem<BlockItem> MV_CABLE = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.MV_CABLE);
    public static final DeferredItem<BlockItem> KINETIC_DYNAMO = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.KINETIC_DYNAMO);
    public static final DeferredItem<BatteryBoxItem> BATTERY_BOX = ModItems.ITEMS.registerItem("battery_box",
            p -> new BatteryBoxItem(Tier5Blocks.BATTERY_BOX.get(), p), p -> p.stacksTo(1).useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> LV_MACHINE_HULL = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.LV_MACHINE_HULL);
    public static final DeferredItem<BlockItem> ELECTRIC_FURNACE = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.ELECTRIC_FURNACE);
    public static final DeferredItem<BlockItem> MACERATOR = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.MACERATOR);
    public static final DeferredItem<BlockItem> WIREMILL = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.WIREMILL);
    public static final DeferredItem<BlockItem> BENDER = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.BENDER);
    public static final DeferredItem<BlockItem> LATHE = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.LATHE);

    public static final DeferredItem<BlockItem> STEAM_TURBINE = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.STEAM_TURBINE);
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR = ModItems.ITEMS.registerSimpleBlockItem(Tier5Blocks.COMBUSTION_GENERATOR);

    /** Metal content of the new forms (spec 4.1): rods are half an ingot, wire a quarter, plates a whole one. */
    public static void metalContent(Map<Item, Melt> map) {
        map.put(COPPER_ROD.get(), Melt.of(Metal.COPPER, 50, 0));
        map.put(COPPER_WIRE.get(), Melt.of(Metal.COPPER, 25, 0));
        map.put(LEAD_PLATE.get(), Melt.of(Metal.LEAD, 100, 0));
        map.put(RED_ALLOY_ROD.get(), Alloy.parts(Metal.RED_ALLOY, 50));
        map.put(RED_ALLOY_WIRE.get(), Alloy.parts(Metal.RED_ALLOY, 25));
        // Spec 4.2: redstone counts only inside a melt, and dissolves only into molten copper.
        map.put(Items.REDSTONE, Melt.of(Metal.REDSTONE, 25, 0));
        map.put(Items.REDSTONE_BLOCK, Melt.of(Metal.REDSTONE, 225, 0));
    }

    /** Loads the class so its items join the register before it fires. */
    public static void init() {}

    private Tier5Items() {}
}
