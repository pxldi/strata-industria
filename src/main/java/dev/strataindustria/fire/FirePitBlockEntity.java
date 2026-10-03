package dev.strataindustria.fire;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fire pit state (tier 0-2 spec 3.5): one fuel slot, one cooking slot, and a temperature that climbs
 * towards the burning fuel's maximum and falls once the fuel is spent. The pit goes out when it has
 * cooled back to ambient, or when rain falls on it.
 */
public class FirePitBlockEntity extends BaseContainerBlockEntity {
    public static final int FUEL_SLOT = 0;
    public static final int COOK_SLOT = 1;
    public static final float AMBIENT = 20.0f;
    /** Cooking needs the pit at or above this temperature. */
    public static final float COOK_TEMPERATURE = 200.0f;
    public static final int COOK_TICKS = 600;
    /** 10 °C a second while burning, 5 °C a second while cooling. */
    static final float HEAT_PER_TICK = 0.5f;
    static final float COOL_PER_TICK = 0.25f;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_BURN_LEFT = 1;
    public static final int DATA_BURN_TOTAL = 2;
    public static final int DATA_COOK = 3;
    public static final int DATA_COUNT = 4;

    private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> cooking =
            RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int burnLeft;
    private int burnTotal;
    private float temperature = AMBIENT;
    private float maxTemperature = AMBIENT;
    private int cookTicks;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_TEMPERATURE -> Math.round(temperature);
                case DATA_BURN_LEFT -> burnLeft;
                case DATA_BURN_TOTAL -> burnTotal;
                case DATA_COOK -> cookTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_TEMPERATURE -> temperature = value;
                case DATA_BURN_LEFT -> burnLeft = value;
                case DATA_BURN_TOTAL -> burnTotal = value;
                case DATA_COOK -> cookTicks = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FirePitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIRE_PIT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FirePitBlockEntity pit) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        boolean lit = state.getValue(FirePitBlock.LIT);
        boolean changed = false;

        if (lit) {
            if (pit.burnLeft > 0) pit.burnLeft--;
            if (pit.burnLeft <= 0 && pit.takeFuel()) changed = true;
            if (pit.burnLeft <= 0) pit.maxTemperature = AMBIENT;

            if (level.getGameTime() % 20 == 0 && Ignitable.rainedOn(level, pos) && level.getRandom().nextInt(3) == 0) {
                pit.burnLeft = 0;
                pit.maxTemperature = AMBIENT;
                FirePitBlock.extinguish(level, pos, state, true);
                lit = false;
                changed = true;
            }
        }

        float before = pit.temperature;
        if (lit && pit.burnLeft > 0 && pit.temperature < pit.maxTemperature) {
            pit.temperature = Math.min(pit.maxTemperature, pit.temperature + HEAT_PER_TICK);
        } else if (pit.temperature > AMBIENT) {
            float floor = lit && pit.burnLeft > 0 ? pit.maxTemperature : AMBIENT;
            pit.temperature = Math.max(floor, pit.temperature - COOL_PER_TICK);
        }
        if (pit.temperature != before) changed = true;

        if (lit && pit.burnLeft <= 0 && pit.temperature <= AMBIENT) {
            FirePitBlock.extinguish(level, pos, state, false);
            changed = true;
        }

        if (pit.cook(serverLevel)) changed = true;
        if (changed) setChanged(level, pos, state);
    }

    /** Starts burning the next fuel item. Returns whether there was one. */
    private boolean takeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        Optional<FirePitFuel> burning = FirePitFuel.of(fuel);
        if (burning.isEmpty()) return false;
        burnLeft = burnTotal = burning.get().burnTicks();
        maxTemperature = burning.get().maxTemperature();
        fuel.shrink(1);
        return true;
    }

    private boolean cook(ServerLevel level) {
        ItemStack food = items.get(COOK_SLOT);
        if (food.isEmpty()) {
            boolean had = cookTicks != 0;
            cookTicks = 0;
            return had;
        }
        if (temperature < COOK_TEMPERATURE) return false;
        SingleRecipeInput input = new SingleRecipeInput(food);
        Optional<RecipeHolder<CampfireCookingRecipe>> recipe = cooking.getRecipeFor(input, level);
        if (recipe.isEmpty()) {
            cookTicks = 0;
            return false;
        }
        if (++cookTicks >= COOK_TICKS) {
            ItemStack result = recipe.get().value().assemble(input);
            items.set(COOK_SLOT, result);
            cookTicks = 0;
            level.playSound(null, worldPosition, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8f, 1.4f);
        }
        return true;
    }

    /** Whether there is something to light: fuel in the slot, or fuel still burning. */
    public boolean hasFuel() {
        return burnLeft > 0 || FirePitFuel.isFuel(items.get(FUEL_SLOT));
    }

    /** Called when the pit is lit: start the first fuel item if nothing is burning yet. */
    void onIgnite() {
        if (burnLeft <= 0) takeFuel();
        setChanged();
    }

    public float temperature() {
        return temperature;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == COOK_SLOT && !ItemStack.isSameItemSameComponents(stack, items.get(COOK_SLOT))) cookTicks = 0;
        super.setItem(slot, stack);
    }

    /** Fuel goes in the fuel slot; the cooking slot takes one item at a time. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == FUEL_SLOT ? FirePitFuel.isFuel(stack) : items.get(COOK_SLOT).isEmpty() && stack.getCount() == 1;
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".fire_pit");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FirePitMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        burnLeft = input.getIntOr("burn_left", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        temperature = input.getFloatOr("temperature", AMBIENT);
        maxTemperature = input.getFloatOr("max_temperature", AMBIENT);
        cookTicks = input.getIntOr("cook_ticks", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
        output.putFloat("temperature", temperature);
        output.putFloat("max_temperature", maxTemperature);
        output.putInt("cook_ticks", cookTicks);
    }
}
