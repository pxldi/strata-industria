package dev.strataindustria.steam;

import dev.strataindustria.registry.Tier4Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Boiler screen, for the bronze boiler and the steel boiler's controller: water, steam, pressure,
 * integrity and what the fire gives. No slots of its own.
 */
public class BoilerMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 84;
    /** How far from the boiler the screen stays open. */
    private static final double REACH = 8.0;

    private final ContainerData data;
    private final BlockPos pos;

    public BoilerMenu(MenuType<?> type, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(type, id, inventory, buf.readBlockPos(), new SimpleContainerData(BoilerBlockEntity.DATA_COUNT));
    }

    public BoilerMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(type, id);
        this.data = data;
        this.pos = pos;
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public int water() {
        return data.get(BoilerBlockEntity.DATA_WATER);
    }

    public int steam() {
        return data.get(BoilerBlockEntity.DATA_STEAM);
    }

    public int waterCapacity() {
        return Math.max(1, data.get(BoilerBlockEntity.DATA_WATER_CAPACITY));
    }

    public int steamCapacity() {
        return Math.max(1, data.get(BoilerBlockEntity.DATA_STEAM_CAPACITY));
    }

    public float ratedPressure() {
        return data.get(BoilerBlockEntity.DATA_RATED) / 10.0f;
    }

    public float pressure() {
        return ratedPressure() * steam() / steamCapacity();
    }

    /** Whether this is a steel boiler's controller. */
    public boolean steel() {
        return getType() == Tier4Menus.BOILER_CONTROLLER.get();
    }

    public SteelBoilerStructure.Problem problem() {
        int p = data.get(BoilerBlockEntity.DATA_PROBLEM);
        var values = SteelBoilerStructure.Problem.values();
        return p >= 0 && p < values.length ? values[p] : SteelBoilerStructure.Problem.NONE;
    }

    /** Layer 1 (the fire) to 5 of the missing block. */
    public int problemLayer() {
        return data.get(BoilerBlockEntity.DATA_WHERE) / 9 + 1;
    }

    /** Where in its layer, from 0 (front left) to 8 (back right), as seen standing at the controller. */
    public int problemSpot() {
        return data.get(BoilerBlockEntity.DATA_WHERE) % 9;
    }

    /** Warm-up done, 0 to 100. */
    public int warmth() {
        return data.get(BoilerBlockEntity.DATA_WARMTH);
    }

    public float integrity() {
        return data.get(BoilerBlockEntity.DATA_INTEGRITY) / 10.0f;
    }

    /** How hot the fire under it is, °C; 0 with no fire. */
    public int temperature() {
        return data.get(BoilerBlockEntity.DATA_TEMPERATURE);
    }

    /** HU per tick it takes. */
    public int heat() {
        return data.get(BoilerBlockEntity.DATA_HEAT);
    }

    public BoilerBlockEntity.Status status() {
        int s = data.get(BoilerBlockEntity.DATA_STATUS);
        var values = BoilerBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : BoilerBlockEntity.Status.NO_HEAT;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // Only the player's inventory: shift-click moves between the main grid and the hotbar.
        boolean hotbar = index >= 27;
        if (!moveItemStackTo(stack, hotbar ? 0 : 27, hotbar ? 27 : 36, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof BoilerBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= REACH * REACH;
    }
}
