package dev.strataindustria.ironworks;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
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
import org.jspecify.annotations.Nullable;

/**
 * The blast furnace (tier 4 spec 12.1). Burden goes into three buffers: iron units at the item's melt
 * value, fuel and flux. With air on a tuyere the hearth heats for {@code warmupTicks}, then every
 * {@code ticksPerIngot} (halved with two blowers' worth of air) it taps one pig iron ingot for 100 iron
 * units, half a coke (or a charcoal) and half a flux, with half a slag on the side. A stopped furnace
 * stays hot for 30 seconds, then cools.
 */
public class BlastFurnaceBlockEntity extends BaseContainerBlockEntity implements FurnaceHost {
    public static final int ORE = 0, FUEL = 1, FLUX = 2, PIG_IRON = 3, SLAG = 4, SLOTS = 5;
    public static final int MAX_IRON = 1600;
    /** Fuel is counted in quarters of a coke: coke is 4, charcoal 2, and an ingot burns 2. */
    public static final int MAX_FUEL = 32 * 4, COKE_FUEL = 4, CHARCOAL_FUEL = 2, COKE_BLOCK_FUEL = 9 * COKE_FUEL, FUEL_PER_INGOT = 2;
    /** Flux in halves: one flux item is 2, an ingot takes 1. */
    public static final int MAX_FLUX = 32 * 2, FLUX_ITEM = 2, FLUX_PER_INGOT = 1;
    public static final int IRON_PER_INGOT = MetalContent.INGOT_UNITS;
    /** At most two blowers' worth of air counts. */
    public static final int MAX_AIR = 2;
    /** How long a stopped furnace keeps its heat before it starts to cool. */
    public static final int STAYS_HOT = 600;
    public static final int DATA_IRON = 0, DATA_FUEL = 1, DATA_FLUX = 2, DATA_WARMTH = 3, DATA_PROGRESS = 4, DATA_STATUS = 5,
            DATA_PROBLEM = 6, DATA_WHERE = 7, DATA_AIR = 8, DATA_COUNT = 9;

    public enum Status {
        INCOMPLETE, NO_AIR, NEEDS_FUEL, HEATING, NEEDS_IRON, NEEDS_FLUX, OUTPUT_FULL, RUNNING;

        public String key() {
            return StrataIndustria.MOD_ID + ".blast_furnace.status." + name().toLowerCase(Locale.ROOT);
        }

        /** Whether the hearth is burning: the controller and throat show fire. */
        public boolean burning() {
            return this == HEATING || this == RUNNING;
        }
    }

    /** What one item adds to the buffers. */
    public record Charge(int iron, int qualityUnits, int fuel, int flux) {
        public static final Charge NONE = new Charge(0, 0, 0, 0);

        public boolean isEmpty() {
            return iron <= 0 && fuel <= 0 && flux <= 0;
        }
    }

