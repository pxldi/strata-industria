package dev.strataindustria.ironworks;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatInletBlockEntity;
import dev.strataindustria.heat.HeatIntake;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.InletHost;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Bessemer converter (tier 4 spec 12.2). Up to 8 pig iron and 2 pieces of iron or steel scrap per 8
 * pig iron go in; a coke preheats each blow, or 30 HU/t at 1250 °C or more through a heat inlet. With
 * air on a tuyere a blow takes 30 seconds: the flame at
 * the throat climbs to white and then drops, and the charge comes out as one steel ingot per pig iron
 * and per scrap item, with a slag for every 4 pig iron.
 */
public class ConverterBlockEntity extends BaseContainerBlockEntity implements FurnaceHost, InletHost {
    public static final int PIG_IRON = 0, SCRAP = 1, COKE = 2, STEEL = 3, SLAG = 4, SLOTS = 5;
    public static final int MAX_PIG_IRON = 8, MAX_SCRAP = 2, MAX_COKE = 8;
    public static final int BLOW_TICKS = 600;
    /** Preheat over a heat inlet instead of a coke (spec 8.3 and 12.2). */
    public static final int PREHEAT_TEMPERATURE = 1250, PREHEAT_HEAT = 30;
    public static final float PREHEATED = 0.75f;
    /** A charge short of 8 pig iron waits this long for more before it is blown as it is. */
    public static final int SETTLE_TICKS = 40;
    public static final int DATA_STATUS = 0, DATA_PROGRESS = 1, DATA_FLAME = 2, DATA_PROBLEM = 3, DATA_WHERE = 4, DATA_AIR = 5,
            DATA_CHARGE_PIG = 6, DATA_CHARGE_SCRAP = 7, DATA_INLETS = 8, DATA_HOT_TEMPERATURE = 9, DATA_HOT_HEAT = 10,
            DATA_HOT_LIMIT = 11, DATA_COUNT = 12;

    public enum Status {
        INCOMPLETE, EMPTY, CHARGING, NO_AIR, NEEDS_PREHEAT, OUTPUT_FULL, BLOWING;

        public String key() {
            return StrataIndustria.MOD_ID + ".converter.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int[] CHARGE_SLOTS = {PIG_IRON, SCRAP, COKE};
    private static final int[] TAP_SLOTS = {STEEL, SLAG};
    private static @Nullable Set<Item> scrap;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** The charge being blown: pig iron and scrap counts and their summed material quality. */
    private int blowPig;
    private int blowScrap;
    private int blown;
    /** Slag owed in quarters: a slag for every 4 pig iron, carried between blows. */
    private int slagQuarters;
    private int settle;
    private int lastPig;
    private int air;
    private boolean built;
    private boolean checked;
    private Status status = Status.INCOMPLETE;
    private BlastFurnaceStructure.Result structure = BlastFurnaceStructure.INCOMPLETE;
    private final HeatIntake preheat = new HeatIntake(PREHEAT_TEMPERATURE, PREHEAT_HEAT);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_PROGRESS -> blown * 1000 / BLOW_TICKS;
                case DATA_FLAME -> Math.round(flame() * 1000);
                case DATA_PROBLEM -> structure.problem().ordinal();
                case DATA_WHERE -> structure.complete() ? 0 : BlastFurnaceStructure.where(worldPosition, facing(), structure.at(), ConverterStructure.HEIGHT);
                case DATA_AIR -> air;
                case DATA_CHARGE_PIG -> blowPig;
                case DATA_CHARGE_SCRAP -> blowScrap;
                case DATA_INLETS -> structure.inlets().size();
                case DATA_HOT_TEMPERATURE -> Math.round(preheat.temperature());
                case DATA_HOT_HEAT -> preheat.heat();
                case DATA_HOT_LIMIT -> preheat.limit();
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

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.CONVERTER.get(), pos, state);
    }

    // ------------------------------------------------------------------ the charge

    /** Wrought iron or steel ingots, plates, rods and tool heads (spec 12.2). */
    public static boolean isScrap(ItemStack stack) {
        if (scrap == null) {
            Set<Item> set = new HashSet<>();
            for (Metal metal : new Metal[] {Metal.WROUGHT_IRON, Metal.STEEL}) {
                if (ModItems.INGOTS.containsKey(metal)) set.add(ModItems.ingot(metal));
                if (ModItems.PLATES.containsKey(metal)) set.add(ModItems.PLATES.get(metal).get());
                if (ModItems.RODS.containsKey(metal)) set.add(ModItems.RODS.get(metal).get());
                if (ModItems.HEADS.containsKey(metal)) {
                    for (var type : metal.toolTypes()) set.add(ModItems.head(metal, type));
                }
            }
            set.add(ModItems.WROUGHT_IRON_ROD.get());
            set.add(ModItems.WROUGHT_IRON_DOUBLE_INGOT.get());
            set.add(ModItems.STEEL_DOUBLE_INGOT.get());
            scrap = set;
        }
        return scrap.contains(stack.getItem());
    }

