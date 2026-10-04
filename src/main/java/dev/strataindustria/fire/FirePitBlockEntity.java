package dev.strataindustria.fire;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.KilnFiring;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fire pit state (tier 0-2 spec 3.5): one fuel slot, one cooking slot, and a temperature that climbs
 * towards the burning fuel's maximum and falls once the fuel is spent. The pit goes out when it has
 * cooled back to ambient, or when rain falls on it.
 * <p>
 * Up to four unfired clay pieces stand on the hearth beside the fire. Each one that stays in the heat of
 * 450 °C or more for {@link Config#FIRING_TICKS} ticks comes out fired; there is no way to ruin a piece.
 */
public class FirePitBlockEntity extends BaseContainerBlockEntity {
    public static final int FUEL_SLOT = 0;
    public static final int COOK_SLOT = 1;
    public static final float AMBIENT = 20.0f;
    /** Cooking needs the pit at or above this temperature. */
    public static final float COOK_TEMPERATURE = 200.0f;
    public static final int COOK_TICKS = 600;
    /** Unfired pieces on the hearth stones. */
    public static final int HEARTH_SPOTS = 4;
    /** Clay on the hearth fires while the pit is at or above this temperature. */
    public static final float FIRING_TEMPERATURE = 450.0f;
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
    private final NonNullList<ItemStack> hearth = NonNullList.withSize(HEARTH_SPOTS, ItemStack.EMPTY);
    private final int[] firingTicks = new int[HEARTH_SPOTS];

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
        if (pit.fireHearth(serverLevel, pos)) changed = true;
        if (changed) setChanged(level, pos, state);
    }

    /** Clay on the hearth heats while the pit is hot enough; a finished piece rings and comes out fired. */
    private boolean fireHearth(ServerLevel level, BlockPos pos) {
        if (temperature < FIRING_TEMPERATURE) return false;
        boolean changed = false;
        boolean sync = false;
        int needed = Config.FIRING_TICKS.getAsInt();
        for (int i = 0; i < HEARTH_SPOTS; i++) {
            ItemStack piece = hearth.get(i);
            if (piece.isEmpty() || !KilnFiring.isFireable(piece)) continue;
            changed = true;
            if (++firingTicks[i] % 20 == 0) sync = true;
            if (firingTicks[i] < needed) continue;
            hearth.set(i, KilnFiring.fire(piece));
            firingTicks[i] = 0;
            sync = true;
            double x = pos.getX() + HEARTH_X[i], y = pos.getY() + 0.3, z = pos.getZ() + HEARTH_Z[i];
            level.playSound(null, pos, ModSounds.POTTERY_RING.get(), SoundSource.BLOCKS, 0.9f, 0.9f + 0.15f * i);
            level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 10, 0.12, 0.1, 0.12, 0.4);
            level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 6, 0.1, 0.05, 0.1, 0.03);
            Journal.awardNear(level, pos, Journal.POTTERY_FIRED);
        }
        if (sync) syncClient();
        return changed;
    }

    /** Where each hearth piece stands, in block-local coordinates: the four corners beside the fire. */
    public static final float[] HEARTH_X = {0.2f, 0.8f, 0.2f, 0.8f};
    public static final float[] HEARTH_Z = {0.2f, 0.2f, 0.8f, 0.8f};

    public NonNullList<ItemStack> hearth() {
        return hearth;
    }

    /** How far along a hearth piece is, 0 to 1; 0 for an empty spot or a fired piece. */
    public float firingProgress(int spot) {
        ItemStack piece = hearth.get(spot);
        if (piece.isEmpty() || !KilnFiring.isFireable(piece)) return 0;
        return Math.min(1, firingTicks[spot] / (float) Config.FIRING_TICKS.getAsInt());
    }

    /** Whether the hearth spot holds clay that is heating now. */
    public boolean isFiring(int spot) {
        ItemStack piece = hearth.get(spot);
        return !piece.isEmpty() && KilnFiring.isFireable(piece) && temperature >= FIRING_TEMPERATURE;
    }

    /** Stands one piece from {@code held} on the first free hearth spot. Returns the spot, or -1 if there is none. */
    public int placeOnHearth(ItemStack held) {
        for (int i = 0; i < HEARTH_SPOTS; i++) {
            if (!hearth.get(i).isEmpty()) continue;
            hearth.set(i, held.copyWithCount(1));
            firingTicks[i] = 0;
            held.shrink(1);
            syncClient();
            return i;
        }
        return -1;
    }

    /** Takes back the most recently set piece. */
    public ItemStack takeFromHearth() {
        for (int i = HEARTH_SPOTS - 1; i >= 0; i--) {
            if (hearth.get(i).isEmpty()) continue;
            ItemStack taken = hearth.get(i);
            hearth.set(i, ItemStack.EMPTY);
            firingTicks[i] = 0;
            syncClient();
            return taken;
        }
        return ItemStack.EMPTY;
    }

    public boolean hearthEmpty() {
        return hearth.stream().allMatch(ItemStack::isEmpty);
    }

    private void syncClient() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, hearth);
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
        for (int i = 0; i < HEARTH_SPOTS; i++) {
            hearth.set(i, ItemStack.EMPTY);
            firingTicks[i] = input.getIntOr("firing_" + i, 0);
        }
        input.child("hearth").ifPresent(child -> ContainerHelper.loadAllItems(child, hearth));
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
        ContainerHelper.saveAllItems(output.child("hearth"), hearth, true);
        for (int i = 0; i < HEARTH_SPOTS; i++) output.putInt("firing_" + i, firingTicks[i]);
    }

    // The client draws the hearth pieces and their glow, so it needs them and the temperature.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
