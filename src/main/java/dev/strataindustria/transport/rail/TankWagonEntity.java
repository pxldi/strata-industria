package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidBuckets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The tank wagon (outposts spec 7.2): a horizontal steel tank holding 16 000 mB of one fluid. It fills and drains
 * through a wagon fluid port at a station; a bucket works too. It has no slots, so a stop reads its fluid level
 * where it would read the fill of a tub.
 */
public class TankWagonEntity extends MineTubEntity {
    public static final int CAPACITY = 16_000;
    public static final int BUCKET = 1000;
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(TankWagonEntity.class, EntityDataSerializers.INT);

    private Fluid fluid = Fluids.EMPTY;
    private int amount;

    public TankWagonEntity(EntityType<? extends TankWagonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_LEVEL, 0);
    }

    @Override
    protected Item getDropItem() {
        return RailwayRegistry.TANK_WAGON.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RailwayRegistry.TANK_WAGON.get());
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        throw new IllegalStateException("A tank wagon has no slots");
    }

    @Override
    protected boolean tips() {
        return false;
    }

    // ---------------------------------------------------------------- the tank

    public Fluid fluid() {
        return amount > 0 ? fluid : Fluids.EMPTY;
    }

    public int amount() {
        return amount;
    }

    /** How full the tank looks, 0 to 16, for the renderer's gauge. */
    public int gauge() {
        return entityData.get(DATA_LEVEL);
    }

    /** Takes up to {@code mb} of {@code in}; returns how much went in, or would, when simulating. One fluid at a time. */
    public int fill(Fluid in, int mb, boolean simulate) {
        if (in.isSame(Fluids.EMPTY) || mb <= 0) return 0;
        if (amount > 0 && !fluid.isSame(in)) return 0;
        int take = Math.min(mb, CAPACITY - amount);
        if (take > 0 && !simulate) {
            fluid = in;
            amount += take;
            sync();
        }
        return Math.max(take, 0);
    }

    /** Gives up to {@code mb}; returns how much came out, or would, when simulating. */
    public int drain(int mb, boolean simulate) {
        int give = Math.min(mb, amount);
        if (give > 0 && !simulate) {
            amount -= give;
            if (amount == 0) fluid = Fluids.EMPTY;
            sync();
        }
        return Math.max(give, 0);
    }

    private void sync() {
        entityData.set(DATA_LEVEL, (int) Math.ceil(16.0 * amount / CAPACITY));
    }

    // ---------------------------------------------------------------- stop rules

    @Override
    public boolean isEmpty() {
        return amount <= 0;
    }

    @Override
    public boolean cannotTakeMore(ServerLevel server) {
        return amount >= CAPACITY;
    }

    /** The level in steps of fifty millibuckets, so a port at work keeps "until idle" from firing. */
    @Override
    public int contentSignature() {
        return amount / 50 + (amount > 0 ? BuiltInRegistries.FLUID.getId(fluid) * 1000 : 0);
    }

    @Override
    public double fillAmount() {
        return 9.0 * amount / CAPACITY;
    }

    @Override
    public int fillCapacity() {
        return 9;
    }

    // ---------------------------------------------------------------- using

    /** A bucket fills or empties it a bucket at a time; a bare hand reads the level. */
    @Override
    protected InteractionResult openLoad(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        Fluid bucketFluid = FluidBuckets.fluidOf(held);
        if (bucketFluid != null) {
            if (fill(bucketFluid, BUCKET, true) < BUCKET) return InteractionResult.PASS;
            if (!level().isClientSide()) {
                fill(bucketFluid, BUCKET, false);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.BUCKET) && amount >= BUCKET && fluid.getBucket() != Items.AIR) {
            if (!level().isClientSide()) {
                ItemStack filled = new ItemStack(fluid.getBucket());
                drain(BUCKET, false);
                if (!player.getAbilities().instabuild) held.shrink(1);
                if (!player.getInventory().add(filled)) player.spawnAtLocation((ServerLevel) level(), filled);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide()) player.sendOverlayMessage(readout());
        return InteractionResult.SUCCESS;
    }

    public Component readout() {
        String key = StrataIndustria.MOD_ID + ".tank_wagon.";
        if (amount <= 0) return Component.translatable(key + "empty", CAPACITY);
        return Component.translatable(key + "holds", amount, CAPACITY, fluid.getFluidType().getDescription());
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (amount > 0) {
            output.store("Fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
            output.putInt("Amount", amount);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        amount = Math.min(input.getIntOr("Amount", 0), CAPACITY);
        fluid = input.read("Fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        if (fluid.isSame(Fluids.EMPTY)) amount = 0;
        sync();
    }
}
