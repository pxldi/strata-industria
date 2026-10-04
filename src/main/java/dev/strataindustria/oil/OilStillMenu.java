package dev.strataindustria.oil;

import dev.strataindustria.registry.Tier6Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Oil still screen: the crude tank, the three product tanks, the batch and the heat it gets. */
public class OilStillMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 100;

    private final ContainerData data;
    private final BlockPos pos;
    private final OilStillBlockEntity still;

    public OilStillMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null, new SimpleContainerData(OilStillBlockEntity.DATA_COUNT));
    }

    public OilStillMenu(int id, Inventory inventory, BlockPos pos, OilStillBlockEntity still, ContainerData data) {
        super(Tier6Menus.OIL_STILL.get(), id);
        this.data = data;
        this.pos = pos;
        this.still = still;
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public OilStillBlockEntity.Status status() {
        int s = data.get(OilStillBlockEntity.DATA_STATUS);
        var values = OilStillBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : OilStillBlockEntity.Status.EMPTY;
    }

    public int temperature() {
        return data.get(OilStillBlockEntity.DATA_TEMPERATURE);
    }

    public int heat() {
        return data.get(OilStillBlockEntity.DATA_HEAT);
    }

    public int limit() {
        return data.get(OilStillBlockEntity.DATA_LIMIT);
    }

    /** How far the batch has come, from 0 to 1. */
    public float progress() {
        return Math.min(1.0f, data.get(OilStillBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    /** Tank 0 is crude, 1 to 3 the products. */
    public int amount(int tank) {
        return data.get(OilStillBlockEntity.DATA_AMOUNT + tank);
    }

    public Fluid fluid(int tank) {
        if (amount(tank) <= 0) return Fluids.EMPTY;
        return BuiltInRegistries.FLUID.byId(data.get(OilStillBlockEntity.DATA_FLUID + tank));
    }

    /** Buttons 0 to 2 fill a bucket from that product tank. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= OilStillBlockEntity.PRODUCTS || still == null) return false;
        return still.takeBucket(player, null, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return still == null || !still.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
