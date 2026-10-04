package dev.strataindustria.washing;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The sluice (tier 3 spec 8.6). Water must run in at the back. Items thrown in or fed by a hopper wait
 * in a buffer of four and are washed one at a time with the washing recipes; what comes out goes into a
 * container in front of the outlet, one block down, or is washed out of the front.
 */
public class SluiceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOTS = 4;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_COUNT = 2;

    public enum Status {
        EMPTY, WORKING, NO_WATER, NO_RECIPE;

        public String key() {
            return StrataIndustria.MOD_ID + ".sluice." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final int[] ALL = {0, 1, 2, 3};
    private static final int[] NONE = {};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int progress;
    private int total = WashingRecipe.DEFAULT_TICKS;
    private Status status = Status.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> total <= 0 ? 0 : progress * 1000 / total;
                case DATA_STATUS -> status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SluiceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLUICE.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(SluiceBlock.FACING);
    }

    /** Water flowing in at the back, at the same level. */
    public static boolean hasFlow(Level level, BlockPos pos, Direction facing) {
        return level.getFluidState(pos.relative(facing.getOpposite())).is(FluidTags.WATER);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SluiceBlockEntity sluice) {
        ServerLevel server = (ServerLevel) level;
        boolean wet = hasFlow(level, pos, state.getValue(SluiceBlock.FACING));
        if (state.getValue(SluiceBlock.WET) != wet) level.setBlock(pos, state.setValue(SluiceBlock.WET, wet), Block.UPDATE_CLIENTS);
        if (level.getGameTime() % 5 == 0) sluice.collect(server);
        Status before = sluice.status;
        sluice.status = sluice.work(server, wet);
        if (sluice.status != before) sluice.setChanged();
    }

    /** Picks up washable items lying in the trough. */
    private void collect(ServerLevel level) {
        AABB trough = new AABB(worldPosition).deflate(0.05, 0.0, 0.05).setMaxY(worldPosition.getY() + 1.0);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, trough, ItemEntity::isAlive)) {
            ItemStack stack = entity.getItem();
            if (WashingRecipe.recipeFor(level, stack).isEmpty()) continue;
            for (int slot = 0; slot < SLOTS && !stack.isEmpty(); slot++) {
                ItemStack held = items.get(slot);
                if (held.isEmpty()) {
                    items.set(slot, stack.split(stack.getMaxStackSize()));
                } else if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize()) {
                    int move = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                    held.grow(move);
                    stack.shrink(move);
                }
            }
            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);
            setChanged();
        }
    }

    private int nextSlot() {
        for (int slot = 0; slot < SLOTS; slot++) if (!items.get(slot).isEmpty()) return slot;
        return -1;
    }

    private Status work(ServerLevel level, boolean wet) {
        int slot = nextSlot();
        if (slot < 0) {
            progress = 0;
            return Status.EMPTY;
        }
        if (!wet) return Status.NO_WATER;
        ItemStack input = items.get(slot);
        Optional<RecipeHolder<WashingRecipe>> recipe = WashingRecipe.recipeFor(level, input);
        if (recipe.isEmpty()) {
            // Not washable (put in by hand): wash it straight out.
            deliver(level, input.split(input.getCount()));
            return Status.NO_RECIPE;
        }
        total = recipe.get().value().ticks();
        progress++;
        if (level.getGameTime() % 30 == 0) {
            level.playSound(null, worldPosition, ModSounds.SLUICE_WASH.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        if (level.getGameTime() % 4 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.35, worldPosition.getZ() + 0.5,
                    2, 0.25, 0.02, 0.25, 0.0);
        }
        if (progress >= total) {
            progress = 0;
            input.shrink(1);
            List<ItemStack> out = recipe.get().value().roll(level.getRandom());
            for (ItemStack stack : out) deliver(level, stack);
            if (out.size() > 1) {
                level.playSound(null, worldPosition, ModSounds.WASHING_PAN_FIND.get(), SoundSource.PLAYERS, 0.4f, 1.5f);
            }
            Journal.awardNear(level, worldPosition, Journal.WASH);
        }
        setChanged();
        return Status.WORKING;
    }

    /** Into a container in front of the outlet and one block down, or out of the front. */
    private void deliver(ServerLevel level, ItemStack stack) {
        Direction facing = facing();
        BlockPos below = worldPosition.relative(facing).below();
        if (level.getBlockEntity(below) instanceof Container container) {
            stack = HopperBlockEntity.addItem(null, container, stack, Direction.UP);
            container.setChanged();
            if (stack.isEmpty()) return;
        }
        ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5 + facing.getStepX() * 0.7, worldPosition.getY() + 0.2,
                worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.7, stack, facing.getStepX() * 0.08, 0.0, facing.getStepZ() * 0.08);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    public Status status() {
        return status;
    }

    // ------------------------------------------------------------------ container

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? NONE : ALL;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return level == null || level.isClientSide() || WashingRecipe.recipeFor(level, stack).isPresent();
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".sluice");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SluiceMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        progress = in.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putInt("progress", progress);
    }
}
