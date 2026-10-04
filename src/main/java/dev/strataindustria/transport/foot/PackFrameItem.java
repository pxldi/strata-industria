package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The pack frame (spec 4.2): nine more slots, worn on the back. Right-click opens it from the hand; sneaking puts it on.
 * Worn, it opens with the pack key.
 */
public class PackFrameItem extends Item {
    public PackFrameItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return super.use(level, player, hand);
        if (player instanceof ServerPlayer serverPlayer) open(serverPlayer, player.getItemInHand(hand), true);
        return InteractionResult.SUCCESS;
    }

    /** The pack frame does not fit into another container item. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    /** Opens the frame's nine slots. {@code held} is true when it is in a hand rather than on the back. */
    public static void open(ServerPlayer player, ItemStack frame, boolean held) {
        if (!frame.is(FootRegistry.PACK_FRAME.get())) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, new PackContents(frame, p, held), 1),
                Component.translatable("container." + StrataIndustria.MOD_ID + ".pack_frame")));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), FootRegistry.PACK_OPEN.get(),
                SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    /** Opens the frame the player is wearing, if any. Returns whether one opened. */
    public static boolean openWorn(ServerPlayer player) {
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!worn.is(FootRegistry.PACK_FRAME.get())) return false;
        open(player, worn, false);
        return true;
    }
}
