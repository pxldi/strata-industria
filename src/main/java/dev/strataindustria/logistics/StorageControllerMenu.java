package dev.strataindustria.logistics;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The storage controller's screen (tier 5 spec 12.3): the grid is not slots but a list the server sends and the
 * client searches, sorts and scrolls; clicks on it go back as one packet each. Only the player's own inventory
 * is made of real slots.
 */
public class StorageControllerMenu extends AbstractContainerMenu {
    public static final int INVENTORY_X = 16, INVENTORY_Y = 152;
    /** Ticks between looks at whether the stored items changed, while the screen is open. */
    private static final int REFRESH = 10;

    private final BlockPos pos;
    private final @Nullable StorageControllerBlockEntity controller;
    private final Inventory inventory;
    private List<StorageEntry> entries = List.of();
    private boolean powered;
    private int inventories;
    private int lastHash = Integer.MIN_VALUE;
    private boolean lastPowered;
    private int age;
    private boolean dirty = true;

    /** The client's menu, built from the position the server wrote. */
    public StorageControllerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null);
    }

    public StorageControllerMenu(int id, Inventory inventory, BlockPos pos, @Nullable StorageControllerBlockEntity controller) {
        super(Tier5Logistics.STORAGE_CONTROLLER_MENU.get(), id);
        this.pos = pos;
        this.controller = controller;
        this.inventory = inventory;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, 9 + row * 9 + col, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
    }

    public BlockPos pos() {
        return pos;
    }

    // ------------------------------------------------------------------ client side

    public List<StorageEntry> entries() {
        return entries;
    }

    public boolean powered() {
        return powered;
    }

    public int inventories() {
        return inventories;
    }

    /** A new list from the server. */
    public void apply(List<StorageEntry> entries, boolean powered, int inventories) {
        this.entries = entries;
        this.powered = powered;
        this.inventories = inventories;
    }

    // ------------------------------------------------------------------ server side

    /** Called every tick the screen is open: sends a fresh list when the contents or the power changed. */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (controller == null || !(inventory.player instanceof ServerPlayer player)) return;
        if (!dirty && ++age < REFRESH) return;
        age = 0;
        if (dirty) controller.invalidate();
        dirty = false;
        List<StorageEntry> now = controller.index();
        boolean power = controller.powered();
        int hash = 17 * controller.inventories().size() + (power ? 1 : 0);
        for (StorageEntry entry : now) hash = 31 * hash + Long.hashCode(entry.count()) + ItemStack.hashItemAndComponents(entry.stack());
        if (hash == lastHash && power == lastPowered) return;
        lastHash = hash;
        lastPowered = power;
        PacketDistributor.sendToPlayer(player, new StoragePayloads.Snapshot(containerId, now, power, controller.inventories().size()));
    }

    /**
     * A click on the grid. With something on the cursor it is stored (all of it, or one with the right button);
     * with an empty cursor it takes the clicked kind (a stack, or half with the right button), or with shift
     * puts a stack straight into the inventory.
     */
    public void gridClick(ServerPlayer player, ItemStack proto, int button, boolean shift) {
        if (controller == null || !controller.powered()) return;
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            ItemStack part = button == 1 ? carried.copyWithCount(1) : carried.copy();
            ItemStack left = controller.insert(part);
            int stored = part.getCount() - left.getCount();
            carried.shrink(stored);
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else if (!proto.isEmpty()) {
            int want = button == 1 ? Math.max(1, proto.getMaxStackSize() / 2) : proto.getMaxStackSize();
            ItemStack taken = controller.take(proto, want);
            if (taken.isEmpty()) return;
            if (shift) {
                inventory.add(taken);
                if (!taken.isEmpty()) controller.insert(taken);
            } else {
                setCarried(taken);
            }
        }
        dirty = true;
        broadcastChanges();
    }

    /** Shift-click on an inventory slot stores that stack. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (controller == null || !slot.hasItem() || !controller.powered()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        ItemStack left = controller.insert(stack.copy());
        if (left.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.set(left);
        slot.setChanged();
        dirty = true;
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller != null && controller.isRemoved()) return false;
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (controller != null && !player.level().isClientSide()) {
            player.level().playSound(null, pos, Tier5Logistics.CONTROLLER_CLOSE.get(), SoundSource.BLOCKS, 0.4f, 1.0f);
        }
    }
}