    public static boolean isPigIron(ItemStack stack) {
        return stack.is(ModItems.ingot(Metal.PIG_IRON));
    }

    public static boolean isCoke(ItemStack stack) {
        return stack.is(Tier4Items.COKE.get());
    }

    /** Which slot an item belongs in, and how many each slot holds. */
    public static boolean fitsSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case PIG_IRON -> isPigIron(stack);
            case SCRAP -> isScrap(stack);
            case COKE -> isCoke(stack);
            default -> false;
        };
    }

    public static int slotLimit(int slot) {
        return switch (slot) {
            case PIG_IRON -> MAX_PIG_IRON;
            case SCRAP -> MAX_SCRAP;
            case COKE -> MAX_COKE;
            default -> 64;
        };
    }

    // ------------------------------------------------------------------ running

    private Direction facing() {
        return getBlockState().getValue(ConverterBlock.FACING);
    }

    public BlastFurnaceStructure.Result checkStructure() {
        if (level == null) return structure;
        structure = ConverterStructure.check(level, worldPosition, facing());
        if (structure.complete()) {
            claim(structure.tap());
            claim(structure.hatch());
            for (BlockPos inlet : structure.inlets()) claim(inlet);
        }
        return structure;
    }

    private void claim(BlockPos pos) {
        if (level == null) return;
        if (level.getBlockEntity(pos) instanceof FurnaceHatchBlockEntity hatch) hatch.claim(worldPosition);
        if (level.getBlockEntity(pos) instanceof HeatInletBlockEntity inlet) inlet.claim(worldPosition);
    }

    /**
     * How bright the throat flame is, 0 to 1: it climbs to white over most of the blow, holds, and
     * drops at the end, which is the sign the steel is done.
     */
    public float flame() {
        if (blowPig <= 0) return 0;
        float p = blown / (float) BLOW_TICKS;
        if (p < 0.7f) return 0.2f + 0.8f * p / 0.7f;
        if (p < 0.92f) return 1.0f;
        return Math.max(0, (1 - p) / 0.08f);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConverterBlockEntity converter) {
        if (level.getGameTime() % 20 == 0 || !converter.checked) {
            converter.checked = true;
            converter.checkStructure();
        }
        boolean complete = converter.structure.complete();
        if (complete && !converter.built) level.playSound(null, pos, Tier4Sounds.MULTIBLOCK_FORM.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        if (complete != converter.built) {
            converter.built = complete;
            converter.setChanged();
        }
        converter.preheat.roll();
        Status before = converter.status;
        converter.status = converter.work(level, pos);
        if (converter.status != before || converter.status == Status.BLOWING) converter.setChanged();

        boolean lit = converter.status == Status.BLOWING;
        if (state.getValue(ConverterBlock.LIT) != lit) level.setBlock(pos, state.setValue(ConverterBlock.LIT, lit), Block.UPDATE_ALL);
        if (complete) {
            BlockState tap = level.getBlockState(converter.structure.tap());
            if (tap.hasProperty(TapHatchBlock.HOT) && tap.getValue(TapHatchBlock.HOT) != lit) {
                level.setBlock(converter.structure.tap(), tap.setValue(TapHatchBlock.HOT, lit), Block.UPDATE_ALL);
            }
        }
        if (lit && level instanceof ServerLevel server) converter.throat(server);
    }

    private Status work(Level level, BlockPos pos) {
        if (!structure.complete()) {
            air = 0;
            return Status.INCOMPLETE;
        }
        air = AirBlast.into(level, structure.tuyeres());
        if (blowPig > 0) {
            // A blow in progress stalls without air and carries on when it comes back.
            if (air <= 0) return Status.NO_AIR;
            if (++blown >= BLOW_TICKS) finish((ServerLevel) level);
            return blowPig > 0 ? Status.BLOWING : Status.EMPTY;
        }
        ItemStack pig = items.get(PIG_IRON);
        if (pig.getCount() != lastPig) {
            lastPig = pig.getCount();
            settle = 0;
        } else if (settle < SETTLE_TICKS) {
            settle++;
        }
        if (pig.isEmpty()) return Status.EMPTY;
        if (pig.getCount() < MAX_PIG_IRON && settle < SETTLE_TICKS) return Status.CHARGING;
        if (air <= 0) return Status.NO_AIR;
        // Most of a full supply last tick, after what the pipes lose, does for a coke.
        boolean heated = preheat.share() >= PREHEATED;
        if (!heated && items.get(COKE).isEmpty()) return Status.NEEDS_PREHEAT;
        int pigCount = pig.getCount();
        int scrapCount = Math.min(items.get(SCRAP).getCount(), pigCount * MAX_SCRAP / MAX_PIG_IRON);
        if (!fits(STEEL, steel(), pigCount + scrapCount) || !fits(SLAG, new ItemStack(Tier4Items.SLAG.get()), (slagQuarters + pigCount) / 4)) {
            return Status.OUTPUT_FULL;
        }
        start(pigCount, scrapCount, heated);
        return Status.BLOWING;
    }

    /** Starts a blow, preheated by the heat inlet when it is hot enough or else by a coke. */
    private void start(int pigCount, int scrapCount, boolean heated) {
        ItemStack pig = items.get(PIG_IRON), scrapStack = items.get(SCRAP);
        blowPig = pigCount;
        blowScrap = scrapCount;
        blown = 0;
        pig.shrink(pigCount);
        scrapStack.shrink(scrapCount);
        if (!heated) items.get(COKE).shrink(1);
        lastPig = 0;
        settle = 0;
    }

    private void finish(ServerLevel level) {
        int count = blowPig + blowScrap;
        insert(STEEL, steel(), count);
        slagQuarters += blowPig;
        int slag = slagQuarters / 4;
        slagQuarters %= 4;
        if (slag > 0) insert(SLAG, new ItemStack(Tier4Items.SLAG.get()), slag);
        blowPig = 0;
        blowScrap = 0;
        blown = 0;
        level.playSound(null, worldPosition, Tier4Sounds.CONVERTER_DONE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        BlockPos tap = structure.tap();
        level.sendParticles(ParticleTypes.LAVA, tap.getX() + 0.5, tap.getY() + 0.5, tap.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
        Journal.awardNear(level, worldPosition, Journal.CONVERTER);
    }

    /** The roar of the blow and its flame and sparks at the throat (spec 21.6). */
    private void throat(ServerLevel level) {
        BlockPos top = ConverterStructure.vessel(worldPosition, facing()).above(ConverterStructure.HEIGHT);
        double x = top.getX() + 0.5, y = top.getY() + 0.1, z = top.getZ() + 0.5;
        float flame = flame();
        if (level.getGameTime() % 2 == 0) {
            level.sendParticles(flame > 0.8f ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, x, y, z, 2 + Math.round(flame * 4),
                    0.15, 0.1, 0.15, 0.08 + flame * 0.08);
        }
        if (flame > 0.5f && level.getGameTime() % 3 == 0) level.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.2, 0.0, 0.2, 0.0);
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), 30) == 0) {
            level.playSound(null, worldPosition, Tier4Sounds.CONVERTER_BLOW.get(), SoundSource.BLOCKS, 1.0f, 0.9f + flame * 0.2f);
        }
    }

    private ItemStack steel() {
        return new ItemStack(ModItems.ingot(Metal.STEEL));
    }

    private boolean fits(int slot, ItemStack made, int count) {
        if (count <= 0) return true;
        ItemStack out = items.get(slot);
        if (out.isEmpty()) return count <= made.getMaxStackSize();
        boolean same = ItemStack.isSameItemSameComponents(out, made);
        return same && out.getCount() + count <= out.getMaxStackSize();
    }

    private void insert(int slot, ItemStack made, int count) {
        ItemStack out = items.get(slot);
        if (out.isEmpty()) {
            items.set(slot, made.copyWithCount(count));
        } else if (ItemStack.isSameItemSameComponents(out, made)) {
            out.grow(count);
        } else if (level != null) {
            // Steel of another quality than the stack already there goes out of the tap.
            BlockPos tap = structure.tap();
            Block.popResource(level, tap.relative(facing()), made.copyWithCount(count));
        }
    }

    // ------------------------------------------------------------------ preheat

    @Override
    public boolean usesInlet(BlockPos inlet) {
        return structure.complete() && structure.inlets().contains(inlet);
    }

    /** It draws while a charge waits in the vessel and through the blow. */
    @Override
    public int heatDemand(float temperature) {
        return preheat.demand(temperature, structure.complete() && (blowPig > 0 || !items.get(PIG_IRON).isEmpty()));
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        return preheat.offer(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        preheat.route(limitedBy);
    }

    // ------------------------------------------------------------------ for tests and the screen

    /** The heat that came in through the inlets last tick. */
    public HeatIntake preheat() {
        return preheat;
    }

    public Status status() {
        return status;
    }

    public int air() {
        return air;
    }

    public int blowing() {
        return blowPig;
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
        return slot == STEEL || slot == SLAG;
    }

    /** Charge slots take their own items up to their limits, one at a time as a hopper gives them. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fitsSlot(slot, stack) && items.get(slot).getCount() < slotLimit(slot);
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".converter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        checkStructure();
        return new ConverterMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        blowPig = in.getIntOr("blow_pig", 0);
        blowScrap = in.getIntOr("blow_scrap", 0);
        blown = in.getIntOr("blown", 0);
        slagQuarters = in.getIntOr("slag_quarters", 0);
        built = in.getIntOr("built", 0) != 0;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putInt("blow_pig", blowPig);
        out.putInt("blow_scrap", blowScrap);
        out.putInt("blown", blown);
        out.putInt("slag_quarters", slagQuarters);
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
