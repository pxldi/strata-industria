package dev.strataindustria.steam;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatInletBlockEntity;
import dev.strataindustria.heat.HeatNetwork;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.ironworks.AirBlast;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.ArrayList;
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
import org.jspecify.annotations.Nullable;

/**
 * The firebox (tier 4 spec 8.1): burns fuel from four slots and offers its heat to the block above, to
 * heat inlets it touches and over heat pipes on any face. It climbs 5 °C a second toward the fuel's
 * maximum while burning and falls as fast once the fuel runs out. It makes heat while it burns whether or
 * not anything takes it, so an idle firebox wastes fuel. A blower blowing into it makes it 150 °C hotter
 * and burns the fuel half again as fast for half again the heat.
 */
public class FireboxBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, HeatPort {
    public static final int SLOTS = 4;
    public static final int DATA_TEMPERATURE = 0, DATA_BURN = 1, DATA_BURN_TOTAL = 2, DATA_OUTPUT = 3, DATA_TAKEN = 4,
            DATA_STATUS = 5, DATA_BLOWN = 6, DATA_COUNT = 7;
    /** 5 °C a second, per tick. */
    public static final float RATE = 5.0f / 20.0f;
    /** The lit door shows the hot texture from here (spec 21.2). */
    public static final float HOT_FROM = 1300.0f;
    /** What a blower adds to the fuel's maximum (spec 8.1). */
    public static final int BLOWER_TEMPERATURE = 150;

    /** What one item of a fuel gives: how long it burns, how hot the firebox can get on it, and HU per tick. */
    public record Fuel(int burnTicks, int maxTemperature, int heat) {}

    public static final Fuel LOG = new Fuel(800, 800, 10);
    public static final Fuel LIGNITE = new Fuel(1200, 1200, 20);
    public static final Fuel CHARCOAL = new Fuel(1600, 1350, 25);
    public static final Fuel COAL = new Fuel(1600, 1400, 30);
    public static final Fuel COKE = new Fuel(2400, 1600, 30);
    public static final Fuel COKE_BLOCK = new Fuel(21600, 1600, 30);

    public enum Status {
        /** Cold and no fuel. */
        EMPTY,
        /** Burning but nothing above takes the heat. */
        IDLE,
        /** Burning and its heat goes somewhere. */
        HEATING,
        /** Out of fuel and cooling down. */
        COOLING;

        public String key() {
            return StrataIndustria.MOD_ID + ".firebox.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int[] ALL_SLOTS = {0, 1, 2, 3};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private float temperature = Heat.AMBIENT;
    private int burnLeft;
    private int burnTotal = 1;
    private int maxTemperature;
    private int heat;
    private int taken;
    private Status status = Status.EMPTY;
    private boolean blown;
    private int burnClock;
    /** The consumers the pipes reach, found again when a pipe changes anywhere or one appears or goes here. */
    private HeatNetwork.Routes routes = HeatNetwork.Routes.NONE;
    private int routesVersion = -1;
    private int pipeFaces = -1;
    private boolean pipesHot;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_TEMPERATURE -> Math.round(temperature);
                case DATA_BURN -> burnLeft;
                case DATA_BURN_TOTAL -> burnTotal;
                case DATA_OUTPUT -> output();
                case DATA_TAKEN -> taken;
                case DATA_STATUS -> status.ordinal();
                case DATA_BLOWN -> blown ? 1 : 0;
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

    public FireboxBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.FIREBOX.get(), pos, state);
    }

