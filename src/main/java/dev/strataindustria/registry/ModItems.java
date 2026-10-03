package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.item.GroundCoverItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StrataIndustria.MOD_ID);

    public static final DeferredItem<Item> PLANT_FIBRE = ITEMS.registerSimpleItem("plant_fibre", p -> p);

    public static final Map<Rock, DeferredItem<BlockItem>> RAW_ROCK = new EnumMap<>(Rock.class);
    public static final Map<Rock, DeferredItem<BlockItem>> COBBLED_ROCK = new EnumMap<>(Rock.class);
    /** The loose rock: picked up from the ground, knapped into tool heads. */
    public static final Map<Rock, DeferredItem<GroundCoverItem>> LOOSE_ROCK = new EnumMap<>(Rock.class);
    public static final Map<Rock, Map<OreMineral, DeferredItem<BlockItem>>> ORE_BLOCKS = new EnumMap<>(Rock.class);
    public static final Map<OreMineral, DeferredItem<GroundCoverItem>> SMALL_ORES = new EnumMap<>(OreMineral.class);
    public static final Map<OreMineral, Map<OreGrade, DeferredItem<Item>>> ORE_PIECES = new EnumMap<>(OreMineral.class);
    public static final Map<OreMineral, Map<OreGrade, DeferredItem<Item>>> CRUSHED_ORES = new EnumMap<>(OreMineral.class);

    static {
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

    public static Item orePiece(OreMineral mineral, OreGrade grade) {
        return ORE_PIECES.get(mineral).get(grade).get();
    }

    public static Item crushedOre(OreMineral mineral, OreGrade grade) {
        return CRUSHED_ORES.get(mineral).get(grade).get();
    }

    private ModItems() {}
}
