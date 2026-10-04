package dev.strataindustria.logistics;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.automation.FilterContents;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Pulls items out of the inventory in front of the mouth and puts them into the item pipe (or storage
 * controller) behind it (tier 5 spec 12.2). It only pulls what the network would take, so nothing is lifted
 * out that has nowhere to go. 4 items per second, 16 for the fast one, for 2 or 8 J/t from an adjacent cable
 * or from the machine it faces; below full power it slows in proportion. A redstone signal pauses it.
 */
public class PipeExtractorBlockEntity extends BlockEntity implements ElectricConsumer {
    public static final int DRAW = 2, FAST_DRAW = 8;
    public static final int RATE = 4, FAST_RATE = 16;
    /** Ticks it stays lit after moving an item. */
    private static final int GLOW = 10;
    /** Ticks between looks for something to pull when it has found nothing. */
    private static final int LOOK = 20;
    private static final int SOUND_INTERVAL = 20;

    public enum Status {
        NO_POWER, PAUSED, NO_INVENTORY, NO_PIPE, NOTHING, PULLING;

        public String key() {
            return StrataIndustria.MOD_ID + ".pipe_extractor.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private ItemStack filter = ItemStack.EMPTY;
    private double budget;
    private double received;
    private double power;
    private boolean wanted;
    private int look;
    private long lastMoved = -1000;
    private long lastSound = -1000;
    private Status status = Status.NOTHING;

    public PipeExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5Logistics.PIPE_EXTRACTOR_BE.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(PipeExtractorBlock.FACING);
    }

    private boolean fast() {
        return getBlockState().getBlock() instanceof PipeExtractorBlock block && block.fast();
    }

    public int draw() {
        return fast() ? FAST_DRAW : DRAW;
    }

    public int rate() {
        return fast() ? FAST_RATE : RATE;
    }

    public Status status() {
        return status;
    }

    // ------------------------------------------------------------------ filter

    public boolean hasFilter() {
        return !filter.isEmpty();
    }

    public ItemStack setFilter(ItemStack stack) {
        ItemStack old = filter;
        filter = stack;
        setChanged();
        look = 0;
        return old;
    }

    public boolean allows(ItemStack stack) {
        return filter.isEmpty() || FilterContents.of(filter).test(stack);
    }

    // ------------------------------------------------------------------ power

    /** It is wired into any network it touches, on every face, so a machine's own cable powers it. */
    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    /** MV so that it runs on either network; it never draws more than 8 J/t. */
    @Override
    public ElectricTier tier() {
        return ElectricTier.MV;
    }

    @Override
    public double request() {
        return wanted ? draw() : 0;
    }

    @Override
    public void receive(double amount) {
        received += amount;
    }

    // ------------------------------------------------------------------ work

    public static void serverTick(Level level, BlockPos pos, BlockState state, PipeExtractorBlockEntity extractor) {
        extractor.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        long now = level.getGameTime();
        ElectricNetwork.Report report = ElectricNetworks.report(level, worldPosition);
        power = report.status().fault() ? 0 : Math.min(1.0, received / Math.max(1, draw()));
        received = 0;
        int moved = 0;
        if (level.hasNeighborSignal(worldPosition)) {
            status = Status.PAUSED;
            wanted = false;
        } else if (!hasSource(level)) {
            status = Status.NO_INVENTORY;
            wanted = false;
        } else if (!hasTarget(level)) {
            status = Status.NO_PIPE;
            wanted = false;
        } else {
            if (!wanted && --look <= 0) {
                wanted = pull(level, true);
                look = LOOK;
            }
            if (!wanted) {
                status = Status.NOTHING;
            } else if (power <= 0) {
                status = Status.NO_POWER;
            } else {
                budget += rate() / 20.0 * power;
                while (budget >= 1.0) {
                    if (!pull(level, false)) {
                        wanted = false;
                        budget = 0;
                        break;
                    }
                    budget -= 1.0;
                    moved++;
                }
                budget = Math.min(budget, 1.0);
                status = Status.PULLING;
            }
        }
        if (moved > 0) {
            lastMoved = now;
            if (now - lastSound >= SOUND_INTERVAL) {
                lastSound = now;
                level.playSound(null, worldPosition, Tier5Logistics.PIPE_EXTRACT.get(), SoundSource.BLOCKS, 0.25f, 0.95f + level.getRandom().nextFloat() * 0.1f);
            }
        }
        boolean lit = now - lastMoved < GLOW;
        if (state.getValue(PipeExtractorBlock.ACTIVE) != lit) level.setBlock(worldPosition, state.setValue(PipeExtractorBlock.ACTIVE, lit), Block.UPDATE_CLIENTS);
    }

