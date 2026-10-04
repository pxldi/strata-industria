package dev.strataindustria.metal;

import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModMenus;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Crucible screen: a 3x3 input grid, the melt in the middle, the mold slot and Pour button on the right. */
public class CrucibleMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8, GRID_Y = 18;
    public static final int MOLD_X = 8, MOLD_Y = 78;
    public static final int INVENTORY_Y = 128;
    public static final int BUTTON_POUR = 0;
    private static final int CONTAINER_SLOTS = CrucibleBlockEntity.INPUT_SLOTS + 1;

    private final Container container;
    private final ContainerData data;
    private final CrucibleBlockEntity crucible;

    public CrucibleMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(CrucibleBlockEntity.DATA_COUNT));
    }

    public CrucibleMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.CRUCIBLE.get(), id);
        checkContainerSize(container, CONTAINER_SLOTS);
        this.container = container;
        this.data = data;
        this.crucible = container instanceof CrucibleBlockEntity be ? be : null;
        for (int i = 0; i < CrucibleBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new Slot(container, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CrucibleBlockEntity.accepts(stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    // The server knows how much room the melt has; the client only checks for metal.
                    if (crucible == null) return super.getMaxStackSize(stack);
                    return Math.min(super.getMaxStackSize(stack), crucible.roomFor(stack, getItem()));
                }
            });
        }
        addSlot(new Slot(container, CrucibleBlockEntity.MOLD_SLOT, MOLD_X, MOLD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CastMoldItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_POUR || crucible == null) return false;
        var problem = crucible.pourProblem();
        if (problem.isPresent()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    dev.strataindustria.StrataIndustria.MOD_ID + ".crucible.problem." + problem.get()));
            return false;
        }
        return crucible.startPour();
    }

    public int temperature() {
        return data.get(CrucibleBlockEntity.DATA_TEMPERATURE);
    }

    public CrucibleStatus status() {
        return CrucibleStatus.values()[Math.floorMod(data.get(CrucibleBlockEntity.DATA_STATUS), CrucibleStatus.values().length)];
    }

    public int meltingPercent() {
        return data.get(CrucibleBlockEntity.DATA_MELTING);
    }

    public int pourPercent() {
        return data.get(CrucibleBlockEntity.DATA_POUR);
    }

    public int slotProgress(int slot) {
        return data.get(CrucibleBlockEntity.DATA_SLOT_PROGRESS + slot);
    }

    /** The melt as the screen sees it (quality is not synced). */
    public Melt melt() {
        Map<Metal, Integer> units = new EnumMap<>(Metal.class);
        for (Metal metal : Metal.values()) {
            int u = data.get(CrucibleBlockEntity.DATA_UNITS + metal.ordinal());
            if (u > 0) units.put(metal, u);
        }
        return new Melt(units, 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof CastMoldItem) {
            if (!moveItemStackTo(stack, CrucibleBlockEntity.MOLD_SLOT, CrucibleBlockEntity.MOLD_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!CrucibleBlockEntity.accepts(stack) || !moveItemStackTo(stack, 0, CrucibleBlockEntity.INPUT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
