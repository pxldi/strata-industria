package dev.strataindustria.metal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatIntake;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The smelter (tier 4 spec 8.7): the automated crucible. It melts and alloys by the crucible's rules in a
 * 1600 unit pot rated to 1700 °C, heated over the heat network or by a firebox below at up to 40 HU/t.
 * With auto-pour on it fills the mold as soon as it can, sets the filled mold aside to cool at the rate
 * it would on the ground, knocks the casting out into the output slot, and puts the mold back, taking a
 * fresh one from the mold stock when a mold cracks. Hoppers and inserters load the inputs and the mold
 * stock from the top and sides and take castings from the bottom.
 */
public class SmelterBlockEntity extends CrucibleBlockEntity implements WorldlyContainer, HeatConsumer, HeatPort {
    public static final int STOCK_SLOT = MOLD_SLOT + 1, COOLING_SLOT = MOLD_SLOT + 2, OUTPUT_SLOT = MOLD_SLOT + 3, SLOTS = MOLD_SLOT + 4;
    public static final int CAPACITY = 1600, MAX_TEMPERATURE = REFRACTORY_MAX_TEMPERATURE;
    /** Most empty molds the stock slot holds. */
    public static final int STOCK_SIZE = 16;
    /** It takes any heat that is warmer than the room; the load decides what that heat can melt. */
    public static final int MIN_TEMPERATURE = 100, HEAT = 40;
    /** Heats as fast as a forge does a crucible when the full 40 HU/t comes in. */
    public static final float RATE = FORGE_RATE;
    public static final int DATA_AUTO = CrucibleBlockEntity.DATA_COUNT, DATA_HEAT = DATA_AUTO + 1, DATA_HEAT_TEMPERATURE = DATA_AUTO + 2,
            DATA_LIMIT = DATA_AUTO + 3, DATA_COOLING = DATA_AUTO + 4, DATA_COUNT = DATA_AUTO + 5;

