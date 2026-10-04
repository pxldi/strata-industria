package dev.strataindustria.forge;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The forge (spec 4.5): charcoal in, four heating slots, a temperature that climbs 8 °C a second while
 * fuel burns and falls 4 °C a second without. Items in the slots heat towards it.
 */
public class ForgeBlockEntity extends BaseContainerBlockEntity {
    public static final int FUEL_SLOT = 0;
    public static final int FIRST_HEAT_SLOT = 1;
    public static final int HEAT_SLOTS = 4;
    public static final int FUEL_STACK = 16;
    public static final float MAX_TEMPERATURE = 1350.0f;
    static final float HEAT_PER_TICK = 8.0f / 20;
    static final float COOL_PER_TICK = 4.0f / 20;
    /** Above this the coals glow and give light. */
    public static final float GLOW_FROM = 400.0f;
    static final int HEAT_INTERVAL = 10;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_BURN_LEFT = 1;
    public static final int DATA_BURN_TOTAL = 2;
    public static final int DATA_COUNT = 3;

    private NonNullList<ItemStack> items = NonNullList.withSize(1 + HEAT_SLOTS, ItemStack.EMPTY);
    private int burnLeft;
    private int burnTotal;
    /** How hot the fuel now burning can drive the forge; lignite burns cooler than charcoal. */
    private float burnCap = MAX_TEMPERATURE;
    private float temperature = Heat.AMBIENT;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_TEMPERATURE -> Math.round(temperature);
                case DATA_BURN_LEFT -> burnLeft;
                case DATA_BURN_TOTAL -> burnTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_TEMPERATURE -> temperature = value;
                case DATA_BURN_LEFT -> burnLeft = value;
                case DATA_BURN_TOTAL -> burnTotal = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGE.get(), pos, state);
    }

    /** Burn ticks of a forge fuel, 0 for anything else. Logs and sticks burn too cool. */
    public static int burnTicks(ItemStack stack) {
        if (stack.is(Items.CHARCOAL)) return 2400;
        if (stack.is(Items.COAL)) return 3200;
        if (stack.is(ModItems.LIGNITE.get())) return 1600;
        return 0;
    }

    /** The hottest a fuel can drive the forge: lignite stalls at 1200 °C, below iron welding heat. */
    public static float maxTemperature(ItemStack stack) {
        return stack.is(ModItems.LIGNITE.get()) ? 1200.0f : MAX_TEMPERATURE;
    }

    public static boolean isFuel(ItemStack stack) {
        return burnTicks(stack) > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeBlockEntity forge) {
        boolean lit = state.getValue(ForgeBlock.LIT);
        float before = forge.temperature;
        if (lit) {
            if (forge.burnLeft > 0) forge.burnLeft--;
            if (forge.burnLeft <= 0 && !forge.takeFuel()) {
                ForgeBlock.setLit(level, pos, state, false);
                lit = false;
            }
        }
        if (lit && forge.temperature < forge.burnCap) {
            forge.temperature = Math.min(forge.burnCap, forge.temperature + HEAT_PER_TICK);
        } else if (lit) {
            forge.temperature = Math.max(forge.burnCap, forge.temperature - COOL_PER_TICK);
        } else forge.temperature = Math.max(Heat.AMBIENT, forge.temperature - COOL_PER_TICK);

        boolean glow = forge.temperature >= GLOW_FROM;
        if (state.getValue(ForgeBlock.HOT) != glow) {
            level.setBlock(pos, level.getBlockState(pos).setValue(ForgeBlock.HOT, glow), 3);
        }

        if (level.getGameTime() % HEAT_INTERVAL == 0) forge.heatItems(level);
        if (forge.temperature != before) setChanged(level, pos, state);
    }

    private void heatItems(Level level) {
        long now = level.getGameTime();
        for (int i = FIRST_HEAT_SLOT; i < FIRST_HEAT_SLOT + HEAT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            // A forge never melts what it holds (spec 4.5): melting happens only in a crucible.
            float cap = ForgeLimits.maxFor(stack);
            float current = Heat.get(stack, now);
            float target = temperature;
            if (target <= current && current - target < 1) continue;
            Heat.heatToward(stack, target, Heat.FORGE_RATE, HEAT_INTERVAL, cap, now);
        }
    }

    private boolean takeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        int ticks = burnTicks(fuel);
        if (ticks <= 0) return false;
        burnLeft = burnTotal = ticks;
        burnCap = maxTemperature(fuel);
        fuel.shrink(1);
        setChanged();
        return true;
    }

    public boolean hasFuel() {
        return burnLeft > 0 || isFuel(items.get(FUEL_SLOT));
    }

    void onIgnite() {
        if (burnLeft <= 0) takeFuel();
        setChanged();
    }

    public float temperature() {
        return temperature;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == FUEL_SLOT ? isFuel(stack) : items.get(slot).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return FUEL_STACK;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".forge");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ForgeMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        burnLeft = input.getIntOr("burn_left", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        burnCap = input.getFloatOr("burn_cap", MAX_TEMPERATURE);
        temperature = input.getFloatOr("temperature", Heat.AMBIENT);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
        output.putFloat("burn_cap", burnCap);
        output.putFloat("temperature", temperature);
    }
}
