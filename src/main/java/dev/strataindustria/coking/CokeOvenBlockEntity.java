package dev.strataindustria.coking;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The coke oven (tier 4 spec 5.1): no fuel and no power. It bakes one item at a time into coke or
 * charcoal and keeps the creosote that cooks out in a 16000 mB tank. A full tank stops the oven until
 * it is drawn off with buckets.
 */
public class CokeOvenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int CAPACITY = 16000;
    public static final int BUCKET = 1000;
    public static final int INPUT = 0, OUTPUT = 1, BUCKET_IN = 2, BUCKET_OUT = 3, SLOTS = 4;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_CREOSOTE = 2, DATA_PROBLEM = 3, DATA_DX = 4, DATA_DY = 5,
            DATA_DZ = 6, DATA_COUNT = 7;

    public enum Status {
        INCOMPLETE, EMPTY, NO_RECIPE, WORKING, OUTPUT_FULL, TANK_FULL;

        public String key() {
            return StrataIndustria.MOD_ID + ".coke_oven.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** One coking recipe: so many of an input bake into one output, with creosote, over so many ticks. */
    public record Coking(java.util.function.Predicate<ItemStack> input, int count, java.util.function.Supplier<ItemStack> result,
            int creosote, int ticks) {}

    /** Spec 5.1, in the order they are matched. */
    public static final List<Coking> RECIPES = List.of(
            new Coking(s -> s.is(Items.COAL), 1, () -> new ItemStack(Tier4Items.COKE.get()), 250, 1800),
            new Coking(s -> s.is(Items.COAL_BLOCK), 1, () -> new ItemStack(Tier4Items.COKE_BLOCK.get()), 2250, 16200),
            new Coking(s -> s.is(ModItems.LIGNITE.get()), 2, () -> new ItemStack(Tier4Items.COKE.get()), 100, 2400),
            new Coking(s -> s.is(ItemTags.LOGS), 1, () -> new ItemStack(Items.CHARCOAL), 100, 1200));

    private static final int[] TOP_SLOTS = {INPUT, BUCKET_IN};
    private static final int[] BOTTOM_SLOTS = {OUTPUT, BUCKET_OUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int creosote;
    private int progress;
    private int total = 1;
    private Status status = Status.INCOMPLETE;
    private CokeOvenStructure.Result structure = CokeOvenStructure.INCOMPLETE;
    private boolean announced;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            BlockPos at = structure.at().subtract(worldPosition);
            return switch (index) {
                case DATA_PROGRESS -> total <= 0 ? 0 : (int) ((long) progress * 1000 / total);
                case DATA_STATUS -> status.ordinal();
                case DATA_CREOSOTE -> creosote;
                case DATA_PROBLEM -> structure.problem().ordinal();
                case DATA_DX -> at.getX();
                case DATA_DY -> at.getY();
                case DATA_DZ -> at.getZ();
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

    public CokeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.COKE_OVEN.get(), pos, state);
    }

    public static java.util.Optional<Coking> recipeFor(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        for (Coking recipe : RECIPES) if (recipe.input().test(stack)) return java.util.Optional.of(recipe);
        return java.util.Optional.empty();
    }

    private Direction facing() {
        return getBlockState().getValue(CokeOvenBlock.FACING);
    }

    public CokeOvenStructure.Result checkStructure() {
        if (level != null) structure = CokeOvenStructure.check(level, worldPosition, facing());
        return structure;
    }

    public int creosote() {
        return creosote;
    }

    public Status status() {
        return status;
    }

    /** Takes a bucket's worth of creosote out as a filled bucket. */
    public ItemStack drainBucket() {
        if (creosote < BUCKET) return ItemStack.EMPTY;
        creosote -= BUCKET;
        sync();
        return new ItemStack(Tier4Items.CREOSOTE_BUCKET.get());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CokeOvenBlockEntity oven) {
        if (level.getGameTime() % 20 == 0 || oven.status == Status.INCOMPLETE && oven.structure.complete()) {
            oven.checkStructure();
            if (oven.structure.complete() && !oven.announced) {
                oven.announced = true;
                Journal.awardNear(level, pos, Journal.COKE_OVEN_BUILT);
                oven.setChanged();
            }
        }
        oven.fillBucket();
        Status before = oven.status;
        oven.status = oven.work(level);
        boolean lit = oven.status == Status.WORKING;
        if (state.getValue(CokeOvenBlock.LIT) != lit) level.setBlock(pos, state.setValue(CokeOvenBlock.LIT, lit), Block.UPDATE_ALL);
        if (oven.status != before || lit) oven.setChanged();
    }

    /** An empty bucket in the bucket slot is filled while there is a bucket's worth in the tank. */
    private void fillBucket() {
        ItemStack in = items.get(BUCKET_IN);
        if (!in.is(Items.BUCKET) || creosote < BUCKET || !items.get(BUCKET_OUT).isEmpty()) return;
        in.shrink(1);
        items.set(BUCKET_OUT, drainBucket());
        if (level != null) level.playSound(null, worldPosition, Tier4Sounds.CREOSOTE_FILL.get(), SoundSource.BLOCKS, 0.6f, 0.9f);
    }

    private Status work(Level level) {
        if (!structure.complete()) {
            progress = 0;
            return Status.INCOMPLETE;
        }
        ItemStack input = items.get(INPUT);
        if (input.isEmpty()) {
            progress = 0;
            return Status.EMPTY;
        }
        var found = recipeFor(input);
        if (found.isEmpty() || input.getCount() < found.get().count()) {
            progress = 0;
            return Status.NO_RECIPE;
        }
        Coking recipe = found.get();
        ItemStack made = recipe.result().get();
        ItemStack out = items.get(OUTPUT);
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, made) || out.getCount() + made.getCount() > out.getMaxStackSize())) {
            return Status.OUTPUT_FULL;
        }
        if (creosote + recipe.creosote() > CAPACITY) return Status.TANK_FULL;
        total = recipe.ticks();
        if (++progress < total) return Status.WORKING;

        progress = 0;
        input.shrink(recipe.count());
        if (out.isEmpty()) items.set(OUTPUT, made);
        else out.grow(made.getCount());
        creosote += recipe.creosote();
        level.playSound(null, worldPosition, Tier4Sounds.COKE_OVEN_DONE.get(), SoundSource.BLOCKS, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        sync();
        return recipeFor(input).isPresent() ? Status.WORKING : Status.EMPTY;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        boolean changed = slot == INPUT && !ItemStack.isSameItem(stack, items.get(INPUT));
        super.setItem(slot, stack);
        if (changed) progress = 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM_SLOTS : TOP_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && (slot == OUTPUT || slot == BUCKET_OUT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> recipeFor(stack).isPresent();
            case BUCKET_IN -> stack.is(Items.BUCKET);
            default -> false;
        };
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".coke_oven");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        checkStructure();
        return new CokeOvenMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        creosote = in.getIntOr("creosote", 0);
        progress = in.getIntOr("progress", 0);
        announced = in.getIntOr("announced", 0) != 0;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putInt("creosote", creosote);
        out.putInt("progress", progress);
        out.putInt("announced", announced ? 1 : 0);
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
