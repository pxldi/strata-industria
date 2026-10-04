package dev.strataindustria.logistics;

import net.minecraft.world.item.ItemStack;

/** An item and its components, for counting stacks of the same kind together. */
record ItemKey(ItemStack stack) {
    @Override
    public boolean equals(Object other) {
        return other instanceof ItemKey key && ItemStack.isSameItemSameComponents(stack, key.stack);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }
}
