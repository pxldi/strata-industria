package dev.strataindustria.transport.foot;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** The nine slots of a pack frame, written back to the item's {@code pack_contents} on every change. */
public class PackContents extends SimpleContainer {
    public static final int SLOTS = 9;

    private final ItemStack frame;
    private final Player owner;
    private final boolean held;

    public PackContents(ItemStack frame, Player owner, boolean held) {
        super(SLOTS);
        this.frame = frame;
        this.owner = owner;
        this.held = held;
        NonNullList<ItemStack> stored = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        frame.getOrDefault(FootRegistry.PACK_CONTENTS.get(), ItemContainerContents.EMPTY).copyInto(stored);
        for (int i = 0; i < SLOTS; i++) setItem(i, stored.get(i), false);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        frame.set(FootRegistry.PACK_CONTENTS.get(), ItemContainerContents.fromItems(getItems()));
    }

    /** A pack frame does not hold another pack frame, a handcart or a shulker box. */
    public static boolean accepts(ItemStack stack) {
        if (stack.is(FootRegistry.PACK_FRAME.get()) || stack.is(FootRegistry.HANDCART.get())) return false;
        return !(stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        if (frame.isEmpty() || player != owner) return false;
        return held ? player.getMainHandItem() == frame || player.getOffhandItem() == frame
                : player.getItemBySlot(EquipmentSlot.CHEST) == frame;
    }

    /** Number of slots holding something. */
    public static int filledSlots(ItemStack frame) {
        ItemContainerContents contents = frame.getOrDefault(FootRegistry.PACK_CONTENTS.get(), ItemContainerContents.EMPTY);
        int n = 0;
        for (var ignored : contents.nonEmptyItems()) n++;
        return n;
    }
}
