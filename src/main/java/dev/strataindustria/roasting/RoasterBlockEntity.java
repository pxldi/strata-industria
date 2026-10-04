package dev.strataindustria.roasting;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatIntake;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The roaster (tier 4 spec 8.5): roasting at scale. Four input slots roast side by side, each its whole
 * stack at once, in half the time a forge slot takes, drawing 20 HU/t at 800 °C or more from a firebox
 * under it or over heat pipes. The sulfur dioxide that comes off is kept in a 4000 mB tank and pushed out
 * of the back face into fluid pipes; with the tank full the roaster vents the rest and keeps working.
 */
public class RoasterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, HeatConsumer, HeatPort, FluidPort {
    public static final int INPUTS = 4, OUTPUTS = 4, SLOTS = INPUTS + OUTPUTS;
    public static final int MIN_TEMPERATURE = 800, HEAT = 20, CAPACITY = 4000, MAX_PUSH = 100;
    /** The gas leaves warm; the pipes have to take it. */
    public static final float GAS_TEMPERATURE = 200.0f;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = INPUTS, DATA_TEMPERATURE = INPUTS + 1, DATA_HEAT = INPUTS + 2,
            DATA_LIMIT = INPUTS + 3, DATA_GAS = INPUTS + 4, DATA_COUNT = INPUTS + 5;

    public enum Status {
        EMPTY, NO_RECIPE, OUTPUT_FULL, NEEDS_HEAT, ROASTING, VENTING;

        public String key() {
            return StrataIndustria.MOD_ID + ".roaster.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int[] INPUT_SLOTS = {0, 1, 2, 3};
    private static final int[] OUTPUT_SLOTS = {4, 5, 6, 7};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final HeatIntake intake = new HeatIntake(MIN_TEMPERATURE, HEAT);
    private final float[] progress = new float[INPUTS];
    private final int[] total = new int[INPUTS];
    /** The recipe each input slot last matched, kept so the lookup runs only when the item changes. */
    private final Item[] cachedItem = new Item[INPUTS];
    private final RoastingRecipe[] cachedRecipe = new RoastingRecipe[INPUTS];
    private int gas;
    /** Ticks left of the venting warning since gas last overflowed. */
    private int venting;
    private Status status = Status.EMPTY;
    private FluidPipes.Network network = FluidPipes.Network.NONE;
    private int age;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_PROGRESS && index < DATA_PROGRESS + INPUTS) {
                int slot = index - DATA_PROGRESS;
                return total[slot] <= 0 ? 0 : Math.min(1000, Math.round(progress[slot] * 1000 / total[slot]));
            }
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_TEMPERATURE -> Math.round(intake.temperature());
                case DATA_HEAT -> intake.heat();
                case DATA_LIMIT -> intake.limit();
                case DATA_GAS -> gas;
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

