package dev.strataindustria.multiblock;

import dev.strataindustria.StrataIndustria;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/** Using the controller of an unfinished multiblock says which block is missing and where. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class MultiblockEvents {
    private MultiblockEvents() {}

    /** The ledger and anything else that takes the click goes first; placing a block against the controller is building, not asking. */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getItemStack().getItem() instanceof BlockItem) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        Optional<Multiblock> multiblock = Multiblocks.forController(level, state.getBlock());
        if (multiblock.isEmpty()) return;
        Component message = incomplete(level, multiblock.get(), event.getPos(), Multiblocks.facing(state));
        if (message != null) player.sendOverlayMessage(message);
    }

    /** "Structure incomplete: Fire Bricks at 1, 64, 3", or null when the structure is whole. */
    public static @Nullable Component incomplete(Level level, Multiblock multiblock, BlockPos controller, Direction facing) {
        Multiblock.Mismatch mismatch = multiblock.firstMismatch(level, controller, facing);
        if (mismatch == null) return null;
        Component what = mismatch.expected() != null ? mismatch.expected().getBlock().getName()
                : Component.translatable(StrataIndustria.MOD_ID + ".multiblock.empty_space");
        return Component.translatable(StrataIndustria.MOD_ID + ".multiblock.incomplete", what, mismatch.pos().toShortString());
    }
}
