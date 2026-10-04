package dev.strataindustria.event;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.PitKilnBlock;
import dev.strataindustria.ceramics.PitKilnBlockEntity;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Setting unfired clay out for a pit kiln (spec 4.2): sneak + right-click the top of a block. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class ClayEvents {
    private ClayEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!player.isSecondaryUseActive() || !held.is(ModTags.Items.PIT_KILN_FIREABLE)) return;
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        BlockState state = level.getBlockState(clicked);

        boolean placed;
        if (state.getBlock() instanceof PitKilnBlock) {
            // More pieces can go into a kiln until the thatch starts.
            placed = state.getValue(PitKilnBlock.STRAW) == 0 && !state.getValue(PitKilnBlock.LIT)
                    && level.getBlockEntity(clicked) instanceof PitKilnBlockEntity kiln
                    && (level.isClientSide() || kiln.place(source(player, held)));
            if (placed && !level.isClientSide()) {
                level.playSound(null, clicked, ModSounds.CLAY_SHAPE.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
            }
        } else if (event.getFace() == Direction.UP) {
            placed = PitKilnBlock.placeNew(level, clicked.above(), source(player, held));
        } else {
            return;
        }
        if (!placed) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Creative players keep what they hold. */
    private static ItemStack source(Player player, ItemStack held) {
        return player.hasInfiniteMaterials() ? held.copy() : held;
    }
}
