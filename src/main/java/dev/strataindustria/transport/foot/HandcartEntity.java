package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The handcart (spec 4.3): eighteen slots on two wheels. Sneak-right-click takes up the shafts and the cart trails
 * its puller on a short tether; do it again, or take a hit, to let go. It climbs only the steps the puller can
 * walk up, and it is nothing special on rails.
 */
public class HandcartEntity extends VehicleEntity implements ContainerEntity {
    public static final int SLOTS = 18;
    /** Blocks between puller and cart. */
    public static final double TETHER = 1.5;
    /** The most the cart moves in one tick: a little more than a walk. */
    private static final double MAX_STEP = 0.3;
    /** Distance beyond which the shafts slip out of the puller's hands. */
    private static final double SNAP = 8.0;
    /** Wheel radius in blocks, for turning them. */
    private static final double WHEEL_RADIUS = 5.0 / 16.0;
    /** Blocks hauled in one go that count for the handcart goal. */
    public static final int HAUL_GOAL = 200;

    private static final EntityDataAccessor<Boolean> DATA_PULLED = SynchedEntityData.defineId(HandcartEntity.class, EntityDataSerializers.BOOLEAN);

    /** Who is pulling which cart; server side only. */
    private static final Map<UUID, HandcartEntity> PULLERS = new HashMap<>();

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private @Nullable ResourceKey<LootTable> lootTable;
    private long lootTableSeed;
    private @Nullable UUID puller;
    private double hauled;
    private long lastBlockedMessage = -100;
    private float wheelAngle;
    private float wheelAngleO;

    public HandcartEntity(EntityType<? extends HandcartEntity> type, Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- pulling

    public boolean isPulled() {
        return entityData.get(DATA_PULLED);
    }

    public @Nullable UUID puller() {
        return puller;
    }

    /** The cart the player is pulling, if any (server side). */
    public static @Nullable HandcartEntity pulledBy(Player player) {
        return PULLERS.get(player.getUUID());
    }

    public static boolean isPulling(Player player) {
        return PULLERS.containsKey(player.getUUID());
    }

    public static void forgetAll() {
        PULLERS.clear();
    }

    /** Takes up the shafts. A player who already pulls a cart lets that one go first. */
    public void grab(ServerPlayer player) {
        HandcartEntity other = PULLERS.get(player.getUUID());
        if (other != null) other.release();
        if (puller != null) release();
        puller = player.getUUID();
        hauled = 0;
        PULLERS.put(puller, this);
        entityData.set(DATA_PULLED, true);
        Burden.setHauling(player, true);
        level().playSound(null, getX(), getY(), getZ(), FootRegistry.HANDCART_SHAFTS.get(), SoundSource.NEUTRAL, 0.7f, 1.0f);
    }

    /** Lets the shafts down. */
    public void release() {
        if (puller == null) return;
        UUID was = puller;
        puller = null;
        PULLERS.remove(was, this);
        entityData.set(DATA_PULLED, false);
        Player player = level().getPlayerByUUID(was);
        if (player != null) Burden.setHauling(player, false);
        level().playSound(null, getX(), getY(), getZ(), FootRegistry.HANDCART_SHAFTS.get(), SoundSource.NEUTRAL, 0.5f, 0.8f);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive()) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (puller != null && puller.equals(player.getUUID())) release();
                else if (puller == null) grab(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        InteractionResult result = interactWithContainerVehicle(player);
        if (result.consumesAction() && player.level() instanceof ServerLevel server) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            Player holder = puller == null ? null : server.getPlayerByUUID(puller);
            if (puller != null && (holder == null || !holder.isAlive() || holder.isSpectator()
                    || holder.distanceToSqr(this) > SNAP * SNAP)) {
                release();
                holder = null;
            }
            if (holder instanceof ServerPlayer player) follow(server, player);
            else settle();
        }
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        Vec3 before = position();
        move(MoverType.SELF, getDeltaMovement());
        if (puller != null) countHaul(before);
        spinWheels(before);
    }

