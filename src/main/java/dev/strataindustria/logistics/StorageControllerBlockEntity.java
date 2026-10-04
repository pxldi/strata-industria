package dev.strataindustria.logistics;

import dev.strataindustria.Config;
import dev.strataindustria.automation.FilterContents;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;

/**
 * The storage controller (tier 5 spec 12.3). It indexes every plain container that touches its pipe network
 * through a pipe face in storage mode and shows them as one list. It does not move items through the pipes;
 * it reaches into the containers directly. The index is looked up fresh when needed and kept for half a second.
 * Pushed into by anything on its other faces (it is a container with one slot that never holds anything), it
 * stores to a container that already holds that item first, then to any with room.
 */
public class StorageControllerBlockEntity extends BlockEntity implements WorldlyContainer, ElectricConsumer, MenuProvider {
    public static final int DRAW = 8;
    private static final int CACHE_TICKS = 10;

    /** An indexed container and the filter on the pipe face that reaches it. */
    public record Inv(Container container, ItemStack filter) {
        boolean allows(ItemStack stack) {
            return filter.isEmpty() || FilterContents.of(filter).test(stack);
        }
    }

    private double received;
    private boolean powered;
    private List<Inv> cache = List.of();
    private long cachedAt = Long.MIN_VALUE;

    public StorageControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5Logistics.STORAGE_CONTROLLER_BE.get(), pos, state);
    }

    public boolean powered() {
        return powered;
    }

    // ------------------------------------------------------------------ the index

    /** The containers in the storage, looked up through the pipes that touch the controller. */
    public List<Inv> inventories() {
        if (!(level instanceof ServerLevel server)) return List.of();
        long now = server.getGameTime();
        if (cachedAt != Long.MIN_VALUE && now >= cachedAt && now - cachedAt < CACHE_TICKS) return cache;
        cache = scan(server);
        cachedAt = now;
        return cache;
    }

    /** Forces the next lookup to look again, after something here changed the containers. */
    public void invalidate() {
        cachedAt = Long.MIN_VALUE;
    }

    private List<Inv> scan(ServerLevel level) {
        int max = Config.LOGISTICS_MAX_STORAGE_INVENTORIES.get();
        int budget = Config.LOGISTICS_MAX_PIPE_NETWORK.get();
        List<Inv> found = new ArrayList<>();
        Set<BlockPos> seenPipes = new HashSet<>();
        Set<BlockPos> seenInventories = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction side : Direction.values()) {
            BlockPos near = worldPosition.relative(side);
            if (level.getBlockState(near).getBlock() instanceof ItemPipeBlock && seenPipes.add(near)) queue.add(near);
        }
        while (!queue.isEmpty() && seenPipes.size() <= budget) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) continue;
            for (Direction side : Direction.values()) {
                ItemPipeBlock.Face face = ItemPipeBlock.face(state, side);
                BlockPos far = pos.relative(side);
                if (face == ItemPipeBlock.Face.PIPE) {
                    if (level.getBlockState(far).getBlock() instanceof ItemPipeBlock && seenPipes.add(far)) queue.add(far);
                } else if (face == ItemPipeBlock.Face.STORAGE && found.size() < max && seenInventories.add(far)) {
                    BlockEntity entity = level.getBlockEntity(far);
                    if (entity == null || !StorageRules.plain(entity)) continue;
                    // A double chest is one container reached from either half.
                    BlockState chest = level.getBlockState(far);
                    if (chest.getBlock() instanceof ChestBlock && chest.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        seenInventories.add(far.relative(ChestBlock.getConnectedDirection(chest)));
                    }
                    Container container = HopperBlockEntity.getContainerAt(level, far);
                    if (container != null) found.add(new Inv(container, pipe.filter(side)));
                }
            }
        }
        return List.copyOf(found);
    }

    /** Everything stored, by kind, in the order first met. */
    public List<StorageEntry> index() {
        Map<ItemKey, Long> counts = new LinkedHashMap<>();
        Map<ItemKey, ItemStack> samples = new LinkedHashMap<>();
        for (Inv inv : inventories()) {
            Container container = inv.container();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                ItemKey key = new ItemKey(stack);
                counts.merge(key, (long) stack.getCount(), Long::sum);
                samples.putIfAbsent(key, stack.copyWithCount(1));
            }
        }
        List<StorageEntry> list = new ArrayList<>(counts.size());
        for (var entry : counts.entrySet()) list.add(new StorageEntry(samples.get(entry.getKey()), entry.getValue()));
        return list;
    }

    /** One of the first kind of item that {@code wanted} accepts, with a count of one, or empty. */
    public ItemStack findFirst(Predicate<ItemStack> wanted) {
        if (!powered) return ItemStack.EMPTY;
        for (Inv inv : inventories()) {
            Container container = inv.container();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty() && wanted.test(stack)) return stack.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Takes up to {@code count} of the kind {@code proto} out of the containers. */
    public ItemStack take(ItemStack proto, int count) {
        if (!powered) return ItemStack.EMPTY;
        ItemStack taken = ItemStack.EMPTY;
        int left = Math.min(count, proto.getMaxStackSize());
        for (Inv inv : inventories()) {
            if (left <= 0) break;
            Container container = inv.container();
            for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, proto)) continue;
                ItemStack part = container.removeItem(slot, Math.min(left, stack.getCount()));
                if (part.isEmpty()) continue;
                if (taken.isEmpty()) taken = part;
                else taken.grow(part.getCount());
                left -= part.getCount();
            }
            container.setChanged();
        }
        return taken;
    }

    /** Stores {@code stack}: first where that item already is, then anywhere with room. Returns what did not fit. */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty() || !powered) return stack;
        ItemStack left = stack.copy();
        for (int pass = 0; pass < 2 && !left.isEmpty(); pass++) {
            for (Inv inv : inventories()) {
                if (left.isEmpty()) break;
                if (!inv.allows(left) || (pass == 0) != holds(inv.container(), left)) continue;
                left = HopperBlockEntity.addItem(null, inv.container(), left, null);
                inv.container().setChanged();
            }
        }
        return left;
    }

    private static boolean holds(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (ItemStack.isSameItemSameComponents(container.getItem(slot), stack)) return true;
        }
        return false;
    }

    /** How many of {@code stack} the containers could take in all. */
    public int room(ItemStack stack) {
        if (!powered) return 0;
        long room = 0;
        for (Inv inv : inventories()) {
            if (!inv.allows(stack)) continue;
            Container container = inv.container();
            int limit = container.getMaxStackSize(stack);
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack there = container.getItem(slot);
                if (there.isEmpty()) {
                    if (container.canPlaceItem(slot, stack)) room += limit;
                } else if (ItemStack.isSameItemSameComponents(there, stack)) {
                    room += Math.max(0, Math.min(limit, there.getMaxStackSize()) - there.getCount());
                }
            }
        }
        return (int) Math.min(room, Integer.MAX_VALUE);
    }

    // ------------------------------------------------------------------ container (pushes from outside)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty() || level == null || level.isClientSide()) return;
        ItemStack left = insert(stack);
        if (!left.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, left);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {}

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return room(stack) >= stack.getCount();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    // ------------------------------------------------------------------ power

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public ElectricTier tier() {
        return ElectricTier.MV;
    }

    @Override
    public double request() {
        return DRAW;
    }

    @Override
    public void receive(double amount) {
        received += amount;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StorageControllerBlockEntity controller) {
        controller.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        ElectricNetwork.Report report = ElectricNetworks.report(level, worldPosition);
        double fraction = report.status().fault() ? 0 : Math.min(1.0, received / DRAW);
        received = 0;
        boolean was = powered;
        powered = fraction >= 0.5;
        if (powered != was || state.getValue(StorageControllerBlock.ACTIVE) != powered) {
            level.setBlock(worldPosition, state.setValue(StorageControllerBlock.ACTIVE, powered), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ screen

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + dev.strataindustria.StrataIndustria.MOD_ID + ".storage_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StorageControllerMenu(id, inventory, worldPosition, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ElectricNetworks.markDirty(level, worldPosition);
    }
}
