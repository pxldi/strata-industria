package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.LargeVesselItem;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.ceramics.SmallVesselItem;
import dev.strataindustria.charcoal.AshItem;
import dev.strataindustria.fire.FirestarterItem;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.item.GroundCoverItem;
import dev.strataindustria.item.ProspectorsPickItem;
import dev.strataindustria.journal.FieldJournalItem;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.metal.ModArmorMaterials;
import dev.strataindustria.metal.ModToolMaterials;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StrataIndustria.MOD_ID);

    public static final DeferredItem<Item> PLANT_FIBRE = ITEMS.registerSimpleItem("plant_fibre", p -> p);
    public static final DeferredItem<Item> STRAW = ITEMS.registerSimpleItem("straw", p -> p);
    public static final DeferredItem<Item> TWINE = ITEMS.registerSimpleItem("twine", p -> p);
    public static final DeferredItem<Item> FIBRE_CLOTH = ITEMS.registerSimpleItem("fibre_cloth", p -> p);
    /** Spec 3.6: opens the journal tab of the advancements screen. */
    public static final DeferredItem<FieldJournalItem> FIELD_JOURNAL = ITEMS.registerItem("field_journal", FieldJournalItem::new,
            p -> p.stacksTo(1));

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
    public static final DeferredItem<CastMoldItem> INGOT_MOLD = ITEMS.registerItem("ingot_mold", p -> new CastMoldItem(null, p),
            p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> FORGE = ITEMS.registerSimpleBlockItem(ModBlocks.FORGE);
    // Smithing (spec 9).
    public static final DeferredItem<BlockItem> BRONZE_ANVIL = ITEMS.registerSimpleBlockItem(ModBlocks.BRONZE_ANVIL);
    public static final DeferredItem<Item> TONGS_JAW = ITEMS.registerSimpleItem("tongs_jaw", p -> p.stacksTo(16));
    public static final DeferredItem<Item> TONGS = ITEMS.registerSimpleItem("tongs", p -> p.durability(250));
    // Quern (spec 10.1).
    public static final DeferredItem<Item> QUERNSTONE = ITEMS.registerSimpleItem("quernstone", p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> QUERN = ITEMS.registerSimpleBlockItem(ModBlocks.QUERN);
    // Charcoal (spec 4.4).
    public static final DeferredItem<AshItem> ASH = ITEMS.registerItem("ash", AshItem::new);
    // Metals (spec 6 to 8). Copper's ingot, nugget, armour and five of its tools are vanilla items.
    public static final Map<Metal, Supplier<Item>> INGOTS = new EnumMap<>(Metal.class);
    public static final Map<Metal, Supplier<Item>> NUGGETS = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> PLATES = new EnumMap<>(Metal.class);
    /** Cast heads and blades, by metal and the mold that casts them. */
    public static final Map<Metal, Map<MoldType, DeferredItem<Item>>> HEADS = new EnumMap<>(Metal.class);
    public static final Map<Metal, Map<MoldType, Supplier<Item>>> TOOLS = new EnumMap<>(Metal.class);
    private static final ArmorType[] ARMOUR_TYPES = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
    /** Armour by metal and piece (spec 8.4); copper's is vanilla. */
    public static final Map<Metal, Map<ArmorType, Supplier<Item>>> ARMOUR = new EnumMap<>(Metal.class);
    /** Prospector's picks and their heads, bronzes only (spec 10.2). */
    public static final Map<Metal, DeferredItem<Item>> PROSPECTOR_HEADS = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<ProspectorsPickItem>> PROSPECTORS_PICKS = new EnumMap<>(Metal.class);
    public static final Map<MoldType, DeferredItem<Item>> UNFIRED_MOLDS = new EnumMap<>(MoldType.class);
    public static final Map<MoldType, DeferredItem<CastMoldItem>> MOLDS = new EnumMap<>(MoldType.class);

    public static final Map<Rock, DeferredItem<BlockItem>> RAW_ROCK = new EnumMap<>(Rock.class);
    public static final Map<Rock, DeferredItem<BlockItem>> COBBLED_ROCK = new EnumMap<>(Rock.class);
    /** The loose rock: picked up from the ground, knapped into tool heads. */
    public static final Map<Rock, DeferredItem<GroundCoverItem>> LOOSE_ROCK = new EnumMap<>(Rock.class);
    public static final Map<Rock, Map<OreMineral, DeferredItem<BlockItem>>> ORE_BLOCKS = new EnumMap<>(Rock.class);
    public static final Map<OreMineral, DeferredItem<GroundCoverItem>> SMALL_ORES = new EnumMap<>(OreMineral.class);
    public static final Map<OreMineral, Map<OreGrade, DeferredItem<Item>>> ORE_PIECES = new EnumMap<>(OreMineral.class);
    public static final Map<OreMineral, Map<OreGrade, DeferredItem<Item>>> CRUSHED_ORES = new EnumMap<>(OreMineral.class);

    static {
        for (Metal metal : Metal.values()) {
            if (!metal.hasIngot()) continue;
            if (metal.isVanilla()) {
                INGOTS.put(metal, () -> Items.COPPER_INGOT);
                NUGGETS.put(metal, () -> Items.COPPER_NUGGET);
            } else {
                INGOTS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_ingot"));
                if (metal.hasNugget()) NUGGETS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_nugget"));
            }
            if (!metal.isToolMetal()) continue;
            PLATES.put(metal, ITEMS.registerSimpleItem(metal.id() + "_plate"));
            Map<MoldType, DeferredItem<Item>> heads = new EnumMap<>(MoldType.class);
            Map<MoldType, Supplier<Item>> tools = new EnumMap<>(MoldType.class);
            for (MoldType type : MoldType.values()) {
                heads.put(type, ITEMS.registerSimpleItem(metal.id() + "_" + type.id(), p -> p.stacksTo(16)));
                Item vanilla = metal.isVanilla() ? vanillaCopperTool(type) : null;
                if (vanilla != null) tools.put(type, () -> vanilla);
                else tools.put(type, ITEMS.registerSimpleItem(metal.id() + "_" + type.tool(), p -> metalTool(p, metal, type)));
            }
            HEADS.put(metal, heads);
            Map<ArmorType, Supplier<Item>> armour = new EnumMap<>(ArmorType.class);
            for (ArmorType type : ARMOUR_TYPES) {
                if (metal.isVanilla()) {
                    Item vanilla = vanillaCopperArmour(type);
                    armour.put(type, () -> vanilla);
                } else {
                    armour.put(type, ITEMS.registerSimpleItem(metal.id() + "_" + type.getName(),
                            p -> p.humanoidArmor(ModArmorMaterials.of(metal), type)));
                }
            }
            ARMOUR.put(metal, armour);
            if (metal.isBronze()) {
                PROSPECTOR_HEADS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_prospectors_pick_head", p -> p.stacksTo(16)));
                ToolMaterial material = ModToolMaterials.of(metal);
                // Mines like a pickaxe, but slowly: it is for listening to the rock, not breaking it.
                ToolMaterial slow = new ToolMaterial(material.incorrectBlocksForDrops(), material.durability(), 3.0f,
                        material.attackDamageBonus(), material.enchantmentValue(), material.repairItems());
                PROSPECTORS_PICKS.put(metal, ITEMS.registerItem(metal.id() + "_prospectors_pick", ProspectorsPickItem::new,
                        p -> p.pickaxe(slow, 1.0f, -2.8f)));
            }
            TOOLS.put(metal, tools);
        }
        for (MoldType type : MoldType.values()) {
            UNFIRED_MOLDS.put(type, ITEMS.registerSimpleItem("unfired_" + type.id() + "_mold", p -> p.stacksTo(16)));
            MOLDS.put(type, ITEMS.registerItem(type.id() + "_mold", p -> new CastMoldItem(type, p), p -> p.stacksTo(16)));
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

    public static ArmorType[] armourTypes() {
        return ARMOUR_TYPES.clone();
    }

    private static Item vanillaCopperArmour(ArmorType type) {
        return switch (type) {
            case HELMET -> Items.COPPER_HELMET;
            case CHESTPLATE -> Items.COPPER_CHESTPLATE;
            case LEGGINGS -> Items.COPPER_LEGGINGS;
            default -> Items.COPPER_BOOTS;
        };
    }

    private static Item vanillaCopperTool(MoldType type) {
        return switch (type) {
            case PICKAXE_HEAD -> Items.COPPER_PICKAXE;
            case AXE_HEAD -> Items.COPPER_AXE;
            case SHOVEL_HEAD -> Items.COPPER_SHOVEL;
            case HOE_HEAD -> Items.COPPER_HOE;
            case SWORD_BLADE -> Items.COPPER_SWORD;
            default -> null;
        };
    }

    /** Spec 8.3: attack damage and speed per tool, on top of the material's bonus. */
    private static Item.Properties metalTool(Item.Properties p, Metal metal, MoldType type) {
        ToolMaterial material = ModToolMaterials.of(metal);
        return switch (type) {
            case PICKAXE_HEAD -> p.pickaxe(material, 1.0f, -2.8f);
            case AXE_HEAD -> p.axe(material, 6.0f, -3.1f);
            case SHOVEL_HEAD -> p.shovel(material, 1.5f, -3.0f);
            case HOE_HEAD -> p.hoe(material, 0.0f, -2.0f);
            case KNIFE_BLADE -> p.tool(material, ModTags.Blocks.MINEABLE_WITH_KNIFE, 1.5f, -2.0f, 0.0f);
            case HAMMER_HEAD -> p.tool(material, ModTags.Blocks.MINEABLE_WITH_HAMMER, 3.0f, -3.2f, 0.0f);
            case SAW_BLADE -> p.tool(material, BlockTags.MINEABLE_WITH_AXE, 1.0f, -2.8f, 0.0f);
            case SWORD_BLADE -> p.sword(material, 3.0f, -2.4f);
        };
    }

    public static Item ingot(Metal metal) {
        return INGOTS.get(metal).get();
    }

    public static Item head(Metal metal, MoldType type) {
        return HEADS.get(metal).get(type).get();
    }

    public static Item tool(Metal metal, MoldType type) {
        return TOOLS.get(metal).get(type).get();
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