    private static final int[] CHARGE_SLOTS = {ORE, FUEL, FLUX};
    private static final int[] TAP_SLOTS = {PIG_IRON, SLAG};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int iron;
    private int ironQuality;
    private int fuel;
    private int flux;
    private int slagHalves;
    private int progress;
    private int warmth;
    private int idle = STAYS_HOT;
    private int air;
    private boolean built;
    /** Whether the structure has been looked at since this block entity loaded. */
    private boolean checked;
    private Status status = Status.INCOMPLETE;
    private BlastFurnaceStructure.Result structure = BlastFurnaceStructure.INCOMPLETE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_IRON -> iron;
                case DATA_FUEL -> fuel;
                case DATA_FLUX -> flux;
                case DATA_WARMTH -> warmup() <= 0 ? 1000 : (int) ((long) warmth * 1000 / warmup());
                case DATA_PROGRESS -> (int) ((long) progress * 1000 / ticksPerIngot());
                case DATA_STATUS -> status.ordinal();
                case DATA_PROBLEM -> structure.problem().ordinal();
                case DATA_WHERE -> where(structure.at());
                case DATA_AIR -> air;
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

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.BLAST_FURNACE.get(), pos, state);
    }

    // ------------------------------------------------------------------ burden

    /** What an item puts into the furnace: wrought iron units, fuel and flux (spec 12.1). */
    public static Charge charge(ItemStack stack) {
        if (stack.isEmpty()) return Charge.NONE;
        int fuel = stack.is(Tier4Items.COKE.get()) ? COKE_FUEL
                : stack.is(Items.CHARCOAL) ? CHARCOAL_FUEL
                : stack.is(Tier4Items.COKE_BLOCK.get()) ? COKE_BLOCK_FUEL : 0;
        int flux = stack.is(ModTags.Items.FLUX) ? FLUX_ITEM : 0;
        int iron = 0, quality = 0;
        // Pig iron is the furnace's own product; it goes on to the converter or the crucible.
        if (!stack.is(ModItems.ingot(Metal.PIG_IRON))) {
            Melt melt = MetalContent.of(stack).orElse(null);
            if (melt != null) {
                iron = melt.units().getOrDefault(Metal.WROUGHT_IRON, 0);
                quality = iron * melt.quality();
            }
        }
        return new Charge(iron, quality, fuel, flux);
    }

    /** Which charge slot an item belongs in: anything with iron is ore, then fuel, then flux. */
    public static boolean fitsSlot(int slot, ItemStack stack) {
        Charge charge = charge(stack);
        return switch (slot) {
            case ORE -> charge.iron() > 0;
            case FUEL -> charge.fuel() > 0;
            case FLUX -> charge.flux() > 0 && charge.iron() <= 0;
            default -> false;
        };
    }

    private boolean roomFor(Charge charge) {
        return iron + charge.iron() <= MAX_IRON && fuel + charge.fuel() <= MAX_FUEL && flux + charge.flux() <= MAX_FLUX;
    }

    /** Takes items from the charge slots into the buffers while there is room for them. */
    private boolean absorb() {
        boolean changed = false;
        for (int slot : CHARGE_SLOTS) {
            ItemStack stack = items.get(slot);
            Charge charge = charge(stack);
            if (charge.isEmpty()) continue;
            while (!stack.isEmpty() && roomFor(charge)) {
                iron += charge.iron();
                ironQuality += charge.qualityUnits();
                fuel += charge.fuel();
                flux += charge.flux();
                stack.shrink(1);
                changed = true;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ running

    private Direction facing() {
        return getBlockState().getValue(BlastFurnaceBlock.FACING);
    }

    public BlastFurnaceStructure.Result checkStructure() {
        if (level == null) return structure;
        structure = BlastFurnaceStructure.check(level, worldPosition, facing());
        if (structure.complete()) {
            claim(structure.tap());
            claim(structure.hatch());
        }
        return structure;
    }

    private void claim(BlockPos pos) {
        if (level != null && level.getBlockEntity(pos) instanceof FurnaceHatchBlockEntity hatch) hatch.claim(worldPosition);
    }

    /** Air from blowers on the tuyeres' outer faces, at most two blowers' worth. */
    private int countAir(Level level) {
        int total = 0;
        for (BlastFurnaceStructure.Opening tuyere : structure.tuyeres()) {
            BlockPos outside = tuyere.pos().relative(tuyere.out());
            if (level.getBlockEntity(outside) instanceof AirBlast blast) total += blast.airOut(tuyere.out().getOpposite());
        }
        return Math.min(MAX_AIR, total);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlastFurnaceBlockEntity furnace) {
        if (level.getGameTime() % 20 == 0 || !furnace.checked) {
            furnace.checked = true;
            furnace.checkStructure();
        }
        boolean complete = furnace.structure.complete();
        if (complete && !furnace.built) {
            level.playSound(null, pos, Tier4Sounds.MULTIBLOCK_FORM.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        if (complete != furnace.built) {
            furnace.built = complete;
            furnace.setChanged();
        }
        boolean changed = complete && furnace.absorb();
        Status before = furnace.status;
        furnace.status = furnace.work(level, pos);
        if (furnace.status != before || changed || furnace.status.burning()) furnace.setChanged();

        boolean lit = furnace.status.burning();
        if (state.getValue(BlastFurnaceBlock.LIT) != lit) level.setBlock(pos, state.setValue(BlastFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        if (complete) {
            BlockState tap = level.getBlockState(furnace.structure.tap());
            boolean hot = furnace.status == Status.RUNNING;
            if (tap.hasProperty(TapHatchBlock.HOT) && tap.getValue(TapHatchBlock.HOT) != hot) {
                level.setBlock(furnace.structure.tap(), tap.setValue(TapHatchBlock.HOT, hot), Block.UPDATE_ALL);
            }
        }
        if (lit && Math.floorMod(level.getGameTime() + pos.asLong(), 50) == 0) {
            level.playSound(null, pos, Tier4Sounds.BLAST_FURNACE_ROAR.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
    }

    private Status work(Level level, BlockPos pos) {
        if (!structure.complete()) {
            air = 0;
            progress = 0;
            cool();
            return Status.INCOMPLETE;
        }
        air = countAir(level);
        Status stopped = air <= 0 ? Status.NO_AIR : fuel < FUEL_PER_INGOT ? Status.NEEDS_FUEL : null;
        if (stopped != null) {
            cool();
            return stopped;
        }
        idle = 0;
        if (warmth < warmup()) {
            warmth++;
            return Status.HEATING;
        }
        stopped = iron < IRON_PER_INGOT ? Status.NEEDS_IRON : flux < FLUX_PER_INGOT ? Status.NEEDS_FLUX : !outputFits() ? Status.OUTPUT_FULL : null;
        // With air and fuel the hearth keeps its heat even while it waits for burden or room.
        if (stopped != null) return stopped;
        progress += air;
        if (progress < ticksPerIngot()) return Status.RUNNING;
        progress = 0;
        tapIngot((ServerLevel) level);
        return Status.RUNNING;
    }

    /** A furnace that is not burning holds its heat for {@link #STAYS_HOT} ticks, then loses it twice as fast as it gained it. */
    private void cool() {
        if (idle < STAYS_HOT) idle++;
        else if (warmth > 0) warmth = Math.max(0, warmth - 2);
    }

    private boolean outputFits() {
        if (!fits(PIG_IRON, pigIron())) return false;
        return slagHalves + 1 < 2 || fits(SLAG, new ItemStack(Tier4Items.SLAG.get()));
    }

    private boolean fits(int slot, ItemStack made) {
        ItemStack out = items.get(slot);
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, made) && out.getCount() < out.getMaxStackSize();
    }

    /** The next ingot: pig iron carrying the unit-weighted quality of the iron in the burden. */
    private ItemStack pigIron() {
        ItemStack ingot = new ItemStack(ModItems.ingot(Metal.PIG_IRON));
        int material = iron <= 0 ? 0 : Math.round(ironQuality / (float) iron);
        if (material != 0) ingot.set(ModDataComponents.QUALITY.get(), new Quality(material, 0));
        return ingot;
    }

    private void tapIngot(ServerLevel level) {
        ItemStack ingot = pigIron();
        int material = ingot.has(ModDataComponents.QUALITY.get()) ? ingot.get(ModDataComponents.QUALITY.get()).material() : 0;
        iron -= IRON_PER_INGOT;
        ironQuality = iron <= 0 ? 0 : ironQuality - material * IRON_PER_INGOT;
        fuel -= FUEL_PER_INGOT;
        flux -= FLUX_PER_INGOT;
        insert(PIG_IRON, ingot);
        if (++slagHalves >= 2) {
            slagHalves -= 2;
            insert(SLAG, new ItemStack(Tier4Items.SLAG.get()));
        }
        BlockPos tap = structure.tap();
        level.playSound(null, tap, Tier4Sounds.BLAST_FURNACE_TAP.get(), SoundSource.BLOCKS, 0.9f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        Direction out = level.getBlockState(tap).hasProperty(TapHatchBlock.FACING) ? level.getBlockState(tap).getValue(TapHatchBlock.FACING) : facing();
        level.sendParticles(ParticleTypes.LAVA, tap.getX() + 0.5 + out.getStepX() * 0.6, tap.getY() + 0.4, tap.getZ() + 0.5 + out.getStepZ() * 0.6,
                4, 0.15, 0.1, 0.15, 0.0);
        Journal.awardNear(level, worldPosition, Journal.BLAST_FURNACE);
    }

    private void insert(int slot, ItemStack made) {
        ItemStack out = items.get(slot);
        if (out.isEmpty()) items.set(slot, made);
        else out.grow(made.getCount());
    }

    private static int warmup() {
        return Config.BLAST_FURNACE_WARMUP.getAsInt();
    }

    private static int ticksPerIngot() {
        return Math.max(1, Config.BLAST_FURNACE_TICKS_PER_INGOT.getAsInt());
    }

    /**
     * Where a missing block is, for the screen: layer 1 to 5 times 9, plus row (front, middle, back) times
     * 3, plus column (left, centre, right) as seen standing at the controller.
     */
    private int where(BlockPos at) {
        if (level == null || structure.complete()) return 0;
        Direction back = facing().getOpposite();
        Direction left = back.getCounterClockWise();
        BlockPos rel = at.subtract(worldPosition);
        int depth = rel.getX() * back.getStepX() + rel.getZ() * back.getStepZ();
        int side = rel.getX() * left.getStepX() + rel.getZ() * left.getStepZ();
        int layer = Math.clamp(rel.getY(), 0, BlastFurnaceStructure.HEIGHT - 1);
        return layer * 9 + Math.clamp(depth, 0, 2) * 3 + (1 - Math.clamp(side, -1, 1));
    }

    // ------------------------------------------------------------------ for tests and the screen

    public Status status() {
        return status;
    }

    public int iron() {
        return iron;
    }

    public int fuel() {
        return fuel;
    }

    public int flux() {
        return flux;
    }

    public int air() {
        return air;
    }

    /** Skips the warm-up, for tests. */
    public void heatUp() {
        warmth = warmup();
        idle = 0;
    }

    // ------------------------------------------------------------------ FurnaceHost

    @Override
    public int[] chargeSlots() {
        return CHARGE_SLOTS;
    }

    @Override
    public int[] tapSlots() {
        return TAP_SLOTS;
    }

    @Override
    public boolean usesPart(BlockPos part) {
        return structure.complete() && (part.equals(structure.tap()) || part.equals(structure.hatch()));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? TAP_SLOTS : CHARGE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == PIG_IRON || slot == SLAG;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fitsSlot(slot, stack);
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".blast_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        checkStructure();
        return new BlastFurnaceMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        iron = in.getIntOr("iron", 0);
        ironQuality = in.getIntOr("iron_quality", 0);
        fuel = in.getIntOr("fuel", 0);
        flux = in.getIntOr("flux", 0);
        slagHalves = in.getIntOr("slag_halves", 0);
        progress = in.getIntOr("progress", 0);
        warmth = in.getIntOr("warmth", 0);
        idle = in.getIntOr("idle", STAYS_HOT);
        built = in.getIntOr("built", 0) != 0;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putInt("iron", iron);
        out.putInt("iron_quality", ironQuality);
        out.putInt("fuel", fuel);
        out.putInt("flux", flux);
        out.putInt("slag_halves", slagHalves);
        out.putInt("progress", progress);
        out.putInt("warmth", warmth);
        out.putInt("idle", idle);
        out.putInt("built", built ? 1 : 0);
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
