package dev.strataindustria.fire;

import dev.strataindustria.Config;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Open fire state (redesign R6): no screen. Fuel fed by hand adds to one pool of burning time that shows as
 * the size of the fire (0 to 3), the temperature climbs towards the hottest fuel fed and falls once the
 * fuel is spent. The fire goes out when it has cooled back to ambient, or when rain falls on it.
 * <p>
 * Four spots on the hearth stones round the fire hold unfired clay or raw food. Clay that stays in the
 * heat of 450 °C or more for {@link Config#FIRING_TICKS} ticks comes out fired and stays where it is;
 * food that stays in the heat of 200 °C or more for its cooking time is done and hops off the stones.
 * There is no way to ruin a piece.
 */
public class FirePitBlockEntity extends BlockEntity {
    public static final float AMBIENT = 20.0f;
    /** Food cooks with the fire at or above this temperature. */
    public static final float COOK_TEMPERATURE = 200.0f;
    /** Pieces on the hearth stones. */
    public static final int HEARTH_SPOTS = 4;
    /** Clay on the hearth fires while the fire is at or above this temperature. */
    public static final float FIRING_TEMPERATURE = 450.0f;
    /** The most burning time the fire holds: three logs. */
    public static final int MAX_BURN = 3600;
    /** Burning time up to which the fire counts as size 1 and size 2; above the second it is size 3. */
    public static final int SMALL_BURN = 600;
    public static final int MEDIUM_BURN = 1800;
    /** 10 °C a second while burning, 5 °C a second while cooling. */
    static final float HEAT_PER_TICK = 0.5f;
    static final float COOL_PER_TICK = 0.25f;

    private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> cooking =
            RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
    private int burnLeft;
    private float temperature = AMBIENT;
    private float maxTemperature = AMBIENT;
    private final NonNullList<ItemStack> hearth = NonNullList.withSize(HEARTH_SPOTS, ItemStack.EMPTY);
    /** Ticks each hearth piece has been in the heat, and how many it needs. Both reach the client. */
    private final int[] heatTicks = new int[HEARTH_SPOTS];
    private final int[] heatNeeded = new int[HEARTH_SPOTS];

    public FirePitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIRE_PIT.get(), pos, state);
    }

    /** The size of the fire for this much burning time: 0 (none) to 3. */
    public static int sizeFor(int burn) {
        if (burn <= 0) return 0;
        return burn <= SMALL_BURN ? 1 : burn <= MEDIUM_BURN ? 2 : 3;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FirePitBlockEntity pit) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        boolean lit = state.getValue(FirePitBlock.LIT);
        boolean changed = false;

        if (lit) {
            if (pit.burnLeft > 0) pit.burnLeft--;
            if (pit.burnLeft <= 0) pit.maxTemperature = AMBIENT;
            if (level.getGameTime() % 20 == 0 && Ignitable.rainedOn(level, pos) && level.getRandom().nextInt(3) == 0) {
                pit.maxTemperature = AMBIENT;
                state = FirePitBlock.extinguish(level, pos, state, true);
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
            state = FirePitBlock.extinguish(level, pos, state, false);
            changed = true;
        }

        // The fire's size follows the burning time: it grows as you feed it and settles as it burns down.
        int size = sizeFor(pit.burnLeft);
        if (state.getValue(FirePitBlock.FUEL) != size) {
            level.setBlock(pos, state.setValue(FirePitBlock.FUEL, size), Block.UPDATE_ALL);
            changed = true;
        }

        if (pit.heatHearth(serverLevel, pos)) changed = true;
        if (changed) setChanged(level, pos, state);
    }

    /** Adds burning time. Returns false when the fire already holds as much as it can. */
    public boolean feed(FirePitFuel fuel) {
        if (burnLeft + fuel.burnTicks() > MAX_BURN) return false;
        boolean wasOut = burnLeft <= 0;
        burnLeft += fuel.burnTicks();
        maxTemperature = wasOut ? fuel.maxTemperature() : Math.max(maxTemperature, fuel.maxTemperature());
        setChanged();
        return true;
    }

    public int burnLeft() {
        return burnLeft;
    }

    public int size() {
        return sizeFor(burnLeft);
    }

    /** Whether there is something to light: fuel laid or still burning. */
    public boolean hasFuel() {
        return burnLeft > 0;
    }

    /** Pieces in the heat count up; a finished one rings (clay) or hops off the stones (food). */
    private boolean heatHearth(ServerLevel level, BlockPos pos) {
        boolean changed = false;
        boolean sync = false;
        for (int i = 0; i < HEARTH_SPOTS; i++) {
            ItemStack piece = hearth.get(i);
            if (piece.isEmpty()) continue;
            boolean clay = KilnFiring.isFireable(piece);
            Optional<RecipeHolder<CampfireCookingRecipe>> food = clay ? Optional.empty() : cooking.getRecipeFor(new SingleRecipeInput(piece), level);
            if (!clay && food.isEmpty()) continue;
            if (temperature < (clay ? FIRING_TEMPERATURE : COOK_TEMPERATURE)) continue;
            int needed = clay ? Config.FIRING_TICKS.getAsInt() : food.get().value().cookingTime();
            changed = true;
            if (heatTicks[i] == 0 || heatNeeded[i] != needed) {
                heatNeeded[i] = needed;
                sync = true;
            }
            if (++heatTicks[i] % 20 == 0) sync = true;
            if (heatTicks[i] < needed) continue;
            heatTicks[i] = 0;
            sync = true;
            double x = pos.getX() + HEARTH_X[i], y = pos.getY() + HEARTH_Y + 0.1, z = pos.getZ() + HEARTH_Z[i];
            if (clay) {
                hearth.set(i, KilnFiring.fire(piece));
                level.playSound(null, pos, ModSounds.POTTERY_RING.get(), SoundSource.BLOCKS, 0.9f, 0.9f + 0.15f * i);
                level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 10, 0.12, 0.1, 0.12, 0.4);
                level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 6, 0.1, 0.05, 0.1, 0.03);
                Journal.awardNear(level, pos, Journal.POTTERY_FIRED);
            } else {
                ItemStack done = food.get().value().assemble(new SingleRecipeInput(piece));
                hearth.set(i, ItemStack.EMPTY);
                heatNeeded[i] = 0;
                popOff(level, x, y, z, done, i);
                level.playSound(null, pos, ModSounds.FIRE_PIT_READY.get(), SoundSource.BLOCKS, 0.9f, 0.95f + 0.1f * i);
                level.sendParticles(ParticleTypes.CLOUD, x, y + 0.1, z, 8, 0.1, 0.08, 0.1, 0.03);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.2, z, 3, 0.15, 0.1, 0.15, 0);
            }
        }
        if (sync) syncClient();
        return changed;
    }

    /** Cooked food hops off its stone and lands beside the fire, on the side it sat on. */
    private static void popOff(ServerLevel level, double x, double y, double z, ItemStack stack, int spot) {
        double dx = (HEARTH_X[spot] - 0.5) * 0.25, dz = (HEARTH_Z[spot] - 0.5) * 0.25;
        ItemEntity item = new ItemEntity(level, x, y, z, stack, dx, 0.28, dz);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /** Where each hearth piece stands, in block-local coordinates: the four flat stones in the corners. */
    public static final float[] HEARTH_X = {0.2f, 0.8f, 0.2f, 0.8f};
    public static final float[] HEARTH_Z = {0.2f, 0.2f, 0.8f, 0.8f};
    /** Height of the top of the hearth stones. */
    public static final float HEARTH_Y = 1.5f / 16;

    /** The hearth spot nearest to a point in block-local coordinates. */
    public static int spotAt(double x, double z) {
        return (z > 0.5 ? 2 : 0) + (x > 0.5 ? 1 : 0);
    }

    public NonNullList<ItemStack> hearth() {
        return hearth;
    }

    /** Whether the stack could stand on the hearth: unfired clay, or food a fire can cook. */
    public boolean canHold(ItemStack stack, Level level) {
        if (KilnFiring.isFireable(stack)) return true;
        return level instanceof ServerLevel server && cooking.getRecipeFor(new SingleRecipeInput(stack), server).isPresent();
    }

    /** How far along a hearth piece is, 0 to 1; 0 for an empty spot or one not heating. */
    public float firingProgress(int spot) {
        if (hearth.get(spot).isEmpty() || heatNeeded[spot] <= 0) return 0;
        return Math.min(1, heatTicks[spot] / (float) heatNeeded[spot]);
    }

    /** Whether the hearth spot holds a piece that is heating now. */
    public boolean isFiring(int spot) {
        return !hearth.get(spot).isEmpty() && heatTicks[spot] > 0;
    }

    /** Whether the piece on the spot is raw clay (it glows) rather than food. */
    public boolean isClay(int spot) {
        return KilnFiring.isFireable(hearth.get(spot));
    }

    /**
     * Stands one piece from {@code held} on the hearth, on {@code preferred} when that spot is free and on the
     * first free one otherwise. Returns the spot, or -1 if there is none.
     */
    public int placeOnHearth(ItemStack held, int preferred) {
        int spot = preferred >= 0 && preferred < HEARTH_SPOTS && hearth.get(preferred).isEmpty() ? preferred : -1;
        for (int i = 0; spot < 0 && i < HEARTH_SPOTS; i++) if (hearth.get(i).isEmpty()) spot = i;
        if (spot < 0) return -1;
        hearth.set(spot, held.copyWithCount(1));
        heatTicks[spot] = 0;
        heatNeeded[spot] = 0;
        held.shrink(1);
        syncClient();
        return spot;
    }

    public int placeOnHearth(ItemStack held) {
        return placeOnHearth(held, -1);
    }

    /** Takes the piece off a spot; empty when the spot is bare. */
    public ItemStack takeFrom(int spot) {
        ItemStack taken = hearth.get(spot);
        if (taken.isEmpty()) return ItemStack.EMPTY;
        hearth.set(spot, ItemStack.EMPTY);
        heatTicks[spot] = 0;
        heatNeeded[spot] = 0;
        syncClient();
        return taken;
    }

    /** Takes back the most recently set piece. */
    public ItemStack takeFromHearth() {
        for (int i = HEARTH_SPOTS - 1; i >= 0; i--) {
            if (!hearth.get(i).isEmpty()) return takeFrom(i);
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

    public float temperature() {
        return temperature;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnLeft = input.getIntOr("burn_left", 0);
        temperature = input.getFloatOr("temperature", AMBIENT);
        maxTemperature = input.getFloatOr("max_temperature", AMBIENT);
        for (int i = 0; i < HEARTH_SPOTS; i++) {
            hearth.set(i, ItemStack.EMPTY);
            heatTicks[i] = input.getIntOr("firing_" + i, 0);
            heatNeeded[i] = input.getIntOr("needed_" + i, 0);
        }
        input.child("hearth").ifPresent(child -> ContainerHelper.loadAllItems(child, hearth));
        // Pits saved with the old screen: the fuel in its slot goes on the fire, the food goes on the stones.
        NonNullList<ItemStack> legacy = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, legacy);
        ItemStack fuel = legacy.get(0);
        for (int n = fuel.getCount(); n > 0; n--) {
            Optional<FirePitFuel> burning = FirePitFuel.of(fuel);
            if (burning.isEmpty() || !feed(burning.get())) break;
        }
        ItemStack food = legacy.get(1);
        if (!food.isEmpty() && hearth.stream().anyMatch(ItemStack::isEmpty)) placeOnHearth(food);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burn_left", burnLeft);
        output.putFloat("temperature", temperature);
        output.putFloat("max_temperature", maxTemperature);
        ContainerHelper.saveAllItems(output.child("hearth"), hearth, true);
        for (int i = 0; i < HEARTH_SPOTS; i++) {
            output.putInt("firing_" + i, heatTicks[i]);
            output.putInt("needed_" + i, heatNeeded[i]);
        }
    }

    // The client draws the hearth pieces and their glow.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
