package dev.strataindustria.journal;

import dev.strataindustria.flora.FloraBlocks;
import dev.strataindustria.flora.IndicatorPlant;
import dev.strataindustria.flora.IndicatorPlantBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Observations about the indicator plants. The first plant of a kind the player walks past is noted; the first
 * ore found after that, of the kind the plant marks, is noted as the link between the two. Text lives under
 * {@code journal.strataindustria.observe.plant.*} and {@code observe.link.*}.
 */
public final class PlantNotes {
    /** Blocks to either side a player notices a plant from. */
    static final int REACH = 3;

    private PlantNotes() {}

    static String seenId(IndicatorPlant plant) {
        return "plant/" + plant.id();
    }

    static String linkId(IndicatorPlant plant) {
        return "link/" + plant.id();
    }

    /** Looks around the player for plants and checks the inventory for the ore that goes with plants already seen. */
    public static void scan(ServerPlayer player) {
        JournalState state = JournalContent.state(player);
        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-REACH, -1, -REACH), at.offset(REACH, 2, REACH))) {
            BlockState block = player.level().getBlockState(pos);
            SignNotes.noticed(player, block);
            if (block.getBlock() instanceof IndicatorPlantBlock plant && !state.seen(seenId(plant.plant()))) {
                seen(player, plant.plant());
            }
        }
        Inventory inventory = player.getInventory();
        for (IndicatorPlant plant : IndicatorPlant.values()) {
            if (plant.minerals().isEmpty() || !state.seen(seenId(plant)) || state.seen(linkId(plant))) continue;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (marks(plant, inventory.getItem(i))) {
                    Leads.observe(player, linkId(plant), Observations.KEY + "link." + plant.id(), List.of(), icon(plant), null);
                    break;
                }
            }
        }
    }

    static boolean seen(ServerPlayer player, IndicatorPlant plant) {
        return Leads.observe(player, seenId(plant), Observations.KEY + "plant." + plant.id(), List.of(), icon(plant), null);
    }

    /** True if the stack is a nugget or an ore piece of a mineral this plant marks. */
    static boolean marks(IndicatorPlant plant, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (OreMineral mineral : plant.minerals()) {
            var nugget = ModItems.SMALL_ORES.get(mineral);
            if (nugget != null && stack.is(nugget.get())) return true;
            if (!mineral.hasPieces()) continue;
            for (OreGrade grade : OreGrade.values()) {
                if (stack.is(ModItems.orePiece(mineral, grade))) return true;
            }
        }
        return false;
    }

    private static net.minecraft.resources.Identifier icon(IndicatorPlant plant) {
        return Observations.id(FloraBlocks.ITEMS.get(plant).get());
    }

}
