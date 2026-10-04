package dev.strataindustria.cabinet;

import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.structure.MineralSpecimenItem;
import dev.strataindustria.structure.StructureContent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What the specimen cabinet (uniqueness 9.5) takes and how its shelves complete. A specimen is a rock shard or a
 * mineral, as a cut specimen or a raw ore piece. Each rock category is a shelf of its own and all the minerals
 * together are one more. A specimen is named {@code rock:basalt} or {@code mineral:cassiterite}.
 */
public final class Specimens {
    public static final String MINERAL_SHELF = "minerals";

    private Specimens() {}

    /** The specimen this stack counts as, or null if the cabinet does not take it. */
    public static @Nullable String idOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Rock rock : Rock.values()) {
            if (stack.is(ModItems.ROCK_SHARD.get(rock).get())) return "rock:" + rock.id();
        }
        if (stack.is(StructureContent.MINERAL_SPECIMEN.get())) {
            String mineral = MineralSpecimenItem.mineral(stack);
            return mineral != null && required(MINERAL_SHELF).contains("mineral:" + mineral) ? "mineral:" + mineral : null;
        }
        for (OreMineral mineral : OreMineral.withPieces()) {
            for (var piece : ModItems.ORE_PIECES.get(mineral).values()) {
                if (stack.is(piece.get())) return "mineral:" + mineral.id();
            }
        }
        return null;
    }

    /** Every shelf: one per rock category, then the minerals. */
    public static List<String> shelves() {
        List<String> shelves = new ArrayList<>();
        for (RockCategory category : RockCategory.values()) shelves.add(category.getSerializedName());
        shelves.add(MINERAL_SHELF);
        return shelves;
    }

    /** The specimens a shelf wants. */
    public static Set<String> required(String shelf) {
        Set<String> wanted = new LinkedHashSet<>();
        if (shelf.equals(MINERAL_SHELF)) {
            for (OreMineral mineral : OreMineral.withPieces()) wanted.add("mineral:" + mineral.id());
        } else {
            for (Rock rock : Rock.values()) if (rock.category().getSerializedName().equals(shelf)) wanted.add("rock:" + rock.id());
        }
        return wanted;
    }

    /** The shelf a specimen sits on. */
    public static String shelfOf(String specimen) {
        if (specimen.startsWith("mineral:")) return MINERAL_SHELF;
        return Rock.valueOf(specimen.substring("rock:".length()).toUpperCase(java.util.Locale.ROOT)).category().getSerializedName();
    }

    /** Whether {@code held} holds everything the shelf wants. */
    public static boolean complete(String shelf, Set<String> held) {
        return held.containsAll(required(shelf));
    }

    /** Every specimen the cabinet can hold. */
    public static int total() {
        int total = 0;
        for (String shelf : shelves()) total += required(shelf).size();
        return total;
    }
}
