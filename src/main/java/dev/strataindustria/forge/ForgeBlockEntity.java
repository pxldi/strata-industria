package dev.strataindustria.forge;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.roasting.RoastingRecipe;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
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
    /** Coke drives the forge to 1450 °C, 1600 °C with a bellows: hot enough to melt iron. */
    public static final float COKE_TEMPERATURE = 1450.0f;
    static final float HEAT_PER_TICK = 8.0f / 20;
    /** Extra degrees a bellows adds over what the fuel can reach. */
    public static final float BELLOWS_BONUS = 150.0f;
    static final float COOL_PER_TICK = 4.0f / 20;
    /** Above this the coals glow and give light. */
    public static final float GLOW_FROM = 400.0f;
    static final int HEAT_INTERVAL = 10;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_BURN_LEFT = 1;
    public static final int DATA_BURN_TOTAL = 2;
    /** Roasting progress of each heating slot, in percent (tier 4 spec 5.3). */
    public static final int DATA_ROAST = 3;
    public static final int DATA_COUNT = DATA_ROAST + HEAT_SLOTS;
    /** Pale yellow sulfur fume over a forge that is roasting ore. */
    private static final int FUME_COLOUR = 0xE2D46E;

    private NonNullList<ItemStack> items = NonNullList.withSize(1 + HEAT_SLOTS, ItemStack.EMPTY);
    private int burnLeft;
    private int burnTotal;
    /** How hot the fuel now burning can drive the forge; lignite burns cooler than charcoal. */
    private float burnCap = MAX_TEMPERATURE;
    private float temperature = Heat.AMBIENT;
    /** Ticks each heating slot has spent at roasting heat, and how many its recipe needs. */
    private final int[] roastProgress = new int[HEAT_SLOTS];
    private final int[] roastTotal = new int[HEAT_SLOTS];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_TEMPERATURE -> Math.round(temperature);
                case DATA_BURN_LEFT -> burnLeft;
                case DATA_BURN_TOTAL -> burnTotal;
                default -> {
                    int slot = index - DATA_ROAST;
                    if (slot < 0 || slot >= HEAT_SLOTS || roastTotal[slot] <= 0) yield 0;
                    yield Math.min(100, roastProgress[slot] * 100 / roastTotal[slot]);
                }
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
        // Tier 4 spec 3: coke burns long and hot enough for iron and steel.
        if (stack.is(dev.strataindustria.registry.Tier4Items.COKE.get())) return 3000;
        return 0;
    }

    /** The hottest a fuel can drive the forge: lignite stalls at 1200 °C, below iron welding heat. */
    public static float maxTemperature(ItemStack stack) {
        if (stack.is(dev.strataindustria.registry.Tier4Items.COKE.get())) return COKE_TEMPERATURE;
        return stack.is(ModItems.LIGNITE.get()) ? 1200.0f : MAX_TEMPERATURE;
    }

    public static boolean isFuel(ItemStack stack) {
        return burnTicks(stack) > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeBlockEntity forge) {
        boolean lit = state.getValue(ForgeBlock.LIT);
        float before = forge.temperature;
        // A bellows (spec 8.3): 150 °C hotter, heats 1.5 times as fast and burns fuel 1.5 times as fast.
        boolean blown = lit && ForgeAir.blown(level, pos);
        float cap = forge.burnCap + (blown ? BELLOWS_BONUS : 0.0f);
        if (lit) {
            if (forge.burnLeft > 0) forge.burnLeft--;
            if (blown && forge.burnLeft > 0 && level.getGameTime() % 2 == 0) forge.burnLeft--;
            if (forge.burnLeft <= 0 && !forge.takeFuel()) {
                ForgeBlock.setLit(level, pos, state, false);
                lit = false;
            }
        }
        if (lit && forge.temperature < cap) {
            forge.temperature = Math.min(cap, forge.temperature + HEAT_PER_TICK * (blown ? 1.5f : 1.0f));
        } else if (lit) {
            forge.temperature = Math.max(cap, forge.temperature - COOL_PER_TICK);
        } else forge.temperature = Math.max(Heat.AMBIENT, forge.temperature - COOL_PER_TICK);

        boolean glow = forge.temperature >= GLOW_FROM;
        if (state.getValue(ForgeBlock.HOT) != glow) {
            level.setBlock(pos, level.getBlockState(pos).setValue(ForgeBlock.HOT, glow), 3);
        }

        if (level.getGameTime() % HEAT_INTERVAL == 0) forge.heatItems(level, level.getGameTime());
        if (forge.temperature != before) setChanged(level, pos, state);
    }

    /** One heating and roasting step for the slots, as at game time {@code now}; the tick runs it every 10 ticks. */
    public void heatItems(Level level, long now) {
        boolean roasting = false;
        for (int i = FIRST_HEAT_SLOT; i < FIRST_HEAT_SLOT + HEAT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                roastProgress[i - FIRST_HEAT_SLOT] = roastTotal[i - FIRST_HEAT_SLOT] = 0;
                continue;
            }
            // A forge never melts what it holds (spec 4.5): melting happens only in a crucible.
            float cap = ForgeLimits.maxFor(stack);
            float current = Heat.get(stack, now);
            float target = temperature;
            if (!(target <= current && current - target < 1)) Heat.heatToward(stack, target, Heat.FORGE_RATE, HEAT_INTERVAL, cap, now);
            roasting |= roast(level, i, now);
        }
        if (roasting && level instanceof ServerLevel server) {
            // Tier 4 spec 21: the gas is lost, rising as a pale yellow fume.
            server.sendParticles(new DustParticleOptions(FUME_COLOUR, 1.4f), worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                    worldPosition.getZ() + 0.5, 2, 0.25, 0.05, 0.25, 0.01);
            if (level.getRandom().nextInt(4) == 0) {
                server.sendParticles(ParticleTypes.WHITE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                        worldPosition.getZ() + 0.5, 1, 0.2, 0.05, 0.2, 0.01);
            }
            if (now % (HEAT_INTERVAL * 4) == 0) {
                level.playSound(null, worldPosition, Tier4Sounds.ROASTING_SIZZLE.get(), SoundSource.BLOCKS, 0.35f,
                        0.9f + level.getRandom().nextFloat() * 0.2f);
            }
        }
    }

    /**
     * Tier 4 spec 5.3: an item with a roasting recipe that sits at or above the recipe's heat turns into
     * its result once it has been there long enough. Below that heat it waits without losing progress.
     */
    private boolean roast(Level level, int slot, long now) {
        int i = slot - FIRST_HEAT_SLOT;
        ItemStack stack = items.get(slot);
        Optional<RecipeHolder<RoastingRecipe>> found = RoastingRecipe.recipeFor(level, stack);
        if (found.isEmpty()) {
            roastProgress[i] = roastTotal[i] = 0;
            return false;
        }
        RoastingRecipe recipe = found.get().value();
        roastTotal[i] = recipe.ticks();
        float heat = Heat.get(stack, now);
        if (heat < recipe.minTemperature()) return false;
        roastProgress[i] += HEAT_INTERVAL;
        if (roastProgress[i] >= recipe.ticks()) {
            ItemStack result = recipe.result().create();
            result.setCount(Math.min(result.getMaxStackSize(), stack.getCount() * result.getCount()));
            Heat.set(result, heat, now);
            items.set(slot, result);
            roastProgress[i] = roastTotal[i] = 0;
            level.playSound(null, worldPosition, Tier4Sounds.ROASTING_DONE.get(), SoundSource.BLOCKS, 0.6f,
                    0.9f + level.getRandom().nextFloat() * 0.2f);
            setChanged();
        }
        return true;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Something else in a heating slot starts its roast from the beginning.
        if (slot >= FIRST_HEAT_SLOT && !ItemStack.isSameItem(stack, items.get(slot))) {
            roastProgress[slot - FIRST_HEAT_SLOT] = 0;
        }
        super.setItem(slot, stack);
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
        for (int i = 0; i < HEAT_SLOTS; i++) roastProgress[i] = input.getIntOr("roast_" + i, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
        output.putFloat("burn_cap", burnCap);
        output.putFloat("temperature", temperature);
        for (int i = 0; i < HEAT_SLOTS; i++) output.putInt("roast_" + i, roastProgress[i]);
    }
}
