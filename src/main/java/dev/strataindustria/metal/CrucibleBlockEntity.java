package dev.strataindustria.metal;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.Temperature;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
    protected static final float FORGE_RATE = 0.030f;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_MELTING = 2;
    public static final int DATA_POUR = 3;
    public static final int DATA_SLOT_PROGRESS = 4;
    public static final int DATA_UNITS = DATA_SLOT_PROGRESS + INPUT_SLOTS;
    public static final int DATA_CAPACITY = DATA_UNITS + Metal.values().length;
    public static final int DATA_MAX_TEMPERATURE = DATA_CAPACITY + 1;
    public static final int DATA_REFRACTORY = DATA_CAPACITY + 2;
    public static final int DATA_COUNT = DATA_CAPACITY + 3;
    /** Tier 4 spec 6.1: the refractory crucible's pot and heat rating. */
    public static final int REFRACTORY_CAPACITY = 600;
    public static final int REFRACTORY_MAX_TEMPERATURE = 1700;
    /** How long the screen shows that spare carbon burned off. */
    static final int BURN_OFF_TICKS = 100;

    private NonNullList<ItemStack> items = NonNullList.withSize(slotCount(), ItemStack.EMPTY);
    private final float[] progress = new float[INPUT_SLOTS];
    private Melt melt = Melt.EMPTY;
    private float temperature = Heat.AMBIENT;
    private CrucibleStatus status = CrucibleStatus.COLD;
    private int meltingPercent;
    /** Units still to pour, the share of the melt set aside for this pour, and what has flowed so far. */
    private int pourLeft;
    private int pourTotal;
    private Melt pourSlice = Melt.EMPTY;
    private Melt poured = Melt.EMPTY;
    /** Ticks left of white fumes over an arsenical pour (uniqueness 4.3); not saved. */
    private int fumeTicks;
    private boolean forgeTooHot;
    private boolean carbonWaiting;
    private boolean redstoneWaiting;
    private boolean calcineShort;
    private int burnedOff;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_TEMPERATURE) return Math.round(temperature);
            if (index == DATA_STATUS) return status.ordinal();
            if (index == DATA_MELTING) return meltingPercent;
            if (index == DATA_POUR) return pourTotal == 0 ? 0 : Math.round(100 * (1 - pourLeft / (float) pourTotal));
            if (index < DATA_UNITS) return Math.round(progress[index - DATA_SLOT_PROGRESS]);
            if (index < DATA_CAPACITY) {
                // What is still in the pot, counting the share that has not yet flowed into the mold.
                Metal metal = Metal.values()[index - DATA_UNITS];
                return melt.units().getOrDefault(metal, 0) + pourSlice.units().getOrDefault(metal, 0);
            }
            if (index == DATA_CAPACITY) return capacityOf();
            if (index == DATA_MAX_TEMPERATURE) return maxTemperature();
            if (index == DATA_REFRACTORY) return refractory() ? 1 : 0;
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
        this(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    public CrucibleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Where the pot's heat comes from this tick: whether there is a source, its temperature, and how fast it heats. */
    protected record HeatSource(boolean present, float target, float rate) {
        static final HeatSource NONE = new HeatSource(false, Heat.AMBIENT, Heat.AMBIENT_RATE);
    }

    /** How many slots the block entity has; the smelter adds mold handling past the crucible's ten. */
    protected int slotCount() {
        return INPUT_SLOTS + 1;
    }

    /** A crucible takes its heat from a forge directly below. */
    protected HeatSource heatSource(Level level, BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof ForgeBlockEntity forge ? new HeatSource(true, forge.temperature(), FORGE_RATE) : HeatSource.NONE;
    }

    /** The status shown when nothing is heating the pot. */
    protected CrucibleStatus noHeatStatus() {
        return CrucibleStatus.NO_FORGE;
    }

    /** Ticks of white fumes still to rise over an arsenical pour. */
    public int fumeTicks() {
        return fumeTicks;
    }

    /** Whether a pour is still running (metal set aside and flowing into the mold). */
    public boolean pouring() {
        return pourLeft > 0;
    }

    protected NonNullList<ItemStack> items() {
        return items;
    }

    /** The clay crucible's capacity. */
    public static int capacity() {
        return Config.CRUCIBLE_CAPACITY.getAsInt();
    }

    public boolean refractory() {
        return getType() == Tier4BlockEntities.REFRACTORY_CRUCIBLE.get();
    }

    public int capacityOf() {
        return refractory() ? REFRACTORY_CAPACITY : capacity();
    }

    /** Tier 4 spec 3 and 6.1: the clay pot holds at 1400 degrees, the refractory one at 1700. */
    public int maxTemperature() {
        return refractory() ? REFRACTORY_MAX_TEMPERATURE : Config.CLAY_CRUCIBLE_MAX.getAsInt();
    }

    public Melt melt() {
        return melt;
    }

    public float temperature() {
        return temperature;
    }

    /** The melt's result when molten: one metal, an alloy, or empty for an unknown mix. */
    /** The screen's status line, for game tests. */
    public CrucibleStatus status() {
        return status;
    }

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
        int free = capacityOf() - melt.total() - pendingUnits() - pourLeft + replaced;
        return Math.max(0, free / each);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        HeatSource source = crucible.heatSource(level, pos);
        boolean forgeBelow = source.present();
        float target = source.target();
        int max = crucible.maxTemperature();
        crucible.forgeTooHot = target > max;
        target = Math.min(target, max);
        float rate = source.rate();
        float before = crucible.temperature;
        crucible.temperature = (float) (target + (crucible.temperature - target) * Math.exp(-rate / 20.0));

        boolean changed = Math.abs(crucible.temperature - before) > 0.01f;
        changed |= crucible.meltInputs(level, pos);
        changed |= crucible.burnOffCarbon(level, pos);
        changed |= crucible.pour(level, pos);
        if (crucible.fumeTicks > 0 && level instanceof net.minecraft.server.level.ServerLevel server) {
            dev.strataindustria.bronze.Fumes.tick(server, pos, crucible.fumeTicks--);
        }
        CrucibleStatus was = crucible.status;
        crucible.status = crucible.computeStatus(forgeBelow);
        if (crucible.status == CrucibleStatus.MOLTEN && was != CrucibleStatus.MOLTEN) {
            Journal.awardNear(level, pos, Journal.CRUCIBLE_MOLTEN);
            if (crucible.melt.units().containsKey(Metal.WROUGHT_IRON)) Journal.awardNear(level, pos, Journal.MOLTEN_IRON);
        }
        if (changed) setChanged(level, pos, state);
    }

    private boolean meltInputs(Level level, BlockPos pos) {
        boolean changed = false;
        int melting = 0, sum = 0;
        carbonWaiting = false;
        redstoneWaiting = false;
        calcineShort = false;
        boolean calcine = hasCalcine();
        // Carbon already promised to calcine pieces that are reducing this tick.
        int reserved = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            Optional<Melt> content = MetalContent.of(stack);
            if (content.isEmpty()) {
                progress[i] = 0;
                continue;
            }
            int point = mixMeltingPoint(content.get());
            if (isCarbon(content.get())) {
                // Tier 4 spec 4.2: carbon dust dissolves only into molten iron, or to reduce calcine.
                boolean iron = melt.units().containsKey(Metal.WROUGHT_IRON) && isMolten();
                if (!iron && !calcine) {
                    progress[i] = 0;
                    carbonWaiting = true;
                    continue;
                }
                point = iron ? mixMeltingPoint(melt) : MetalContent.CALCINE_REDUCTION;
            } else if (isRedstone(content.get())) {
                // Tier 5 spec 4.2: redstone dissolves only into molten copper.
                if (!melt.units().containsKey(Metal.COPPER) || !isMolten()) {
                    progress[i] = 0;
                    redstoneWaiting = true;
                    continue;
                }
                point = mixMeltingPoint(melt);
            } else if (MetalContent.isCalcine(stack)) {
                // Each piece takes a tenth of its zinc in carbon from the melt before it can melt.
                point = MetalContent.CALCINE_REDUCTION;
                int need = MetalContent.carbonFor(content.get());
                if (melt.units().getOrDefault(Metal.CARBON, 0) - reserved < need) {
                    progress[i] = 0;
                    // Short only once it is hot enough and no more dust is waiting to dissolve.
                    if (temperature >= point && !carbonInSlots()) calcineShort = true;
                    continue;
                }
                if (temperature >= point) reserved += need;
            }
            if (temperature < point) continue;
            progress[i] += (float) Math.pow(2, (temperature - point) / 200.0);
            melting++;
            sum += Math.round(Math.min(100, progress[i] / MELT_TICKS * 100));
            if (progress[i] >= MELT_TICKS) {
                boolean reduce = MetalContent.isCalcine(stack);
                melt = melt.plus(content.get());
                if (reduce) melt = withoutCarbon(melt, MetalContent.carbonFor(content.get()));
                stack.shrink(1);
                progress[i] = 0;
                level.playSound(null, pos, reduce ? Tier4Sounds.CALCINE_REDUCE.get() : ModSounds.CRUCIBLE_MELT.get(), SoundSource.BLOCKS, 0.5f,
                        0.8f + level.getRandom().nextFloat() * 0.3f);
                changed = true;
            }
        }
        meltingPercent = melting == 0 ? 0 : sum / melting;
        return changed || melting > 0;
    }

    private boolean carbonInSlots() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (MetalContent.of(items.get(i)).map(CrucibleBlockEntity::isCarbon).orElse(false)) return true;
        }
        return false;
    }

    private boolean hasCalcine() {
        for (int i = 0; i < INPUT_SLOTS; i++) if (MetalContent.isCalcine(items.get(i))) return true;
        return false;
    }

    /** The melt with {@code amount} units of carbon taken out (spent reducing calcine, or burned off). */
    private static Melt withoutCarbon(Melt melt, int amount) {
        java.util.Map<Metal, Integer> left = new java.util.EnumMap<>(melt.units());
        int carbon = left.getOrDefault(Metal.CARBON, 0);
        int taken = Math.min(carbon, amount);
        if (carbon - taken > 0) left.put(Metal.CARBON, carbon - taken);
        else left.remove(Metal.CARBON);
        int total = melt.total();
        return new Melt(left, total == 0 ? 0 : Math.round(melt.qualityUnits() * (float) (total - taken) / total));
    }

    private static boolean isRedstone(Melt content) {
        return content.units().size() == 1 && content.units().containsKey(Metal.REDSTONE);
    }

    private static boolean isCarbon(Melt content) {
        return content.units().size() == 1 && content.units().containsKey(Metal.CARBON);
    }

    /** Tier 4 spec 4.2: carbon with no iron to hold it burns off once the rest is molten. */
    private boolean burnOffCarbon(Level level, BlockPos pos) {
        if (burnedOff > 0) burnedOff--;
        if (!melt.units().containsKey(Metal.CARBON) || melt.units().containsKey(Metal.WROUGHT_IRON)) return false;
        // Calcine still in the slots keeps its carbon waiting for it.
        if (meltingPercent > 0 || !isMolten() || hasCalcine()) return false;
        melt = withoutCarbon(melt, melt.units().get(Metal.CARBON));
        burnedOff = BURN_OFF_TICKS;
        level.playSound(null, pos, Tier4Sounds.CARBON_BURN.get(), SoundSource.BLOCKS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        return true;
    }

    private CrucibleStatus computeStatus(boolean forgeBelow) {
        if (meltingPercent > 0) return CrucibleStatus.MELTING;
        if (calcineShort) return CrucibleStatus.CALCINE_SHORT;
        if (burnedOff > 0 && isMolten()) return CrucibleStatus.CARBON_BURNED;
        boolean atLimit = forgeTooHot && temperature >= maxTemperature() - 5;
        if (!melt.isEmpty()) {
            if (redstoneWaiting) return CrucibleStatus.REDSTONE_WAITING;
            if (isMolten()) return CrucibleStatus.MOLTEN;
            if (carbonWaiting && !melt.units().containsKey(Metal.WROUGHT_IRON)) return CrucibleStatus.CARBON_WAITING;
            return atLimit ? CrucibleStatus.AT_LIMIT : CrucibleStatus.SOLID;
        }
        if (carbonWaiting) return CrucibleStatus.CARBON_WAITING;
        if (redstoneWaiting) return CrucibleStatus.REDSTONE_WAITING;
        if (!forgeBelow) return noHeatStatus();
        if (atLimit) return CrucibleStatus.AT_LIMIT;
        return temperature > Heat.AMBIENT + 10 ? CrucibleStatus.HEATING : CrucibleStatus.COLD;
    }

    /** Why a pour cannot start, as a lang key suffix; empty when it can. */
    public Optional<String> pourProblem() {
        ItemStack mold = targetMold();
        if (!(mold.getItem() instanceof CastMoldItem cast) || mold.has(ModDataComponents.CAST_CONTENTS.get())) {
            return Optional.of("no_mold");
        }
        if (pourLeft > 0) return Optional.of("pouring");
        if (!isMolten()) return Optional.of("not_molten");
        if (melt.total() < cast.units()) return Optional.of("not_enough");
        // Tier 4 spec 3: clay molds crack under metal hotter than 1300 degrees.
        if (!cast.takes(mixMeltingPoint(melt))) return Optional.of("mold_too_weak");
        Optional<Metal> result = result();
        if (cast.isBell()) {
            if (result.isEmpty() || !CastMoldItem.ringsAsBell(result.get())) return Optional.of("no_alloy");
        } else if (cast.isGear()) {
            if (result.isEmpty() || !result.get().hasGear()) return Optional.of("no_gear");
        } else if (cast.type() == null) {
            if (result.isEmpty() && melt.units().size() < 2) return Optional.of("no_alloy");
            if (result.isPresent() && !result.get().hasIngot()) return Optional.of("no_alloy");
        } else if (result.isEmpty() || !result.get().isToolMetal()) {
            return Optional.of("no_alloy");
        }
        return Optional.empty();
    }

    /** The first casting table beside the pot, at its height or one lower, that still has an empty mold on it. */
    private @Nullable CastingTableBlockEntity tableBeside() {
        if (level == null) return null;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int drop = 0; drop <= 1; drop++) {
                if (level.getBlockEntity(worldPosition.relative(side).below(drop)) instanceof CastingTableBlockEntity table && table.hasEmptyMold()) return table;
            }
        }
        return null;
    }

    /** The table that holds {@code mold}, if the pot is pouring onto one. */
    private @Nullable CastingTableBlockEntity tableBeside(ItemStack mold) {
        if (level == null) return null;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int drop = 0; drop <= 1; drop++) {
                if (level.getBlockEntity(worldPosition.relative(side).below(drop)) instanceof CastingTableBlockEntity table
                        && table.molds().stream().anyMatch(m -> m == mold)) return table;
            }
        }
        return null;
    }

    /** Where a pour goes: the mold in the pot's own slot, or with that slot bare, the next empty mold on a casting table beside it. */
    private ItemStack targetMold() {
        ItemStack own = items.get(MOLD_SLOT);
        if (!own.isEmpty()) return own;
        CastingTableBlockEntity table = tableBeside();
        return table == null ? ItemStack.EMPTY : table.nextEmptyMold();
    }

    /** Starts pouring into the mold; the metal flows over the next few ticks. */
    public boolean startPour() {
        if (pourProblem().isPresent()) return false;
        CastMoldItem cast = (CastMoldItem) targetMold().getItem();
        pourLeft = pourTotal = cast.units();
        // The mold's share is set aside whole, so it keeps the melt's make-up (1% carbon stays 1%).
        Melt after = melt.minus(pourTotal);
        pourSlice = subtract(melt, after);
        melt = after;
        poured = Melt.EMPTY;
        if (level != null) {
            level.playSound(null, worldPosition, pourSound(), SoundSource.BLOCKS, 0.8f, 1.0f);
            if (pourSlice.units().getOrDefault(Metal.ARSENIC, 0) > 0) {
                fumeTicks = pourTotal / POUR_PER_TICK + dev.strataindustria.bronze.Fumes.LINGER;
                level.playSound(null, worldPosition, dev.strataindustria.bronze.BronzeRegistry.FUMES.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
            }
        }
        setChanged();
        return true;
    }

    protected net.minecraft.sounds.SoundEvent pourSound() {
        return ModSounds.CRUCIBLE_POUR.get();
    }

    private boolean pour(Level level, BlockPos pos) {
        if (pourLeft <= 0) return false;
        ItemStack mold = targetMold();
        Melt all = melt.plus(pourSlice);
        if (!(mold.getItem() instanceof CastMoldItem) || temperature < mixMeltingPoint(all)) {
            // The mold was taken away or the metal froze mid-pour: what flowed so far goes back.
            melt = all.plus(poured);
            pourSlice = Melt.EMPTY;
            poured = Melt.EMPTY;
            pourLeft = pourTotal = 0;
            return true;
        }
        int step = Math.min(POUR_PER_TICK, pourLeft);
        Melt before = pourSlice;
        pourSlice = pourLeft - step <= 0 ? Melt.EMPTY : pourSlice.minus(step);
        poured = poured.plus(subtract(before, pourSlice));
        pourLeft -= step;
        if (pourLeft == 0) {
            mold.set(ModDataComponents.CAST_CONTENTS.get(), poured);
            mold.set(DataComponents.MAX_STACK_SIZE, 1);
            Heat.set(mold, temperature, level.getGameTime());
            poured = Melt.EMPTY;
            pourTotal = 0;
            level.playSound(null, pos, ModSounds.QUENCH.get(), SoundSource.BLOCKS, 0.4f, 1.6f);
            if (items.get(MOLD_SLOT).isEmpty() && tableBeside(mold) instanceof CastingTableBlockEntity table) {
                // Poured onto a casting table: tell it, and carry on into the next empty mold while the metal lasts.
                table.changed();
                Journal.awardNear(level, pos, Journal.CASTING_TABLE_POURED);
                if (pourProblem().isEmpty()) startPour();
            }
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
        return accepts(stack, refractory()) && roomFor(stack, items.get(slot)) >= stack.getCount();
    }

    /** Anything with metal, except iron, which no tier 3 fire can melt (tier 3 spec 4.1). */
    public static boolean accepts(ItemStack stack) {
        return accepts(stack, false);
    }

    /**
     * What a crucible takes. Only the refractory crucible takes iron and steel (tier 4 spec 3), and no
     * crucible takes iron ore: that goes to a bloomery or a blast furnace.
     */
    public static boolean accepts(ItemStack stack, boolean refractory) {
        if (isIronOre(stack)) return false;
        return MetalContent.of(stack).map(melt -> melt.units().keySet().stream()
                .allMatch(metal -> metal.meltsInCrucible() || refractory && metal.meltingPoint() <= REFRACTORY_MAX_TEMPERATURE)).orElse(false);
    }

    public static boolean isIronOre(ItemStack stack) {
        return stack.is(ModTags.Items.IRON_ORES) || stack.is(Items.RAW_IRON) || stack.is(ModItems.BLOOMERY_SLAG.get())
                || stack.has(ModDataComponents.BLOOM_CONTENTS.get());
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + (refractory() ? ".refractory_crucible" : ".crucible"));
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
        Melt all = melt.plus(pourSlice).plus(poured);
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
        pourSlice = input.read("pour_slice", Melt.CODEC).orElse(Melt.EMPTY);
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
        output.store("pour_slice", Melt.CODEC, pourSlice);
        output.putFloat("temperature", temperature);
        output.putInt("pour_left", pourLeft);
        output.putInt("pour_total", pourTotal);
    }
}