    private boolean hasSource(ServerLevel level) {
        BlockPos at = worldPosition.relative(facing());
        return level.getBlockEntity(at) instanceof StorageControllerBlockEntity || ItemPipeBlock.isInventory(level, at);
    }

    private boolean hasTarget(ServerLevel level) {
        BlockEntity back = level.getBlockEntity(worldPosition.relative(facing().getOpposite()));
        return back instanceof ItemPipeBlockEntity || back instanceof StorageControllerBlockEntity;
    }

    /** Moves one item from the inventory in front into the pipe behind, or only checks that it could. Returns whether there was one. */
    private boolean pull(ServerLevel level, boolean simulate) {
        Direction facing = facing();
        BlockEntity back = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));
        BlockPos sourcePos = worldPosition.relative(facing);
        if (level.getBlockEntity(sourcePos) instanceof StorageControllerBlockEntity controller) {
            ItemStack found = controller.findFirst(stack -> allows(stack) && accepts(back, stack, facing));
            if (found.isEmpty()) return false;
            if (simulate) return true;
            ItemStack taken = controller.take(found, 1);
            if (taken.isEmpty()) return false;
            if (!deliver(back, taken, facing)) {
                controller.insert(taken);
                return false;
            }
            return true;
        }
        if (!ItemPipeBlock.isInventory(level, sourcePos)) return false;
        Container source = HopperBlockEntity.getContainerAt(level, sourcePos);
        if (source == null) return false;
        Direction face = facing.getOpposite();
        int[] slots = source instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(face) : null;
        int count = slots == null ? source.getContainerSize() : slots.length;
        for (int n = 0; n < count; n++) {
            int slot = slots == null ? n : slots[n];
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty() || !allows(stack)) continue;
            if (source instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, stack, face)) continue;
            ItemStack one = stack.copyWithCount(1);
            if (!accepts(back, one, facing)) continue;
            if (simulate) return true;
            ItemStack taken = source.removeItem(slot, 1);
            if (taken.isEmpty()) continue;
            source.setChanged();
            if (!deliver(back, taken, facing)) {
                // The pipe filled up in between; the item goes back where it was.
                ItemStack left = HopperBlockEntity.addItem(null, source, taken, face);
                if (!left.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, left);
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean accepts(@Nullable BlockEntity target, ItemStack stack, Direction facing) {
        if (target instanceof ItemPipeBlockEntity pipe) return pipe.accepts(stack, facing);
        if (target instanceof StorageControllerBlockEntity controller) return controller.room(stack) >= stack.getCount();
        return false;
    }

    private static boolean deliver(@Nullable BlockEntity target, ItemStack stack, Direction facing) {
        if (target instanceof ItemPipeBlockEntity pipe) return pipe.insert(stack, facing);
        if (target instanceof StorageControllerBlockEntity controller) return controller.insert(stack).isEmpty();
        return false;
    }

    public Component report() {
        return Component.translatable(status.key());
    }

    // ------------------------------------------------------------------ lifecycle

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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filter);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        filter = in.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!filter.isEmpty()) out.store("filter", ItemStack.OPTIONAL_CODEC, filter);
    }
}
