package dev.strataindustria.tanning;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModFluids;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The soaking barrel (tier 3 spec 12.1): an input stack, an output slot and a 4000 mB tank. Recipes run
 * only while the lid is sealed and use what is in the barrel when they finish, so opening the lid early
 * cancels without losing anything. Unsealed and open to the sky, it collects rain.
 */
public class SoakingBarrelBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int CAPACITY = 4000;
    public static final int BUCKET = 1000;
    public static final int INPUT = 0, OUTPUT = 1, SLOTS = 2;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_FLUID = 2, DATA_AMOUNT = 3, DATA_SHORT = 4, DATA_COUNT = 5;

    public enum Status {
        EMPTY, OPEN, NO_RECIPE, NEEDS_MORE, NEEDS_FLUID, OUTPUT_FULL, WORKING;

        public String key() {
            return StrataIndustria.MOD_ID + ".soaking_barrel." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The fluids the screen knows how to draw, by index; 0 is an empty tank. */
    public enum TankFluid {
        NONE, WATER, LYE, TANNIN, OTHER;

        public static TankFluid of(Fluid fluid) {
            if (fluid == Fluids.EMPTY) return NONE;
            if (fluid.isSame(Fluids.WATER)) return WATER;
            if (fluid.isSame(ModFluids.LYE.get())) return LYE;
            if (fluid.isSame(ModFluids.TANNIN.get())) return TANNIN;
            return OTHER;
        }

        public String key() {
            return StrataIndustria.MOD_ID + ".soaking_barrel.fluid." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final int[] INPUT_SLOTS = {INPUT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private Fluid fluid = Fluids.EMPTY;
    private int amount;
    private int progress;
    private int total = 1;
    private int itemsShort;
    private Status status = Status.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> total <= 0 ? 0 : progress * 1000 / total;
                case DATA_STATUS -> status.ordinal();
                case DATA_FLUID -> TankFluid.of(fluid).ordinal();
                case DATA_AMOUNT -> amount;
                case DATA_SHORT -> itemsShort;
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

    public SoakingBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOAKING_BARREL.get(), pos, state);
    }

    public Fluid fluid() {
        return fluid;
    }

    public int amount() {
        return amount;
    }

    private boolean sealed() {
        return getBlockState().getValue(SoakingBarrelBlock.SEALED);
    }

    /** Adds water from a bucket or the sky; false when the tank holds something else or is too full. */
    public boolean addWater(int mb, boolean simulate) {
        if (amount > 0 && !fluid.isSame(Fluids.WATER)) return false;
        if (amount + mb > CAPACITY) return false;
        if (!simulate) {
            fluid = Fluids.WATER;
            amount += mb;
            sync();
        }
        return true;
    }

    /**
     * Tips out lye or tannin (an empty bucket on an open barrel), so leftovers too small for another
     * batch never block the barrel. Returns what was poured away, or null if there was nothing to pour.
     */
    public TankFluid pourOut() {
        if (amount <= 0 || fluid.isSame(Fluids.WATER)) return null;
        TankFluid poured = TankFluid.of(fluid);
        amount = 0;
        fluid = Fluids.EMPTY;
        sync();
        return poured;
    }

    /** Takes a bucket of water back out. */
    public boolean takeWater() {
        if (!fluid.isSame(Fluids.WATER) || amount < BUCKET) return false;
        amount -= BUCKET;
        if (amount == 0) fluid = Fluids.EMPTY;
        sync();
        return true;
    }

    /** Opening the lid cancels the soak in progress; everything stays in the barrel. */
    public void lidChanged() {
        progress = 0;
        sync();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SoakingBarrelBlockEntity barrel) {
        ServerLevel server = (ServerLevel) level;
        Status before = barrel.status;
        if (!state.getValue(SoakingBarrelBlock.SEALED)) {
            barrel.progress = 0;
            barrel.status = Status.OPEN;
            int rain = dev.strataindustria.Config.BARREL_RAIN_FILL.getAsInt();
            if (rain > 0 && server.isRainingAt(pos.above()) && barrel.amount < CAPACITY && barrel.addWater(0, true)) {
                int was = barrel.amount;
                barrel.fluid = Fluids.WATER;
                barrel.amount = Math.min(CAPACITY, barrel.amount + rain);
                if (barrel.amount / 50 != was / 50) barrel.sync();
                if (server.getRandom().nextInt(400) == 0) {
                    server.playSound(null, pos, ModSounds.SOAKING_BARREL_FILL.get(), SoundSource.BLOCKS, 0.25f, 1.2f);
                }
            }
        } else {
            barrel.status = barrel.soak(server);
        }
        if (barrel.status != before) barrel.setChanged();
    }

    private BarrelInput input() {
        return new BarrelInput(items.get(INPUT), fluid, amount);
    }

    private Status soak(ServerLevel level) {
        BarrelInput input = input();
        itemsShort = 0;
        if (input.isEmpty()) {
            progress = 0;
            return Status.EMPTY;
        }
        Optional<RecipeHolder<BarrelRecipe>> found = BarrelRecipe.recipeFor(level, input);
        if (found.isEmpty()) {
            progress = 0;
            return Status.NO_RECIPE;
        }
        BarrelRecipe recipe = found.get().value();
        int batches = recipe.batches(input);
        itemsShort = recipe.itemsShort(input);
        if (itemsShort > 0) {
            progress = 0;
            return Status.NEEDS_MORE;
        }
        if (batches <= 0) {
            progress = 0;
            return Status.NEEDS_FLUID;
        }
        ItemStack made = recipe.assemble(input);
        made.setCount(made.getCount() * batches);
        ItemStack out = items.get(OUTPUT);
        if (!made.isEmpty() && !out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, made)
                || out.getCount() + made.getCount() > out.getMaxStackSize())) {
            return Status.OUTPUT_FULL;
        }
        total = recipe.ticks();
        if (++progress < total) return Status.WORKING;

        // Done: use up the inputs and fill the outputs.
        progress = 0;
        if (recipe.ingredient().isPresent()) items.get(INPUT).shrink(Math.min(items.get(INPUT).getCount(), batches * recipe.count()));
        if (recipe.fluidResult().isPresent()) {
            FluidAmount result = recipe.fluidResult().get();
            // The whole tank turns over: 1000 mB of water makes 1000 mB of lye.
            amount = recipe.fluid().isPresent()
                    ? (int) Math.min(CAPACITY, (long) amount * result.amount() / recipe.fluid().get().amount())
                    : Math.min(CAPACITY, amount + batches * result.amount());
            fluid = result.fluid();
        } else if (recipe.fluid().isPresent()) {
            amount -= batches * recipe.fluid().get().amount();
        }
        if (amount <= 0) {
            amount = 0;
            fluid = Fluids.EMPTY;
        }
        if (!made.isEmpty()) {
            if (out.isEmpty()) items.set(OUTPUT, made);
            else out.grow(made.getCount());
        }
        level.playSound(null, worldPosition, ModSounds.SOAKING_BARREL_DONE.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
        sync();
        return Status.EMPTY;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == INPUT) progress = 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == INPUT && side != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT && side == Direction.DOWN;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT;
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".soaking_barrel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SoakingBarrelMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        fluid = in.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        amount = in.getIntOr("amount", 0);
        if (fluid == Fluids.EMPTY) amount = 0;
        progress = in.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        if (amount > 0) {
            out.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
            out.putInt("amount", amount);
        }
        out.putInt("progress", progress);
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
