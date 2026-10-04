package dev.strataindustria.automation;

import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * A chute's one slot. Every 4 ticks one item falls on: into the next chute, into whatever inventory is
 * below (through its top face), or out of the bottom when nothing is there. A solid block with no
 * inventory holds the line. Items can be put in from any side but the bottom and never taken out.
 */
public class ChuteBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int INTERVAL = 4;
    private static final int[] SLOTS = {0};

    private ItemStack held = ItemStack.EMPTY;
    private int cooldown;
    private boolean dirty;

    public ChuteBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.CHUTE.get(), pos, state);
    }

    public ItemStack held() {
        return held;
    }

    /** Whether some of {@code stack} would fit. */
    public boolean accepts(ItemStack stack) {
        return held.isEmpty() || ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize();
    }

    /** Takes what fits of {@code stack}, shrinking it. */
    public void insert(ItemStack stack) {
        if (held.isEmpty()) {
            held = stack.split(stack.getMaxStackSize());
        } else if (ItemStack.isSameItemSameComponents(held, stack)) {
            int move = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
            held.grow(move);
            stack.shrink(move);
        }
        changed();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChuteBlockEntity chute) {
        chute.tick((ServerLevel) level, pos);
    }

    private void tick(ServerLevel level, BlockPos pos) {
        if (dirty && level.getGameTime() % 2 == 0) {
            dirty = false;
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        if (--cooldown > 0) return;
        cooldown = INTERVAL;
        collect(level, pos);
        if (!held.isEmpty()) drop(level, pos);
    }

    /** Items lying in the tube or on its lip fall in. */
    private void collect(ServerLevel level, BlockPos pos) {
        AABB mouth = new AABB(pos).setMaxY(pos.getY() + 1.25);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, mouth, ItemEntity::isAlive);
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem().copy();
            if (!accepts(stack)) continue;
            insert(stack);
            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);
        }
    }

    /** One item on down. */
    private void drop(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockEntity target = level.getBlockEntity(below);
        ItemStack one = held.copyWithCount(1);
        if (target instanceof Container container) {
            ItemStack left = HopperBlockEntity.addItem(this, container, one, Direction.UP);
            if (!left.isEmpty()) return;
            container.setChanged();
            if (!(target instanceof ChuteBlockEntity)) clatter(level, pos);
        } else if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() - 0.3, pos.getZ() + 0.5, one, 0.0, -0.05, 0.0);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            clatter(level, pos);
        } else {
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) held = ItemStack.EMPTY;
        changed();
    }

    private void clatter(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, Tier4Sounds.CHUTE_DROP.get(), SoundSource.BLOCKS, 0.25f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    private void changed() {
        dirty = true;
        setChanged();
    }

    // ------------------------------------------------------------------ container

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN;
    }

    /** Spec 13.3: a chute never gives items up to whatever pulls on it. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return held.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? held : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || held.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = held.split(count);
        changed();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack taken = held;
        held = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        held = stack;
        changed();
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        held = ItemStack.EMPTY;
        changed();
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        held = in.read("held", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!held.isEmpty()) out.store("held", ItemStack.OPTIONAL_CODEC, held);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
