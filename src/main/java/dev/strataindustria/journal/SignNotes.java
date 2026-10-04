package dev.strataindustria.journal;

import dev.strataindustria.block.BoulderBlock;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.signs.Stain;
import dev.strataindustria.signs.StainBlock;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Notes about the colours the ground takes over ore (redesign R7): rust, green streaks, a yellow crust and
 * black sand. The first one of a kind the player walks past, or rubs, is written down and opens the lead for
 * that ore early. Text under {@code journal.strataindustria.observe.sign.*}.
 */
public final class SignNotes {
    static final String BLACK_SAND = "black_sand";

    private SignNotes() {}

    /** Called for each block near the player by {@link PlantNotes#scan}. */
    static void noticed(ServerPlayer player, BlockState state) {
        if (state.getBlock() instanceof StainBlock stain) {
            observe(player, stain.stain().id());
        } else if (state.getBlock() instanceof BoulderBlock && state.getValue(BoulderBlock.COAT) != BoulderBlock.Coat.NONE) {
            observe(player, state.getValue(BoulderBlock.COAT).stain().id());
        } else if (state.is(ModBlocks.BLACK_SAND.get())) {
            observe(player, BLACK_SAND);
        }
    }

    /** Rubbing a stain is a closer look than walking past it. */
    public static void rubbed(ServerPlayer player, Stain stain) {
        observe(player, stain.id());
    }

    private static void observe(ServerPlayer player, String id) {
        JournalState state = JournalContent.state(player);
        if (state.seen("sign/" + id)) return;
        Leads.observe(player, "sign/" + id, Observations.KEY + "sign." + id, List.of(), icon(id), lead(id));
    }

    private static String lead(String id) {
        return switch (id) {
            case "gossan" -> "t3/iron_ore";
            case "malachite_bloom" -> "t1/nugget";
            case BLACK_SAND -> "t2/alloy_metal";
            default -> null;
        };
    }

    private static Identifier icon(String id) {
        Item item = switch (id) {
            case "gossan" -> ModItems.SMALL_ORES.get(OreMineral.LIMONITE).get();
            case "malachite_bloom" -> ModItems.SMALL_ORES.get(OreMineral.MALACHITE).get();
            case BLACK_SAND -> ModItems.BLACK_SAND.get();
            default -> dev.strataindustria.registry.Tier4Items.SULFUR.get();
        };
        return Observations.id(item);
    }
}
