package dev.strataindustria.registry;


import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.charcoal.AshItem;
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
    public static final DeferredItem<BlockItem> FIRE_PIT = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_PIT);

    // Clay (spec 4.1 to 4.3). Unfired pieces are shaped by hand and fired on the fire pit.
    public static final DeferredItem<Item> UNFIRED_CRUCIBLE = ITEMS.registerSimpleItem("unfired_crucible", p -> p.stacksTo(1));
    public static final DeferredItem<Item> UNFIRED_INGOT_MOLD = ITEMS.registerSimpleItem("unfired_ingot_mold", p -> p.stacksTo(16));
    public static final DeferredItem<Item> UNFIRED_BRICK = ITEMS.registerSimpleItem("unfired_brick");
    public static final DeferredItem<BlockItem> CRUCIBLE = ITEMS.registerSimpleBlockItem(ModBlocks.CRUCIBLE, p -> p.stacksTo(1));
    public static final DeferredItem<CastMoldItem> INGOT_MOLD = ITEMS.registerItem("ingot_mold", p -> new CastMoldItem(null, p),
            p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> FORGE = ITEMS.registerSimpleBlockItem(ModBlocks.FORGE);
    // Smithing (spec 9).
    public static final DeferredItem<BlockItem> IRON_ANVIL = ITEMS.registerSimpleBlockItem(ModBlocks.IRON_ANVIL);
    /** Tier 3 spec 9.4: welding flux, ground from sand or carbonate rock. */
    public static final DeferredItem<Item> FLUX = ITEMS.registerSimpleItem("flux");
    static {
        // Smithing patterns are gone (hammer machines have a shape button); old ones in a world turn into paper.
        ITEMS.addAlias(StrataIndustria.id("smithing_pattern"), net.minecraft.resources.Identifier.withDefaultNamespace("paper"));
        for (String old : new String[] {"bronze_anvil", "wrought_iron_anvil", "steel_anvil"}) {
            ITEMS.addAlias(StrataIndustria.id(old), StrataIndustria.id("iron_anvil"));
        }
    }
    public static final DeferredItem<Item> TONGS_JAW = ITEMS.registerSimpleItem("tongs_jaw", p -> p.stacksTo(16));
    public static final DeferredItem<Item> TONGS = ITEMS.registerSimpleItem("tongs", p -> p.durability(250));
    // Tier 3 spec 7 and 8: mechanical power and machines.
    public static final DeferredItem<BlockItem> WOODEN_AXLE = ITEMS.registerSimpleBlockItem(ModBlocks.WOODEN_AXLE);
    public static final DeferredItem<Item> WOODEN_GEAR = ITEMS.registerSimpleItem("wooden_gear");
    public static final DeferredItem<BlockItem> WOODEN_GEARBOX = ITEMS.registerSimpleBlockItem(ModBlocks.WOODEN_GEARBOX);
    public static final DeferredItem<BlockItem> HAND_CRANK = ITEMS.registerSimpleBlockItem(ModBlocks.HAND_CRANK);
    public static final DeferredItem<BlockItem> WATER_WHEEL = ITEMS.registerSimpleBlockItem(ModBlocks.WATER_WHEEL);
    public static final DeferredItem<BlockItem> MILLSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.MILLSTONE);
    public static final DeferredItem<BlockItem> BELLOWS = ITEMS.registerSimpleBlockItem(ModBlocks.BELLOWS);
    public static final DeferredItem<BlockItem> SAW_MILL = ITEMS.registerSimpleBlockItem(ModBlocks.SAW_MILL);
    public static final DeferredItem<BlockItem> TRIP_HAMMER = ITEMS.registerSimpleBlockItem(ModBlocks.TRIP_HAMMER);
    public static final DeferredItem<BlockItem> CORE_SAMPLER = ITEMS.registerSimpleBlockItem(ModBlocks.CORE_SAMPLER);
    /** Tier 3 spec 8.5: a drilled core; use it to read the ground below the sampler. */
    public static final DeferredItem<dev.strataindustria.prospecting.CoreSampleItem> CORE_SAMPLE = ITEMS.registerItem("core_sample",
            dev.strataindustria.prospecting.CoreSampleItem::new, p -> p.stacksTo(1));
    // Tier 3 spec 7.2 and 7.3.
    public static final DeferredItem<BlockItem> STEP_UP_GEARBOX = ITEMS.registerSimpleBlockItem(ModBlocks.STEP_UP_GEARBOX);
    public static final DeferredItem<BlockItem> PULLEY = ITEMS.registerSimpleBlockItem(ModBlocks.PULLEY);
    public static final DeferredItem<BlockItem> WINDMILL_BEARING = ITEMS.registerSimpleBlockItem(ModBlocks.WINDMILL_BEARING);
    public static final DeferredItem<BlockItem> WINDMILL_SAIL = ITEMS.registerSimpleBlockItem(ModBlocks.WINDMILL_SAIL);
    public static final DeferredItem<dev.strataindustria.power.LeatherBeltItem> LEATHER_BELT = ITEMS.registerItem("leather_belt",
            dev.strataindustria.power.LeatherBeltItem::new, p -> p.stacksTo(16));
    // Tier 3 spec 12.1: hides and the soaking barrel.
    public static final DeferredItem<Item> RAW_HIDE = ITEMS.registerSimpleItem("raw_hide");
    public static final DeferredItem<Item> LIMED_HIDE = ITEMS.registerSimpleItem("limed_hide");
    public static final DeferredItem<Item> SCRAPED_HIDE = ITEMS.registerSimpleItem("scraped_hide");
    public static final DeferredItem<BlockItem> SOAKING_BARREL = ITEMS.registerSimpleBlockItem(ModBlocks.SOAKING_BARREL);
    // Tier 3 spec 11: washing.
    public static final DeferredItem<dev.strataindustria.washing.WashingPanItem> WASHING_PAN = ITEMS.registerItem("washing_pan",
            dev.strataindustria.washing.WashingPanItem::new, p -> p.durability(128));
    public static final DeferredItem<BlockItem> SLUICE = ITEMS.registerSimpleBlockItem(ModBlocks.SLUICE);
    /** Tier 3 spec 12.4: from the saw mill; tannin later in tier 3, and a weak fuel meanwhile. */
    public static final DeferredItem<Item> BARK = ITEMS.registerSimpleItem("bark");
    // Quern (spec 10.1).
    public static final DeferredItem<Item> QUERNSTONE = ITEMS.registerSimpleItem("quernstone", p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> QUERN = ITEMS.registerSimpleBlockItem(ModBlocks.QUERN);
    // Charcoal (spec 4.4).
    public static final DeferredItem<AshItem> ASH = ITEMS.registerItem("ash", AshItem::new);
    // Tier 3: fire clay and fire bricks (tier 3 spec 3).
    public static final DeferredItem<Item> FIRE_CLAY_BALL = ITEMS.registerSimpleItem("fire_clay_ball");
    public static final DeferredItem<Item> GROG = ITEMS.registerSimpleItem("grog");
    public static final DeferredItem<Item> UNFIRED_FIRE_BRICK = ITEMS.registerSimpleItem("unfired_fire_brick");
    public static final DeferredItem<Item> FIRE_BRICK = ITEMS.registerSimpleItem("fire_brick");
    public static final DeferredItem<BlockItem> FIRE_CLAY = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_CLAY);
    public static final DeferredItem<BlockItem> FIRE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICKS);
    public static final DeferredItem<BlockItem> FIRE_BRICK_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> FIRE_BRICK_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> FIRE_BRICK_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_BRICK_WALL);
    public static final DeferredItem<BlockItem> BLOOMERY = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOMERY);
    /** Spec 5.4: a spongy lump of iron and slag, straight from the bloomery. */
    public static final DeferredItem<Item> RAW_BLOOM = ITEMS.registerSimpleItem("raw_bloom", p -> p.stacksTo(1));
    public static final DeferredItem<Item> BLOOMERY_SLAG = ITEMS.registerSimpleItem("bloomery_slag");
    // Tier 3 deposits (spec 4.3).
    public static final DeferredItem<Item> LIGNITE = ITEMS.registerSimpleItem("lignite");
    public static final DeferredItem<Item> BAUXITE = ITEMS.registerSimpleItem("bauxite");
    public static final DeferredItem<BlockItem> BAUXITE_BED = ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_BED);
    public static final DeferredItem<BlockItem> LIGNITE_SEAM = ITEMS.registerSimpleBlockItem(ModBlocks.LIGNITE_SEAM);
    public static final DeferredItem<BlockItem> BOG_IRON = ITEMS.registerSimpleBlockItem(ModBlocks.BOG_IRON);
    public static final DeferredItem<BlockItem> PLACER_GRAVEL = ITEMS.registerSimpleBlockItem(ModBlocks.PLACER_GRAVEL);
    public static final DeferredItem<BlockItem> PLACER_SAND = ITEMS.registerSimpleBlockItem(ModBlocks.PLACER_SAND);
    // Wrought iron forms beyond the vanilla ingot and nugget (spec 4.1).
    public static final DeferredItem<Item> WROUGHT_IRON_ROD = ITEMS.registerSimpleItem("wrought_iron_rod");
    public static final DeferredItem<Item> WROUGHT_IRON_DOUBLE_INGOT = ITEMS.registerSimpleItem("wrought_iron_double_ingot", p -> p.stacksTo(16));
    /** Tier 4 spec 14.1: welded from two steel ingots; the steel sword blade. */
    public static final DeferredItem<Item> STEEL_DOUBLE_INGOT = ITEMS.registerSimpleItem("steel_double_ingot", p -> p.stacksTo(16));
    // Metals (spec 6 to 8). Copper's ingot, nugget, armour and five of its tools are vanilla items.
    public static final Map<Metal, Supplier<Item>> INGOTS = new EnumMap<>(Metal.class);
    public static final Map<Metal, Supplier<Item>> NUGGETS = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> PLATES = new EnumMap<>(Metal.class);
    /** Tier 4 spec 4.1: rods (half an ingot) and gears (one ingot) of steel and brass; gears of the bronzes too. */
    public static final Map<Metal, DeferredItem<Item>> RODS = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> GEARS = new EnumMap<>(Metal.class);
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
    /** Tier 3 spec 11.2: crushed ore washed clean, worth a tenth more. */
    public static final Map<OreMineral, Map<OreGrade, DeferredItem<Item>>> WASHED_ORES = new EnumMap<>(OreMineral.class);

    static {
        for (Metal metal : Metal.values()) {
            if (!metal.hasIngot()) continue;
            if (metal.isVanilla()) {
                Item ingot = vanillaIngot(metal), nugget = vanillaNugget(metal);
                INGOTS.put(metal, () -> ingot);
                NUGGETS.put(metal, () -> nugget);
            } else {
                INGOTS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_ingot"));
                if (metal.hasNugget()) NUGGETS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_nugget"));
            }
            if (metal.hasPlate()) PLATES.put(metal, ITEMS.registerSimpleItem(metal.id() + "_plate"));
            if (metal.hasRod()) RODS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_rod"));
            if (metal.hasGear()) GEARS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_gear"));
            if (!metal.isToolMetal()) continue;
            Map<MoldType, DeferredItem<Item>> heads = new EnumMap<>(MoldType.class);
            Map<MoldType, Supplier<Item>> tools = new EnumMap<>(MoldType.class);
            for (MoldType type : metal.toolTypes()) {
                heads.put(type, ITEMS.registerSimpleItem(metal.id() + "_" + type.id(), p -> p.stacksTo(16)));
                Item vanilla = metal.isVanilla() ? vanillaTool(metal, type) : null;
                if (vanilla != null) tools.put(type, () -> vanilla);
                else tools.put(type, ITEMS.registerSimpleItem(metal.id() + "_" + type.tool(), p -> metalTool(p, metal, type)));
            }
            HEADS.put(metal, heads);
            Map<ArmorType, Supplier<Item>> armour = new EnumMap<>(ArmorType.class);
            for (ArmorType type : ARMOUR_TYPES) {
                if (metal.isVanilla()) {
                    Item vanilla = vanillaArmour(metal, type);
                    armour.put(type, () -> vanilla);
                } else {
                    armour.put(type, ITEMS.registerSimpleItem(metal.id() + "_" + type.getName(),
                            p -> p.humanoidArmor(ModArmorMaterials.of(metal), type)));
                }
            }
            ARMOUR.put(metal, armour);
            if (metal.hasProspectorsPick()) {
                PROSPECTOR_HEADS.put(metal, ITEMS.registerSimpleItem(metal.id() + "_prospectors_pick_head", p -> p.stacksTo(16)));
                ToolMaterial material = ModToolMaterials.of(metal);
                // Mines like a pickaxe, but slowly: it is for listening to the rock, not breaking it.
                ToolMaterial slow = new ToolMaterial(material.incorrectBlocksForDrops(), material.durability(), 3.0f,
                        material.attackDamageBonus(), material.enchantmentValue(), material.repairItems());
                // Tier 3 spec 10.3: the wrought iron pick listens further.
                int radius = metal == Metal.WROUGHT_IRON ? ProspectorsPickItem.WROUGHT_IRON_RADIUS : ProspectorsPickItem.RADIUS;
                PROSPECTORS_PICKS.put(metal, ITEMS.registerItem(metal.id() + "_prospectors_pick", p -> new ProspectorsPickItem(radius, p),
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
            for (OreMineral mineral : OreMineral.inRockValues()) {
                ores.put(mineral, ITEMS.registerSimpleBlockItem(ModBlocks.ORES.get(rock).get(mineral)));
            }
            ORE_BLOCKS.put(rock, ores);
        }
        // Coal and sulfur indicators drop their item, so only metal ores have a small ore item (tier 4 spec 4.4).
        for (OreMineral mineral : OreMineral.withPieces()) {
            SMALL_ORES.put(mineral, ITEMS.registerItem("small_" + mineral.id(),
                    p -> new GroundCoverItem(ModBlocks.SMALL_ORES.get(mineral).get(), p), p -> p.useBlockDescriptionPrefix()));
            Map<OreGrade, DeferredItem<Item>> pieces = new EnumMap<>(OreGrade.class);
            Map<OreGrade, DeferredItem<Item>> crushed = new EnumMap<>(OreGrade.class);
            for (OreGrade grade : OreGrade.values()) {
                pieces.put(grade, ITEMS.registerSimpleItem(grade.prefix() + mineral.id()));
                crushed.put(grade, ITEMS.registerSimpleItem("crushed_" + grade.prefix() + mineral.id()));
            }
            if (mineral.washable()) {
                Map<OreGrade, DeferredItem<Item>> washed = new EnumMap<>(OreGrade.class);
                for (OreGrade grade : OreGrade.values()) {
                    washed.put(grade, ITEMS.registerSimpleItem("washed_" + grade.prefix() + mineral.id()));
                }
                WASHED_ORES.put(mineral, washed);
            }
            ORE_PIECES.put(mineral, pieces);
            CRUSHED_ORES.put(mineral, crushed);
        }
    }

    public static ArmorType[] armourTypes() {
        return ARMOUR_TYPES.clone();
    }

    private static Item vanillaIngot(Metal metal) {
        return switch (metal) {
            case WROUGHT_IRON -> Items.IRON_INGOT;
            case GOLD -> Items.GOLD_INGOT;
            default -> Items.COPPER_INGOT;
        };
    }

    private static Item vanillaNugget(Metal metal) {
        return switch (metal) {
            case WROUGHT_IRON -> Items.IRON_NUGGET;
            case GOLD -> Items.GOLD_NUGGET;
            default -> Items.COPPER_NUGGET;
        };
    }

    private static Item vanillaArmour(Metal metal, ArmorType type) {
        return switch (metal) {
            case WROUGHT_IRON -> switch (type) {
                case HELMET -> Items.IRON_HELMET;
                case CHESTPLATE -> Items.IRON_CHESTPLATE;
                case LEGGINGS -> Items.IRON_LEGGINGS;
                default -> Items.IRON_BOOTS;
            };
            case GOLD -> switch (type) {
                case HELMET -> Items.GOLDEN_HELMET;
                case CHESTPLATE -> Items.GOLDEN_CHESTPLATE;
                case LEGGINGS -> Items.GOLDEN_LEGGINGS;
                default -> Items.GOLDEN_BOOTS;
            };
            default -> switch (type) {
                case HELMET -> Items.COPPER_HELMET;
                case CHESTPLATE -> Items.COPPER_CHESTPLATE;
                case LEGGINGS -> Items.COPPER_LEGGINGS;
                default -> Items.COPPER_BOOTS;
            };
        };
    }

    /** The vanilla tool a head of this metal goes on, or null when the mod adds its own (knife, hammer, saw). */
    public static Item vanillaTool(Metal metal, MoldType type) {
        if (!metal.isVanilla()) return null;
        return switch (metal) {
            case WROUGHT_IRON -> switch (type) {
                case PICKAXE_HEAD -> Items.IRON_PICKAXE;
                case AXE_HEAD -> Items.IRON_AXE;
                case SHOVEL_HEAD -> Items.IRON_SHOVEL;
                case HOE_HEAD -> Items.IRON_HOE;
                case SWORD_BLADE -> Items.IRON_SWORD;
                default -> null;
            };
            case GOLD -> switch (type) {
                case PICKAXE_HEAD -> Items.GOLDEN_PICKAXE;
                case AXE_HEAD -> Items.GOLDEN_AXE;
                case SHOVEL_HEAD -> Items.GOLDEN_SHOVEL;
                case HOE_HEAD -> Items.GOLDEN_HOE;
                case SWORD_BLADE -> Items.GOLDEN_SWORD;
                default -> null;
            };
            default -> switch (type) {
                case PICKAXE_HEAD -> Items.COPPER_PICKAXE;
                case AXE_HEAD -> Items.COPPER_AXE;
                case SHOVEL_HEAD -> Items.COPPER_SHOVEL;
                case HOE_HEAD -> Items.COPPER_HOE;
                case SWORD_BLADE -> Items.COPPER_SWORD;
                default -> null;
            };
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

    public static Item washedOre(OreMineral mineral, OreGrade grade) {
        return WASHED_ORES.get(mineral).get(grade).get();
    }

    private ModItems() {}
}
