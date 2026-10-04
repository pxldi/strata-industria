package dev.strataindustria.mark;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.smithing.AnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The mark editor: an 8x8 grid with no slots. Button ids 0-63 flip a cell, {@link #CLEAR} empties the grid and
 * {@link #CONFIRM} keeps the mark. If the editor was opened from an anvil, the piece waiting in its output is
 * stamped too.
 */
public class MarkMenu extends AbstractContainerMenu {
    public static final int CLEAR = MakersMark.CELLS;
    public static final int CONFIRM = MakersMark.CELLS + 1;

    private final int[] cells = new int[MakersMark.CELLS];
    private final @Nullable BlockPos anvil;

    /** Client side: the cells arrive through the data slots. */
    public MarkMenu(int id) {
        this(id, MakersMark.BLANK, null);
    }

    public MarkMenu(int id, MakersMark start, @Nullable BlockPos anvil) {
        super(MarkRegistry.MENU.get(), id);
        this.anvil = anvil;
        for (int i = 0; i < cells.length; i++) {
            cells[i] = start.get(i) ? 1 : 0;
            addDataSlot(DataSlot.shared(cells, i));
        }
    }

    public boolean isOn(int cell) {
        return cells[cell] != 0;
    }

    public MakersMark mark() {
        long bits = 0L;
        for (int i = 0; i < cells.length; i++) if (cells[i] != 0) bits |= 1L << i;
        return new MakersMark(bits);
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id >= 0 && id < MakersMark.CELLS) {
            cells[id] ^= 1;
            sound(clicker, MarkRegistry.MARK_CUT.get(), 0.9f + clicker.getRandom().nextFloat() * 0.2f);
            return true;
        }
        if (id == CLEAR) {
            java.util.Arrays.fill(cells, 0);
            return true;
        }
        if (id == CONFIRM && clicker instanceof ServerPlayer player) {
            MakersMark mark = mark();
            if (mark.isBlank()) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".mark.blank"));
                return false;
            }
            MakerMarks.set(player, mark);
            stampWaitingPiece(player);
            sound(player, MarkRegistry.STAMP_STRIKE.get(), 1.0f);
            player.closeContainer();
            return true;
        }
        return false;
    }

    private void stampWaitingPiece(ServerPlayer player) {
        if (anvil == null || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(anvil)) > 64.0) return;
        if (player.level().getBlockEntity(anvil) instanceof AnvilBlockEntity entity) entity.stampOutput(player);
    }

    private static void sound(Player player, net.minecraft.sounds.SoundEvent event, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), event, SoundSource.PLAYERS, 0.7f, pitch);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
