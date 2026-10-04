package dev.strataindustria.metal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.Tier4Menus;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Smelter screen: the crucible's grid, melt and gauge, then a row from the mold stock through the mold
 * and the cooling slot to the output, with the Pour and Auto-pour buttons under it.
 */
public class SmelterMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8, GRID_Y = 18;
    public static final int ROW_Y = 78, STOCK_X = 8, MOLD_X = 26, COOLING_X = 62, OUTPUT_X = 98;
    public static final int BUTTON_Y = 96, INVENTORY_Y = 158;
    public static final int BUTTON_POUR = 0, BUTTON_AUTO = 1;
    private static final int SLOTS = SmelterBlockEntity.SLOTS;

    private final Container container;
    private final ContainerData data;
    private final @Nullable SmelterBlockEntity smelter;

    public SmelterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(SLOTS), new SimpleContainerData(SmelterBlockEntity.DATA_COUNT));
        buf.readBlockPos();
    }

    public SmelterMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(Tier4Menus.SMELTER.get(), id);
        checkContainerSize(container, SLOTS);
        this.container = container;
        this.data = data;
        this.smelter = container instanceof SmelterBlockEntity be ? be : null;
        for (int i = 0; i < CrucibleBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new Slot(container, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CrucibleBlockEntity.accepts(stack, true);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    // The server knows how much room the melt has; the client only checks for metal.
                    if (smelter == null) return super.getMaxStackSize(stack);
                    return Math.min(super.getMaxStackSize(stack), smelter.roomFor(stack, getItem()));
                }
            });
        }
        addSlot(new Slot(container, CrucibleBlockEntity.MOLD_SLOT, MOLD_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return emptyMold(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, SmelterBlockEntity.STOCK_SLOT, STOCK_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return emptyMold(stack);
            }

            @Override
            public int getMaxStackSize() {
                return SmelterBlockEntity.STOCK_SIZE;
            }
        });
        addSlot(new Slot(container, SmelterBlockEntity.COOLING_SLOT, COOLING_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(container, SmelterBlockEntity.OUTPUT_SLOT, OUTPUT_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    private static boolean emptyMold(ItemStack stack) {
        return stack.getItem() instanceof CastMoldItem && !stack.has(ModDataComponents.CAST_CONTENTS.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (smelter == null) return false;
        if (id == BUTTON_AUTO) {
            smelter.toggleAutoPour();
            return true;
        }
        if (id != BUTTON_POUR) return false;
        var problem = smelter.pourProblem();
        if (problem.isPresent()) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".crucible.problem." + problem.get()));
            return false;
        }
        return smelter.startPour();
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

    public int capacity() {
        return data.get(CrucibleBlockEntity.DATA_CAPACITY);
    }

    public int maxTemperature() {
        return data.get(CrucibleBlockEntity.DATA_MAX_TEMPERATURE);
    }

    public int slotProgress(int slot) {
        return data.get(CrucibleBlockEntity.DATA_SLOT_PROGRESS + slot);
    }

    public boolean autoPour() {
        return data.get(SmelterBlockEntity.DATA_AUTO) != 0;
    }

    public int heat() {
        return data.get(SmelterBlockEntity.DATA_HEAT);
    }

    /** The temperature of the heat coming in, which is what the pot heats towards. */
    public int heatTemperature() {
        return data.get(SmelterBlockEntity.DATA_HEAT_TEMPERATURE);
    }

    public int limit() {
        return data.get(SmelterBlockEntity.DATA_LIMIT);
    }

    /** The cooling mold's temperature, or 0 with none cooling. */
    public int coolingTemperature() {
        return data.get(SmelterBlockEntity.DATA_COOLING);
    }

    /** What the load needs to melt: the melt's own melting point, or the highest of what waits in the inputs. */
    public int need() {
        Melt melt = melt();
        int need = melt.isEmpty() ? 0 : CrucibleBlockEntity.mixMeltingPoint(melt);
        for (int i = 0; i < CrucibleBlockEntity.INPUT_SLOTS; i++) {
            // Carbon and redstone dissolve into a melt rather than melting on their own.
            need = Math.max(need, MetalContent.of(slots.get(i).getItem())
                    .filter(m -> !m.units().containsKey(Metal.CARBON) && !m.units().containsKey(Metal.REDSTONE))
                    .map(CrucibleBlockEntity::mixMeltingPoint).orElse(0));
        }
        return need;
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
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (emptyMold(stack)) {
            // Slot order in the menu: inputs, mold, stock.
            int mold = CrucibleBlockEntity.MOLD_SLOT;
            if (!moveItemStackTo(stack, mold, mold + 2, false)) return ItemStack.EMPTY;
        } else if (!CrucibleBlockEntity.accepts(stack, true) || !moveItemStackTo(stack, 0, CrucibleBlockEntity.INPUT_SLOTS, false)) {
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