    /** Spec 8.1's fuel table; null for anything that does not burn here. */
    public static @Nullable Fuel fuelFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ItemTags.LOGS_THAT_BURN)) return LOG;
        if (stack.is(ModItems.LIGNITE.get())) return LIGNITE;
        if (stack.is(Items.CHARCOAL)) return CHARCOAL;
        if (stack.is(Items.COAL)) return COAL;
        if (stack.is(Tier4Items.COKE.get())) return COKE;
        if (stack.is(Tier4Items.COKE_BLOCK.get())) return COKE_BLOCK;
        return null;
    }

    public float temperature() {
        return temperature;
    }

    public boolean burning() {
        return burnLeft > 0;
    }

    /** HU per tick it makes right now: the fuel's rate, half again with a blower. */
    public int output() {
        return burnLeft <= 0 ? 0 : blown ? heat * 3 / 2 : heat;
    }

    public boolean blown() {
        return blown;
    }

    /** HU per tick its consumers took last tick. */
    public int taken() {
        return taken;
    }

    public Status status() {
        return status;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity firebox) {
        firebox.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        blown = blower(level, pos);
        boolean wasBurning = burnLeft > 0;
        if (burnLeft <= 0 && takeFuel() && !wasBurning) {
            level.playSound(null, pos, Tier4Sounds.FIREBOX_LIGHT.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        boolean burning = burnLeft > 0;
        if (burning) {
            burnLeft--;
            // A blower burns the fuel half again as fast: every other tick takes one more.
            if (blown && (++burnClock & 1) == 0) burnLeft = Math.max(0, burnLeft - 1);
            float max = maxTemperature + (blown ? BLOWER_TEMPERATURE : 0);
            // A weaker fuel after a stronger one lets the fire settle down to its own maximum.
            temperature = temperature > max ? Math.max(max, temperature - RATE) : Math.min(max, temperature + RATE);
        } else {
            temperature = Math.max(Heat.AMBIENT, temperature - RATE);
        }
        findRoutes(level, pos);
        int before = taken;
        taken = 0;
        if (burning) {
            int output = blown ? heat * 3 / 2 : heat;
            taken = Math.max(0, Math.min(output, HeatNetwork.deliver(level, targets(level, pos), temperature, output)));
        }
        boolean glowing = burning && temperature >= HeatPipeBlock.GLOWS_FROM;
        if (glowing != pipesHot) {
            pipesHot = glowing;
            HeatNetwork.glow(level, routes.pipes(), glowing);
        }
        Status next = burning ? (taken > 0 ? Status.HEATING : Status.IDLE) : temperature > Heat.AMBIENT + 1 ? Status.COOLING : Status.EMPTY;
        boolean changed = next != status || taken != before;
        status = next;

        boolean hot = burning && temperature >= HOT_FROM;
        if (state.getValue(FireboxBlock.LIT) != burning || state.getValue(FireboxBlock.HOT) != hot) {
            level.setBlock(pos, state.setValue(FireboxBlock.LIT, burning).setValue(FireboxBlock.HOT, hot), Block.UPDATE_ALL);
        }
        if (changed || burning || status == Status.COOLING) setChanged();
    }

    /** Whether a running blower blows into any face (spec 8.1). */
    private static boolean blower(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(pos.relative(side)) instanceof AirBlast blast && blast.airOut(side.getOpposite()) > 0) return true;
        }
        return false;
    }

    /** The block above, heat inlets on the other faces, then whatever the pipes reach. */
    private List<HeatNetwork.Route> targets(Level level, BlockPos pos) {
        List<HeatNetwork.Route> targets = new ArrayList<>();
        BlockPos above = pos.above();
        if (level.getBlockEntity(above) instanceof HeatConsumer) targets.add(HeatNetwork.Route.direct(above));
        for (Direction side : Direction.values()) {
            if (side == Direction.UP) continue;
            BlockPos next = pos.relative(side);
            if (level.getBlockEntity(next) instanceof HeatInletBlockEntity) targets.add(HeatNetwork.Route.direct(next));
        }
        for (HeatNetwork.Route route : routes.routes()) {
            boolean touching = false;
            for (HeatNetwork.Route direct : targets) touching |= direct.pos().equals(route.pos());
            if (!touching) targets.add(route);
        }
        return targets;
    }

    /** Looks for the consumers on its pipes again when a pipe joined or left anything since the last look. */
    private void findRoutes(Level level, BlockPos pos) {
        int faces = 0;
        for (Direction side : Direction.values()) {
            if (level.getBlockState(pos.relative(side)).getBlock() instanceof HeatPipeBlock) faces |= 1 << side.ordinal();
        }
        int version = HeatNetwork.version();
        if (faces == pipeFaces && version == routesVersion) return;
        pipeFaces = faces;
        routesVersion = version;
        HeatNetwork.Routes found = faces == 0 ? HeatNetwork.Routes.NONE : HeatNetwork.find(level, pos);
        if (pipesHot) {
            // Pipes cut off from the fire cool; newly joined ones light up.
            List<BlockPos> cut = new ArrayList<>(routes.pipes());
            cut.removeAll(found.pipes());
            HeatNetwork.glow(level, cut, false);
            HeatNetwork.glow(level, found.pipes(), true);
        }
        routes = found;
    }

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    /** Its contents spill as usual, and the pipes it was heating cool. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (pipesHot && level != null) HeatNetwork.glow(level, routes.pipes(), false);
    }

    /** Lights the next fuel item in slot order; false when there is none. */
    private boolean takeFuel() {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.get(i);
            Fuel fuel = fuelFor(stack);
            if (fuel == null) continue;
            stack.shrink(1);
            burnLeft = burnTotal = fuel.burnTicks();
            maxTemperature = fuel.maxTemperature();
            heat = fuel.heat();
            return true;
        }
        return false;
    }

    /** For game tests: start burning at this temperature without waiting for the climb. */
    public void preheat(float temperature) {
        this.temperature = temperature;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fuelFor(stack) != null;
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".firebox");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FireboxMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        temperature = in.getFloatOr("temperature", Heat.AMBIENT);
        burnLeft = in.getIntOr("burn", 0);
        burnTotal = Math.max(1, in.getIntOr("burn_total", 1));
        maxTemperature = in.getIntOr("max_temperature", 0);
        heat = in.getIntOr("heat", 0);
        pipesHot = in.getBooleanOr("pipes_hot", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putFloat("temperature", temperature);
        out.putInt("burn", burnLeft);
        out.putInt("burn_total", burnTotal);
        out.putInt("max_temperature", maxTemperature);
        out.putInt("heat", heat);
        out.putBoolean("pipes_hot", pipesHot);
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
