package dev.strataindustria.event;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.charcoal.LogPileBlock;
import dev.strataindustria.charcoal.LogPileBlockEntity;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Stacking logs for a charcoal pit (spec 4.4): sneak + right-click with a log. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class CharcoalEvents {
    private CharcoalEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!player.isSecondaryUseActive() || !held.is(ItemTags.LOGS_THAT_BURN) || event.getFace() == null) return;
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        BlockState state = level.getBlockState(clicked);
        ItemStack source = player.hasInfiniteMaterials() ? held.copy() : held;

        boolean placed = false;
        if (state.getBlock() instanceof LogPileBlock && !state.getValue(LogPileBlock.LIT)
                && level.getBlockEntity(clicked) instanceof LogPileBlockEntity pile
                && pile.count() < LogPileBlockEntity.MAX_LOGS) {
            placed = level.isClientSide() || pile.add(source);
            if (placed && !level.isClientSide()) {
                level.playSound(null, clicked, ModSounds.KILN_LOG.get(), SoundSource.BLOCKS, 0.8f,
                        0.9f + level.getRandom().nextFloat() * 0.2f);
            }
        }
        if (!placed) placed = LogPileBlock.placeNew(level, clicked.relative(event.getFace()), source);
        if (!placed) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
