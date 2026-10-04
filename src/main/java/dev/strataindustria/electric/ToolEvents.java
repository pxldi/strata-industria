package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.prospecting.OreScannerItem;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier5Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Tier 5 hand tools (spec 13). A click with a tool in hand would otherwise reach the block's own empty-hand action
 * first, so the wrench and the scanner's charging take it here.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class ToolEvents {
    private ToolEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        InteractionResult result = InteractionResult.PASS;
        if (stack.is(Tier5Items.WRENCH.get())) {
            result = WrenchItem.apply(event.getLevel(), event.getPos(), event.getEntity(), event.getHitVec());
        } else if (stack.is(Tier5Items.ORE_SCANNER.get())) {
            result = OreScannerItem.charge(event.getLevel(), event.getPos(), event.getEntity(), stack);
        }
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    /** Spec 13.1: left-click takes cables and pipes off instantly. */
    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity().getMainHandItem().is(Tier5Items.WRENCH.get()) && event.getState().is(ModTags.Blocks.WRENCH_BREAKABLE)) {
            event.setNewSpeed(1000.0f);
        }
    }

    /** Spec 15, goal 77: a cinnabar block mined with a pick that can take it. */
    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player && event.getState().getBlock() instanceof OreBlock ore
                && ore.mineral() == OreMineral.CINNABAR) {
            Journal.award(player, Journal.CINNABAR_MINED);
        }
    }

    @SubscribeEvent
    static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (event.getEntity().getMainHandItem().is(Tier5Items.WRENCH.get()) && event.getTargetBlock().is(ModTags.Blocks.WRENCH_BREAKABLE)) {
            event.setCanHarvest(true);
        }
    }
}
