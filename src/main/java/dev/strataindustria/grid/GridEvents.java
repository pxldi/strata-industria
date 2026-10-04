package dev.strataindustria.grid;

import dev.strataindustria.StrataIndustria;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Clicks with the stethoscope, taken before the block's own empty-hand action (like the wrench in {@code ToolEvents}). */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class GridEvents {
    private GridEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(GridBlocks.STETHOSCOPE.get())) return;
        InteractionResult result = StethoscopeItem.listen(event.getLevel(), event.getPos(), event.getEntity(), stack, event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
