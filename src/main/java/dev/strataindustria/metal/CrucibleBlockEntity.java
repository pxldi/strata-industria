package dev.strataindustria.metal;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.Temperature;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A crucible (spec 7.1): nine input slots that melt into one pool of metal once hot enough, and a
 * mold slot to pour into. It takes its heat from a forge directly below.
 */
public class CrucibleBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT_SLOTS = 9;
    public static final int MOLD_SLOT = 9;
    /** Ticks to melt one item at its melting point; halved for every 200 °C above. */
    public static final float MELT_TICKS = 100.0f;
    public static final int POUR_PER_TICK = 10;
    static final float FORGE_RATE = 0.030f;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_MELTING = 2;
    public static final int DATA_POUR = 3;
    public static final int DATA_SLOT_PROGRESS = 4;
    public static final int DATA_UNITS = DATA_SLOT_PROGRESS + INPUT_SLOTS;
    public static final int DATA_COUNT = DATA_UNITS + Metal.values().length;

    private NonNullList<ItemStack> items = NonNullList.withSize(INPUT_SLOTS + 1, ItemStack.EMPTY);
    private final float[] progress = new float[INPUT_SLOTS];
    private Melt melt = Melt.EMPTY;
    private float temperature = Heat.AMBIENT;
    private CrucibleStatus status = CrucibleStatus.COLD;
    private int meltingPercent;
    /** Units still to pour, and what has been poured so far. */
    private int pourLeft;
    private int pourTotal;
    private Melt poured = Melt.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_TEMPERATURE) return Math.round(temperature);
            if (index == DATA_STATUS) return status.ordinal();
            if (index == DATA_MELTING) return meltingPercent;
            if (index == DATA_POUR) return pourTotal == 0 ? 0 : Math.round(100 * (1 - pourLeft / (float) pourTotal));
            if (index < DATA_UNITS) return Math.round(progress[index - DATA_SLOT_PROGRESS]);
            if (index < DATA_COUNT) return melt.units().getOrDefault(Metal.values()[index - DATA_UNITS], 0);
            return 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    public static int capacity() {
        return Config.CRUCIBLE_CAPACITY.getAsInt();
    }

    public Melt melt() {
        return melt;
    }

    public float temperature() {
        return temperature;
    }

    /** The melt's result when molten: one metal, an alloy, or empty for an unknown mix. */
    public Optional<Metal> result() {
        return Alloy.resultOf(melt);
    }

    /** Where the current mix is liquid: its alloy's melting point, or its highest metal's. */
    public static int mixMeltingPoint(Melt melt) {
        return Alloy.resultOf(melt).map(Metal::meltingPoint).orElse(melt.meltingPoint());
    }

    public boolean isMolten() {
        return !melt.isEmpty() && temperature >= mixMeltingPoint(melt);
    }

    /** Metal units waiting in the input slots. */
    int pendingUnits() {
        int total = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            total += MetalContent.of(stack).map(Melt::total).orElse(0) * stack.getCount();
        }
        return total;
    }

    /** How many of {@code stack} still fit, by metal units (spec 7.1: insertion past capacity is blocked). */
    public int roomFor(ItemStack stack, ItemStack replacing) {
        int each = MetalContent.of(stack).map(Melt::total).orElse(0);
        if (each <= 0) return 0;
        int replaced = MetalContent.of(replacing).map(Melt::total).orElse(0) * replacing.getCount();
        int free = capacity() - melt.total() - pendingUnits() - pourLeft + replaced;
        return Math.max(0, free / each);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        boolean forgeBelow = level.getBlockEntity(pos.below()) instanceof ForgeBlockEntity;
        float target = level.getBlockEntity(pos.below()) instanceof ForgeBlockEntity forge ? forge.temperature() : Heat.AMBIENT;
        float rate = forgeBelow ? FORGE_RATE : Heat.AMBIENT_RATE;
        float before = crucible.temperature;
        crucible.temperature = (float) (target + (crucible.temperature - target) * Math.exp(-rate / 20.0));

        boolean changed = Math.abs(crucible.temperature - before) > 0.01f;
        changed |= crucible.meltInputs(level, pos);
        changed |= crucible.pour(level, pos);
        crucible.status = crucible.computeStatus(forgeBelow);
        if (changed) setChanged(level, pos, state);
    }

    private boolean meltInputs(Level level, BlockPos pos) {
        boolean changed = false;
        int melting = 0, sum = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            Optional<Melt> content = MetalContent.of(stack);
            if (content.isEmpty()) {
                progress[i] = 0;
                continue;
            }
            int point = mixMeltingPoint(content.get());
            if (temperature < point) continue;
            progress[i] += (float) Math.pow(2, (temperature - point) / 200.0);
            melting++;
            sum += Math.round(Math.min(100, progress[i] / MELT_TICKS * 100));
            if (progress[i] >= MELT_TICKS) {
                melt = melt.plus(content.get());
                stack.shrink(1);
                progress[i] = 0;
                level.playSound(null, pos, ModSounds.CRUCIBLE_MELT.get(), SoundSource.BLOCKS, 0.5f,
                        0.8f + level.getRandom().nextFloat() * 0.3f);
                changed = true;
            }
        }
        meltingPercent = melting == 0 ? 0 : sum / melting;
        return changed || melting > 0;
    }

    private CrucibleStatus computeStatus(boolean forgeBelow) {
        if (meltingPercent > 0) return CrucibleStatus.MELTING;
        if (!melt.isEmpty()) return isMolten() ? CrucibleStatus.MOLTEN : CrucibleStatus.SOLID;
        if (!forgeBelow) return CrucibleStatus.NO_FORGE;
        return temperature > Heat.AMBIENT + 10 ? CrucibleStatus.HEATING : CrucibleStatus.COLD;
    }

    /** Why a pour cannot start, as a lang key suffix; empty when it can. */
    public Optional<String> pourProblem() {
        ItemStack mold = items.get(MOLD_SLOT);
        if (!(mold.getItem() instanceof CastMoldItem cast) || mold.has(ModDataComponents.CAST_CONTENTS.get())) {
            return Optional.of("no_mold");
        }
        if (pourLeft > 0) return Optional.of("pouring");
        if (!isMolten()) return Optional.of("not_molten");
        if (melt.total() < cast.units()) return Optional.of("not_enough");
        Optional<Metal> result = result();
        if (cast.type() == null) {
            if (result.isEmpty() && melt.units().size() < 2) return Optional.of("no_alloy");
            if (result.isPresent() && !result.get().hasIngot()) return Optional.of("no_alloy");
        } else if (result.isEmpty() || !result.get().isToolMetal()) {
            return Optional.of("no_alloy");
        }
        return Optional.empty();
    }

    /** Starts pouring into the mold; the metal flows over the next few ticks. */
    public boolean startPour() {
        if (pourProblem().isPresent()) return false;
        CastMoldItem cast = (CastMoldItem) items.get(MOLD_SLOT).getItem();
        pourLeft = pourTotal = cast.units();
        poured = Melt.EMPTY;
        if (level != null) {
            level.playSound(null, worldPosition, ModSounds.CRUCIBLE_POUR.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        setChanged();
        return true;
    }

    private boolean pour(Level level, BlockPos pos) {
        if (pourLeft <= 0) return false;
        ItemStack mold = items.get(MOLD_SLOT);
        if (!(mold.getItem() instanceof CastMoldItem) || !isMolten()) {
            // The mold was taken away or the metal froze mid-pour: what flowed so far goes back.
            melt = melt.plus(poured);
            poured = Melt.EMPTY;
            pourLeft = pourTotal = 0;
            return true;
        }
        int step = Math.min(POUR_PER_TICK, pourLeft);
        Melt before = melt;
        melt = melt.minus(step);
        poured = poured.plus(subtract(before, melt));
        pourLeft -= step;
        if (pourLeft == 0) {
            mold.set(ModDataComponents.CAST_CONTENTS.get(), poured);
            mold.set(DataComponents.MAX_STACK_SIZE, 1);
            Heat.set(mold, temperature, level.getGameTime());
            poured = Melt.EMPTY;
            pourTotal = 0;
            level.playSound(null, pos, ModSounds.QUENCH.get(), SoundSource.BLOCKS, 0.4f, 1.6f);
        }
        return true;
    }

    /** What left the melt between two states. */
    private static Melt subtract(Melt before, Melt after) {
        java.util.Map<Metal, Integer> out = new java.util.EnumMap<>(Metal.class);
        before.units().forEach((metal, u) -> {
            int d = u - after.units().getOrDefault(metal, 0);
            if (d > 0) out.put(metal, d);
        });
        return new Melt(out, before.qualityUnits() - after.qualityUnits());
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == MOLD_SLOT) return stack.getItem() instanceof CastMoldItem;
        return roomFor(stack, items.get(slot)) >= stack.getCount();
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".crucible");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CrucibleMenu(id, inventory, this, data);
    }

    /** Broken crucibles keep their contents, melt and heat on the item (spec 7.1). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        Melt all = melt.plus(poured);
        if (!all.isEmpty()) components.set(ModDataComponents.CRUCIBLE_MELT.get(), all);
        if (temperature > Heat.AMBIENT + 5 && level != null) {
            components.set(ModDataComponents.TEMPERATURE.get(), new Temperature(temperature, level.getGameTime()));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        melt = components.getOrDefault(ModDataComponents.CRUCIBLE_MELT.get(), Melt.EMPTY);
        Temperature t = components.get(ModDataComponents.TEMPERATURE.get());
        if (t != null && level != null) temperature = Heat.get(t, level.getGameTime());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        melt = input.read("melt", Melt.CODEC).orElse(Melt.EMPTY);
        poured = input.read("poured", Melt.CODEC).orElse(Melt.EMPTY);
        temperature = input.getFloatOr("temperature", Heat.AMBIENT);
        pourLeft = input.getIntOr("pour_left", 0);
        pourTotal = input.getIntOr("pour_total", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.store("melt", Melt.CODEC, melt);
        output.store("poured", Melt.CODEC, poured);
        output.putFloat("temperature", temperature);
        output.putInt("pour_left", pourLeft);
        output.putInt("pour_total", pourTotal);
    }
}
