package dev.strataindustria.ceramics;

import dev.strataindustria.StrataIndustria;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A fired clay pot that carries four stacks (spec 4.3). Right-click to open it. */
public class SmallVesselItem extends Item {
    public SmallVesselItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new SmallVesselMenu(id, inventory, hand),
                    Component.translatable("container." + StrataIndustria.MOD_ID + ".small_vessel")),
                    buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DECORATED_POT_INSERT,
                    SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Vessels do not nest. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
