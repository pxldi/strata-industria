package dev.strataindustria.smithing;

import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Anvil screen: workpiece and finished piece, a grid of plans to pick from, the work bar, eight hit
 * buttons, and the recipe's rules next to the last three hits.
 */
public class AnvilMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 8, INPUT_Y = 27, OUTPUT_X = 152, OUTPUT_Y = 27;
    public static final int PLANS_X = 30, PLANS_Y = 18, PLANS_PER_ROW = 6;
    /** The weld row (tier 3 spec 9.4 and 9.5): second piece, flux, the Weld button, and the pattern slot. */
    public static final int WELD_Y = 130, SECOND_X = 8, FLUX_X = 26, WELD_BUTTON_X = 46, PATTERN_X = 152;
    public static final int INVENTORY_Y = 173;
    /** Button ids: 0 to 7 are the hits, {@code PLAN_BUTTON + i} picks plan i, {@code WELD_BUTTON} welds. */
    public static final int PLAN_BUTTON = 100, WELD_BUTTON = 200;
    private static final int PLAN_SLOT_START = AnvilBlockEntity.SLOTS;
    private static final int INVENTORY_START = PLAN_SLOT_START + AnvilBlockEntity.MAX_PLANS;

    private final Container container;
    private final ContainerData data;
    private final AnvilBlockEntity anvil;

    public AnvilMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(AnvilBlockEntity.SLOTS), new SimpleContainer(AnvilBlockEntity.MAX_PLANS),
                new SimpleContainerData(AnvilBlockEntity.DATA_COUNT));
    }

    public AnvilMenu(int id, Inventory inventory, AnvilBlockEntity anvil, ContainerData data) {
        this(id, inventory, anvil, anvil.plans(), data);
    }

    private AnvilMenu(int id, Inventory inventory, Container container, Container plans, ContainerData data) {
        super(ModMenus.ANVIL.get(), id);
        checkContainerSize(container, AnvilBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.anvil = container instanceof AnvilBlockEntity be ? be : null;
        addSlot(new Slot(container, AnvilBlockEntity.INPUT, INPUT_X, INPUT_Y));
        addSlot(new Slot(container, AnvilBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(container, AnvilBlockEntity.SECOND, SECOND_X, WELD_Y));
        addSlot(new Slot(container, AnvilBlockEntity.FLUX, FLUX_X, WELD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AnvilBlockEntity.isFlux(stack);
            }
        });
        addSlot(new Slot(container, AnvilBlockEntity.PATTERN, PATTERN_X, WELD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.SMITHING_PATTERN.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < AnvilBlockEntity.MAX_PLANS; i++) {
            addSlot(new Slot(plans, i, PLANS_X + (i % PLANS_PER_ROW) * 18, PLANS_Y + (i / PLANS_PER_ROW) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (anvil == null || !(player instanceof ServerPlayer server)) return false;
        if (id == WELD_BUTTON) {
            anvil.weld(server);
            return true;
        }
        if (id >= PLAN_BUTTON) {
            anvil.choose(id - PLAN_BUTTON);
            return true;
        }
        HitType hit = HitType.byId(id);
        if (hit == null) return false;
        anvil.hit(server, hit);
        return true;
    }

    public static boolean isPlanSlot(int index) {
        return index >= PLAN_SLOT_START && index < INVENTORY_START;
    }

    public int position() {
        return data.get(AnvilBlockEntity.DATA_POSITION);
    }

    /** The target position, or -1 before a plan is picked. */
    public int target() {
        return data.get(AnvilBlockEntity.DATA_TARGET);
    }

    /** The {@code i}-th most recent hit (0 = last), or null. */
    public HitType recent(int i) {
        return HitType.byId(data.get(AnvilBlockEntity.DATA_RECENT + i));
    }

    /** The recipe's {@code i}-th rule, or null. */
    public Rule rule(int i) {
        int code = data.get(AnvilBlockEntity.DATA_RULES + i);
        return code < 0 ? null : Rule.decode(code);
    }

    public int selectedPlan() {
        return data.get(AnvilBlockEntity.DATA_SELECTED);
    }

    public AnvilBlockEntity.Status status() {
        AnvilBlockEntity.Status[] values = AnvilBlockEntity.Status.values();
        return values[Math.floorMod(data.get(AnvilBlockEntity.DATA_STATUS), values.length)];
    }

    public int hits() {
        return data.get(AnvilBlockEntity.DATA_HITS);
    }

    public AnvilBlockEntity.WeldStatus weldStatus() {
        AnvilBlockEntity.WeldStatus[] values = AnvilBlockEntity.WeldStatus.values();
        return values[Math.floorMod(data.get(AnvilBlockEntity.DATA_WELD), values.length)];
    }

    public int weldingTemperature() {
        return data.get(AnvilBlockEntity.DATA_WELD_TEMP);
    }

    public int workingTemperature() {
        return data.get(AnvilBlockEntity.DATA_WORKING);
    }

    public ItemStack workpiece() {
        return slots.get(AnvilBlockEntity.INPUT).getItem();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || isPlanSlot(index)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAN_SLOT_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (AnvilBlockEntity.isFlux(stack)) {
            if (!moveItemStackTo(stack, AnvilBlockEntity.FLUX, AnvilBlockEntity.FLUX + 1, false)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.SMITHING_PATTERN.get())) {
            if (!moveItemStackTo(stack, AnvilBlockEntity.PATTERN, AnvilBlockEntity.PATTERN + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, AnvilBlockEntity.INPUT, AnvilBlockEntity.INPUT + 1, false)
                && !moveItemStackTo(stack, AnvilBlockEntity.SECOND, AnvilBlockEntity.SECOND + 1, false)) {
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