    private static final int[] TOP_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, STOCK_SLOT};
    private static final int[] BOTTOM_SLOTS = {OUTPUT_SLOT};

    private final HeatIntake intake = new HeatIntake(MIN_TEMPERATURE, HEAT);
    private boolean autoPour = true;
    /** The cooling mold's temperature, which the smelter cools tick by tick at the rate of the open air; 0 for none. */
    private float coolingHeat;

    private final ContainerData smelterData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < DATA_AUTO) return SmelterBlockEntity.super.data().get(index);
            // The crucible's indexes depend on how many metals there are, so these are not constants to switch on.
            if (index == DATA_AUTO) return autoPour ? 1 : 0;
            if (index == DATA_HEAT) return intake.heat();
            if (index == DATA_HEAT_TEMPERATURE) return Math.round(intake.temperature());
            if (index == DATA_LIMIT) return intake.limit();
            if (index == DATA_COOLING) return coolingTemperature();
            return 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SmelterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.SMELTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmelterBlockEntity smelter) {
        smelter.intake.roll();
        CrucibleBlockEntity.serverTick(level, pos, state, smelter);
        if (smelter.automate(level, pos)) smelter.setChanged();
        boolean lit = smelter.isMolten() || smelter.status() == CrucibleStatus.MELTING;
        if (state.getValue(SmelterBlock.LIT) != lit) level.setBlock(pos, state.setValue(SmelterBlock.LIT, lit), Block.UPDATE_ALL);
    }

    /** One step of auto-pour: cool, knock out, put the mold back, restock it, and pour. Off, it is a plain crucible. */
    private boolean automate(Level level, BlockPos pos) {
        if (!autoPour) return false;
        NonNullList<ItemStack> items = items();
        boolean changed = false;
        ItemStack mold = items.get(MOLD_SLOT);
        // A filled mold makes way for the next pour while it cools.
        if (!pouring() && mold.has(ModDataComponents.CAST_CONTENTS.get()) && items.get(COOLING_SLOT).isEmpty()) {
            items.set(COOLING_SLOT, mold);
            items.set(MOLD_SLOT, ItemStack.EMPTY);
            coolingHeat = Heat.get(mold, level);
            changed = true;
        }
        changed |= cool(level);
        changed |= knockOut(level, pos);
        changed |= takeBack();
        changed |= restock();
        if (!pouring() && pourProblem().isEmpty()) changed |= startPour();
        return changed;
    }

    /** One tick of the filled mold cooling as it would on the ground; the item carries the same heat. */
    private boolean cool(Level level) {
        ItemStack cooling = items().get(COOLING_SLOT);
        if (!cooling.has(ModDataComponents.CAST_CONTENTS.get())) return false;
        // After a reload, pick the heat up from the item.
        if (coolingHeat <= 0) coolingHeat = Heat.get(cooling, level);
        coolingHeat = (float) (Heat.AMBIENT + (coolingHeat - Heat.AMBIENT) * Math.exp(-Heat.AMBIENT_RATE / 20.0));
        Heat.set(cooling, coolingHeat, level.getGameTime());
        return true;
    }

    /** Knocks the casting out of the cooling mold once its metal has set, if the output has room. */
    private boolean knockOut(Level level, BlockPos pos) {
        NonNullList<ItemStack> items = items();
        ItemStack cooling = items.get(COOLING_SLOT);
        Melt contents = cooling.get(ModDataComponents.CAST_CONTENTS.get());
        if (contents == null) return false;
        float heat = coolingHeat;
        if (heat >= mixMeltingPoint(contents)) return false;
        ItemStack cast = CastMoldItem.castOf(cooling, heat, level.getGameTime());
        if (!merge(level, cast)) return false;
        coolingHeat = 0;
        if (CastMoldItem.breaks(cooling, level.getRandom())) {
            items.set(COOLING_SLOT, ItemStack.EMPTY);
            level.playSound(null, pos, ModSounds.MOLD_BREAK.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
        } else {
            items.set(COOLING_SLOT, CastMoldItem.emptied(cooling));
            level.playSound(null, pos, ModSounds.MOLD_KNOCK.get(), SoundSource.BLOCKS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        return true;
    }

    /** An emptied mold in the cooling slot goes back to the mold slot, or to the stock if the slot is taken. */
    private boolean takeBack() {
        NonNullList<ItemStack> items = items();
        ItemStack mold = items.get(COOLING_SLOT);
        if (mold.isEmpty() || mold.has(ModDataComponents.CAST_CONTENTS.get())) return false;
        if (items.get(MOLD_SLOT).isEmpty()) {
            items.set(MOLD_SLOT, mold);
            items.set(COOLING_SLOT, ItemStack.EMPTY);
            return true;
        }
        ItemStack stock = items.get(STOCK_SLOT);
        if (stock.isEmpty()) {
            items.set(STOCK_SLOT, mold);
        } else if (ItemStack.isSameItemSameComponents(stock, mold) && stock.getCount() < STOCK_SIZE) {
            stock.grow(1);
        } else {
            return false;
        }
        items.set(COOLING_SLOT, ItemStack.EMPTY);
        return true;
    }

    /** An empty mold slot takes the next mold from the stock. */
    private boolean restock() {
        NonNullList<ItemStack> items = items();
        if (!items.get(MOLD_SLOT).isEmpty() || items.get(STOCK_SLOT).isEmpty()) return false;
        items.set(MOLD_SLOT, items.get(STOCK_SLOT).split(1));
        return true;
    }

    /**
     * Puts a casting in the output slot. Castings of the same kind stack whatever their heat; the stack
     * keeps the hotter of the two temperatures, so nothing hot passes for cold.
     */
    private boolean merge(Level level, ItemStack cast) {
        NonNullList<ItemStack> items = items();
        ItemStack out = items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            items.set(OUTPUT_SLOT, cast);
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(withoutHeat(out), withoutHeat(cast)) || out.getCount() + cast.getCount() > out.getMaxStackSize()) {
            return false;
        }
        long now = level.getGameTime();
        float heat = Math.max(Heat.get(out, now), Heat.get(cast, now));
        out.grow(cast.getCount());
        Heat.set(out, heat, now);
        return true;
    }

    private static ItemStack withoutHeat(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        copy.remove(ModDataComponents.TEMPERATURE.get());
        return copy;
    }

    private int coolingTemperature() {
        return items().get(COOLING_SLOT).has(ModDataComponents.CAST_CONTENTS.get()) ? Math.round(coolingHeat) : 0;
    }

    public boolean autoPour() {
        return autoPour;
    }

    public void toggleAutoPour() {
        autoPour = !autoPour;
        setChanged();
    }

    /** The heat that came in last tick, for tests. */
    public HeatIntake intake() {
        return intake;
    }

    @Override
    public ContainerData data() {
        return smelterData;
    }

    // ------------------------------------------------------------------ crucible

    @Override
    protected int slotCount() {
        return SLOTS;
    }

    @Override
    protected HeatSource heatSource(Level level, BlockPos pos) {
        if (intake.heat() <= 0) return new HeatSource(false, Heat.AMBIENT, Heat.AMBIENT_RATE);
        // A short supply heats more slowly towards the same temperature.
        return new HeatSource(true, intake.temperature(), RATE * Math.max(0.1f, intake.share()));
    }

    @Override
    protected CrucibleStatus noHeatStatus() {
        return CrucibleStatus.NO_HEAT;
    }

    @Override
    protected SoundEvent pourSound() {
        return Tier4Sounds.SMELTER_POUR.get();
    }

    @Override
    public boolean refractory() {
        return true;
    }

    @Override
    public int capacityOf() {
        return CAPACITY;
    }

    @Override
    public int maxTemperature() {
        return MAX_TEMPERATURE;
    }

    // ------------------------------------------------------------------ heat

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    @Override
    public int heatDemand(float temperature) {
        // It draws while it has anything to melt or keep molten.
        return intake.demand(temperature, !melt().isEmpty() || pendingUnits() > 0 || pouring());
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        return intake.offer(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        intake.route(limitedBy);
    }

    // ------------------------------------------------------------------ container

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == STOCK_SLOT) {
            return stack.getItem() instanceof CastMoldItem && !stack.has(ModDataComponents.CAST_CONTENTS.get())
                    && items().get(STOCK_SLOT).getCount() + stack.getCount() <= STOCK_SIZE;
        }
        if (slot == MOLD_SLOT) return stack.getItem() instanceof CastMoldItem && !stack.has(ModDataComponents.CAST_CONTENTS.get());
        if (slot == COOLING_SLOT || slot == OUTPUT_SLOT) return false;
        return super.canPlaceItem(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM_SLOTS : TOP_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".smelter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SmelterMenu(id, inventory, this, smelterData);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        autoPour = in.getBooleanOr("auto_pour", true);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("auto_pour", autoPour);
    }
}
