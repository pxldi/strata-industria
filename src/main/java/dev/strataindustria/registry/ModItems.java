package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.LargeVesselItem;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.ceramics.SmallVesselItem;
import dev.strataindustria.fire.FirestarterItem;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.item.GroundCoverItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StrataIndustria.MOD_ID);

    public static final DeferredItem<Item> PLANT_FIBRE = ITEMS.registerSimpleItem("plant_fibre", p -> p);
    public static final DeferredItem<Item> STRAW = ITEMS.registerSimpleItem("straw", p -> p);
    public static final DeferredItem<Item> TWINE = ITEMS.registerSimpleItem("twine", p -> p);
    public static final DeferredItem<Item> FIBRE_CLOTH = ITEMS.registerSimpleItem("fibre_cloth", p -> p);

    // Knapped heads (tier 0-2 spec 3.2). Each carries knapped_from.
    public static final DeferredItem<Item> STONE_AXE_HEAD = ITEMS.registerSimpleItem("stone_axe_head", p -> p.stacksTo(16));
    public static final DeferredItem<Item> STONE_KNIFE_BLADE = ITEMS.registerSimpleItem("stone_knife_blade", p -> p.stacksTo(16));
    public static final DeferredItem<Item> STONE_SHOVEL_HEAD = ITEMS.registerSimpleItem("stone_shovel_head", p -> p.stacksTo(16));
    public static final DeferredItem<Item> STONE_HOE_HEAD = ITEMS.registerSimpleItem("stone_hoe_head", p -> p.stacksTo(16));
    public static final DeferredItem<Item> STONE_HAMMER_HEAD = ITEMS.registerSimpleItem("stone_hammer_head", p -> p.stacksTo(16));
    public static final DeferredItem<Item> STONE_SPEAR_HEAD = ITEMS.registerSimpleItem("stone_spear_head", p -> p.stacksTo(16));
    public static final DeferredItem<Item> STONE_PICKAXE_HEAD = ITEMS.registerSimpleItem("stone_pickaxe_head", p -> p.stacksTo(16));

    // Knapped tools (spec 3.4). Base durability 80, scaled by the rock at assembly. The stone spear is
    // the vanilla one, assembled from a knapped head.
    public static final int KNAPPED_DURABILITY = 80;
    public static final int KNAPPED_SPEAR_DURABILITY = 60;
    public static final DeferredItem<Item> STONE_AXE = ITEMS.registerSimpleItem("stone_axe",
            p -> p.axe(knapped(2.5f), 3.0f, -3.2f));
    public static final DeferredItem<Item> STONE_KNIFE = ITEMS.registerSimpleItem("stone_knife",
            p -> p.tool(knapped(4.0f), ModTags.Blocks.MINEABLE_WITH_KNIFE, 1.0f, -2.0f, 0.0f));
    public static final DeferredItem<Item> STONE_SHOVEL = ITEMS.registerSimpleItem("stone_shovel",
            p -> p.shovel(knapped(2.5f), 1.0f, -3.0f));
    public static final DeferredItem<Item> STONE_HOE = ITEMS.registerSimpleItem("stone_hoe",
            p -> p.hoe(knapped(2.5f), 0.0f, -2.0f));
    public static final DeferredItem<Item> STONE_HAMMER = ITEMS.registerSimpleItem("stone_hammer",
            p -> p.tool(knapped(1.0f), ModTags.Blocks.MINEABLE_WITH_HAMMER, 2.5f, -3.2f, 0.0f));
    public static final DeferredItem<Item> STONE_PICKAXE = ITEMS.registerSimpleItem("stone_pickaxe",
            p -> p.pickaxe(knapped(2.0f), 1.0f, -2.8f));

    // Fire (spec 3.5).
    public static final DeferredItem<FirestarterItem> FIRESTARTER = ITEMS.registerItem("firestarter", FirestarterItem::new,
            p -> p.durability(10));
    public static final DeferredItem<BlockItem> FIRE_PIT = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_PIT);

    // Clay (spec 4.1 to 4.3). Unfired pieces are formed on the grid and fired in a pit kiln.
    public static final DeferredItem<Item> UNFIRED_SMALL_VESSEL = ITEMS.registerSimpleItem("unfired_small_vessel", p -> p.stacksTo(1));
    public static final DeferredItem<Item> UNFIRED_LARGE_VESSEL = ITEMS.registerSimpleItem("unfired_large_vessel", p -> p.stacksTo(1));
    public static final DeferredItem<Item> UNFIRED_CRUCIBLE = ITEMS.registerSimpleItem("unfired_crucible", p -> p.stacksTo(1));
    public static final DeferredItem<Item> UNFIRED_INGOT_MOLD = ITEMS.registerSimpleItem("unfired_ingot_mold", p -> p.stacksTo(16));
    public static final DeferredItem<Item> UNFIRED_BRICK = ITEMS.registerSimpleItem("unfired_brick");
    public static final DeferredItem<SmallVesselItem> SMALL_VESSEL = ITEMS.registerItem("small_vessel", SmallVesselItem::new,
            p -> p.stacksTo(1));
    public static final DeferredItem<LargeVesselItem> LARGE_VESSEL = ITEMS.registerItem("large_vessel",
            p -> new LargeVesselItem(ModBlocks.LARGE_VESSEL.get(), p), p -> p.stacksTo(1).useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> CRUCIBLE = ITEMS.registerSimpleBlockItem(ModBlocks.CRUCIBLE, p -> p.stacksTo(1));
    public static final DeferredItem<Item> INGOT_MOLD = ITEMS.registerSimpleItem("ingot_mold", p -> p.stacksTo(16));
    public static final Map<MoldType, DeferredItem<Item>> UNFIRED_MOLDS = new EnumMap<>(MoldType.class);
    public static final Map<MoldType, DeferredItem<Item>> MOLDS = new EnumMap<>(MoldType.class);

    public static final Map<Rock, DeferredItem<BlockItem>> RAW_ROCK = new EnumMap<>(Rock.class);
    public static final Map<Rock, DeferredItem<BlockItem>> COBBLED_ROCK = new EnumMap<>(Rock.class);
    /** The loose rock: picked up from the ground, knapped into tool heads. */
    public static final Map<Rock, DeferredItem<GroundCoverItem>> LOOSE_ROCK = new EnumMap<>(Rock.class);
    public static final Map<Rock, Map<OreMineral, DeferredItem<BlockItem>>> ORE_BLOCKS = new EnumMap<>(Rock.class);
    public static final Map<OreMineral, DeferredItem<GroundCoverItem>> SMALL_ORES = new EnumMap<>(OreMineral.class);
    public static final Map<OreMineral, Map<OreGrade, DeferredItem<Item>>> ORE_PIECES = new EnumMap<>(OreMineral.class);
    public static final Map<OreMineral, Map<OreGrade, DeferredItem<Item>>> CRUSHED_ORES = new EnumMap<>(OreMineral.class);

    static {
        for (MoldType type : MoldType.values()) {
            UNFIRED_MOLDS.put(type, ITEMS.registerSimpleItem("unfired_" + type.id() + "_mold", p -> p.stacksTo(16)));
            MOLDS.put(type, ITEMS.registerSimpleItem(type.id() + "_mold", p -> p.stacksTo(16)));
        }
        for (Rock rock : Rock.values()) {
            RAW_ROCK.put(rock, ITEMS.registerSimpleBlockItem(ModBlocks.RAW_ROCK.get(rock)));
            COBBLED_ROCK.put(rock, ITEMS.registerSimpleBlockItem(ModBlocks.COBBLED_ROCK.get(rock)));
            LOOSE_ROCK.put(rock, ITEMS.registerItem("loose_" + rock.id(),
                    p -> new GroundCoverItem(ModBlocks.LOOSE_ROCK.get(rock).get(), p), p -> p.useBlockDescriptionPrefix()));
            Map<OreMineral, DeferredItem<BlockItem>> ores = new EnumMap<>(OreMineral.class);
            for (OreMineral mineral : OreMineral.values()) {
                ores.put(mineral, ITEMS.registerSimpleBlockItem(ModBlocks.ORES.get(rock).get(mineral)));
            }
            ORE_BLOCKS.put(rock, ores);
        }
        for (OreMineral mineral : OreMineral.values()) {
            SMALL_ORES.put(mineral, ITEMS.registerItem("small_" + mineral.id(),
                    p -> new GroundCoverItem(ModBlocks.SMALL_ORES.get(mineral).get(), p), p -> p.useBlockDescriptionPrefix()));
            Map<OreGrade, DeferredItem<Item>> pieces = new EnumMap<>(OreGrade.class);
            Map<OreGrade, DeferredItem<Item>> crushed = new EnumMap<>(OreGrade.class);
            for (OreGrade grade : OreGrade.values()) {
                pieces.put(grade, ITEMS.registerSimpleItem(grade.prefix() + mineral.id()));
                crushed.put(grade, ITEMS.registerSimpleItem("crushed_" + grade.prefix() + mineral.id()));
            }
            ORE_PIECES.put(mineral, pieces);
            CRUSHED_ORES.put(mineral, crushed);
        }
    }

    /** Knapped stone: stone mining tier, 80 base durability, repaired with loose rocks. */
    private static ToolMaterial knapped(float speed) {
        return new ToolMaterial(ModTags.Blocks.INCORRECT_FOR_STONE_TOOL, KNAPPED_DURABILITY, speed, 0.0f, 5, ModTags.Items.LOOSE_ROCKS);
    }

    public static Item orePiece(OreMineral mineral, OreGrade grade) {
        return ORE_PIECES.get(mineral).get(grade).get();
    }

    public static Item crushedOre(OreMineral mineral, OreGrade grade) {
        return CRUSHED_ORES.get(mineral).get(grade).get();
    }

    private ModItems() {}
}