    /** Turns the wheels by how far the cart moved along its length. */
    private void spinWheels(Vec3 before) {
        double yaw = Math.toRadians(getYRot());
        double along = (getX() - before.x) * -Math.sin(yaw) + (getZ() - before.z) * Math.cos(yaw);
        wheelAngleO = wheelAngle;
        wheelAngle += (float) (along / WHEEL_RADIUS);
    }

    /** Wheel turn in radians, for the renderer. */
    public float wheelAngle(float partialTicks) {
        return Mth.lerp(partialTicks, wheelAngleO, wheelAngle);
    }

    /** Draws the cart after the puller on a tether of {@link #TETHER} blocks, turning it to face them. */
    private void follow(ServerLevel server, ServerPlayer player) {
        double dx = player.getX() - getX();
        double dz = player.getZ() - getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        Vec3 motion = getDeltaMovement();
        if (distance > TETHER) {
            double step = Math.min(distance - TETHER, MAX_STEP);
            setDeltaMovement(dx / distance * step, motion.y, dz / distance * step);
            float yaw = (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI));
            setYRot(Mth.rotLerp(0.35f, getYRot(), yaw));
            if (horizontalCollision && distance > TETHER + 0.7 && server.getGameTime() - lastBlockedMessage > 60) {
                lastBlockedMessage = server.getGameTime();
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".handcart.blocked"));
            }
            if (tickCount % 8 == 0) {
                server.playSound(null, getX(), getY(), getZ(), FootRegistry.HANDCART_ROLL.get(), SoundSource.NEUTRAL,
                        0.4f, 0.9f + (float) step);
            }
        } else {
            setDeltaMovement(motion.x * 0.5, motion.y, motion.z * 0.5);
        }
    }

    /** With nobody at the shafts the cart stops where it is. */
    private void settle() {
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.4, motion.y, motion.z * 0.4);
    }

    private void countHaul(Vec3 before) {
        if (hauled >= HAUL_GOAL) return;
        Vec3 now = position();
        hauled += Math.sqrt((now.x - before.x) * (now.x - before.x) + (now.z - before.z) * (now.z - before.z));
        if (hauled >= HAUL_GOAL && puller != null && level().getPlayerByUUID(puller) instanceof ServerPlayer player) {
            Journal.award(player, Journal.HANDCART_HAUL);
        }
    }

    /** Distance hauled in this pull, for tests. */
    public double hauled() {
        return hauled;
    }

    /** A cart climbs a slab, a stair or a snow layer, not a whole block (spec 4.3). */
    @Override
    public float maxUpStep() {
        return 0.6f;
    }

    // ---------------------------------------------------------------- body

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PULLED, false);
    }

    /** Only a player's blow breaks it, and it is never despawned. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isRemoved()) return true;
        if (source.getEntity() instanceof Player) {
            destroy(level, source);
            return true;
        }
        return false;
    }

    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        release();
        destroy(level, getDropItem());
        chestVehicleDestroyed(source, level, this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            release();
            if (reason.shouldDestroy()) Containers.dropContents(level(), this, this);
        }
        super.remove(reason);
    }

    @Override
    protected Item getDropItem() {
        return FootRegistry.HANDCART.get();
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(FootRegistry.HANDCART.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        addChestVehicleSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        readChestVehicleSaveData(input);
    }

    // ---------------------------------------------------------------- container

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (lootTable != null && player.isSpectator()) return null;
        unpackChestVehicleLootTable(inventory.player);
        return new ChestMenu(MenuType.GENERIC_9x2, containerId, inventory, this, 2);
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? getCustomName() : Component.translatable("entity." + StrataIndustria.MOD_ID + ".handcart");
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getChestVehicleItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return removeChestVehicleItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeChestVehicleItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setChestVehicleItem(slot, stack);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return getChestVehicleSlot(slot);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return isChestVehicleStillValid(player);
    }

    @Override
    public void clearContent() {
        clearChestVehicleContent();
    }

    @Override
    public @Nullable ResourceKey<LootTable> getContainerLootTable() {
        return lootTable;
    }

    @Override
    public void setContainerLootTable(@Nullable ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public long getContainerLootTableSeed() {
        return lootTableSeed;
    }

    @Override
    public void setContainerLootTableSeed(long seed) {
        lootTableSeed = seed;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return items;
    }

    @Override
    public void clearItemStacks() {
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    }
}