    public RoasterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.ROASTER.get(), pos, state);
    }

    /** Ticks a roaster takes for a recipe: half the forge's time (spec 8.5). */
    public static int ticks(RoastingRecipe recipe) {
        return Math.max(1, recipe.ticks() / 2);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RoasterBlockEntity roaster) {
        roaster.intake.roll();
        roaster.age++;
        Status before = roaster.status;
        roaster.status = roaster.work((ServerLevel) level, pos, state);
        if (roaster.status != before || roaster.status == Status.ROASTING || roaster.status == Status.VENTING) roaster.setChanged();
        boolean lit = roaster.status == Status.ROASTING || roaster.status == Status.VENTING;
        if (state.getValue(RoasterBlock.LIT) != lit) level.setBlock(pos, state.setValue(RoasterBlock.LIT, lit), Block.UPDATE_ALL);
    }

    private Status work(ServerLevel level, BlockPos pos, BlockState state) {
        boolean any = false, loaded = false, blocked = true, finished = false;
        int released = 0;
        boolean hot = intake.heat() > 0;
        for (int i = 0; i < INPUTS; i++) {
            ItemStack input = items.get(INPUT_SLOTS[i]);
            if (input.isEmpty()) {
                progress[i] = total[i] = 0;
                continue;
            }
            loaded = true;
            RoastingRecipe recipe = recipe(level, i, input);
            if (recipe == null) {
                progress[i] = total[i] = 0;
                continue;
            }
            any = true;
            total[i] = ticks(recipe);
            ItemStack made = recipe.assemble(new SingleRecipeInput(input));
            made.setCount(made.getCount() * input.getCount());
            if (!fits(OUTPUT_SLOTS[i], made)) continue;
            blocked = false;
            if (!hot || intake.temperature() < recipe.minTemperature()) continue;
            // A short supply of heat roasts more slowly.
            progress[i] += intake.share();
            if (progress[i] < total[i]) continue;
            progress[i] = 0;
            released += recipe.gas().map(g -> g.amount() * input.getCount()).orElse(0);
            insert(OUTPUT_SLOTS[i], made);
            items.set(INPUT_SLOTS[i], ItemStack.EMPTY);
            finished = true;
        }

        gas += released;
        boolean overflow = gas > CAPACITY;
        if (overflow) {
            gas = CAPACITY;
            venting = 40;
        } else if (venting > 0) {
            venting--;
        }
        push(level, pos, state);

        if (finished) {
            level.playSound(null, pos, Tier4Sounds.ROASTING_DONE.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        if (overflow) {
            // The gas has nowhere to go: a thick yellow plume out of the hood.
            level.sendParticles(new DustParticleOptions(RoasterBlock.FUME_COLOUR, 2.0f), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    10, 0.2, 0.15, 0.2, 0.02);
        }
        if (!any) return loaded ? Status.NO_RECIPE : Status.EMPTY;
        if (blocked) return Status.OUTPUT_FULL;
        if (!hot) return Status.NEEDS_HEAT;
        if (venting > 0) return Status.VENTING;
        return Status.ROASTING;
    }

    /** Sends gas out of the back face into whatever pipes or ports are there. */
    private void push(ServerLevel level, BlockPos pos, BlockState state) {
        if (gas <= 0) return;
        if (age % 20 == 1) {
            network = FluidPipes.find(level, pos, back(state));
        }
        FluidPipes.Push push = FluidPipes.push(level, network, Tier4Fluids.SULFUR_DIOXIDE.get(), Math.min(gas, MAX_PUSH), GAS_TEMPERATURE, 0);
        gas -= push.moved();
    }

    private static Direction back(BlockState state) {
        return state.getValue(RoasterBlock.FACING).getOpposite();
    }

    private @Nullable RoastingRecipe recipe(Level level, int slot, ItemStack input) {
        if (cachedItem[slot] != input.getItem()) {
            cachedItem[slot] = input.getItem();
            cachedRecipe[slot] = RoastingRecipe.recipeFor(level, input).map(h -> h.value()).orElse(null);
        }
        return cachedRecipe[slot];
    }

    /** Whether the roaster roasts this: anything with a roasting recipe. Only the server knows the recipes. */
    public static boolean roasts(Level level, ItemStack stack) {
        return RoastingRecipe.recipeFor(level, stack).isPresent();
    }

    private boolean fits(int slot, ItemStack made) {
        ItemStack there = items.get(slot);
        if (there.isEmpty()) return made.getCount() <= made.getMaxStackSize();
        return ItemStack.isSameItemSameComponents(there, made) && there.getCount() + made.getCount() <= there.getMaxStackSize();
    }

    private void insert(int slot, ItemStack made) {
        ItemStack there = items.get(slot);
        if (there.isEmpty()) items.set(slot, made);
        else there.grow(made.getCount());
    }

    public Status status() {
        return status;
    }

    /** Sulfur dioxide in the tank, in mB. */
    public int gas() {
        return gas;
    }

    /** The heat that came in last tick, for tests. */
    public HeatIntake intake() {
        return intake;
    }

    // ------------------------------------------------------------------ heat

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    @Override
    public int heatDemand(float temperature) {
        return intake.demand(temperature, status == Status.ROASTING || status == Status.VENTING || status == Status.NEEDS_HEAT);
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        return intake.offer(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        intake.route(limitedBy);
    }

    // ------------------------------------------------------------------ gas

    @Override
    public boolean connectsFluid(Direction side) {
        return side == back(getBlockState());
    }

    /** The gas outlet only gives. */
    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return 0;
    }

    // ------------------------------------------------------------------ container

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= INPUTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < INPUTS && level != null && roasts(level, stack);
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".roaster");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new RoasterMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        for (int i = 0; i < INPUTS; i++) progress[i] = in.getFloatOr("progress" + i, 0);
        gas = in.getIntOr("gas", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        for (int i = 0; i < INPUTS; i++) out.putFloat("progress" + i, progress[i]);
        out.putInt("gas", gas);
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
