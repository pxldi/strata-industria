package dev.strataindustria.fluid;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Fluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A fluid tank (tier 4 spec 9.3): 16 000 mB. Tanks stacked in a column, up to eight, act as one: they hold
 * one fluid, fill from the bottom and drain from the top. Any face takes a pipe. A tank with an open valve
 * right below it drains into the pipes past the valve.
 */
public class FluidTankBlockEntity extends BlockEntity implements FluidPort {
    public static final int CAPACITY = 16_000, MAX_STACK = 8, BUCKET = 1000;
    /** Ticks between looks for the pipes past the valve, and between client updates while the level moves. */
    private static final int NETWORK_REFRESH = 20, SYNC_INTERVAL = 4;

    private Fluid fluid = Fluids.EMPTY;
    private int amount;
    /** Steam keeps the pressure it came in at, so a tank can buffer a boiler. */
    private float pressure;
    private boolean dirty;
    private int age;
    private FluidPipes.Network network = FluidPipes.Network.NONE;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.FLUID_TANK.get(), pos, state);
    }

    public Fluid fluid() {
        return amount > 0 ? fluid : Fluids.EMPTY;
    }

    public int amount() {
        return amount;
    }

    // ------------------------------------------------------------------ the column

    /** The tanks acting with this one, bottom first: its column, cut into groups of eight from the bottom. */
    public List<FluidTankBlockEntity> group() {
        List<FluidTankBlockEntity> tanks = new ArrayList<>();
        if (level == null) {
            tanks.add(this);
            return tanks;
        }
        BlockPos bottom = worldPosition;
        for (int i = 0; i < 256 && level.getBlockEntity(bottom.below()) instanceof FluidTankBlockEntity; i++) bottom = bottom.below();
        int first = (worldPosition.getY() - bottom.getY()) / MAX_STACK * MAX_STACK;
        for (int i = 0; i < MAX_STACK; i++) {
            if (!(level.getBlockEntity(bottom.above(first + i)) instanceof FluidTankBlockEntity tank)) break;
            tanks.add(tank);
        }
        return tanks;
    }

    /** The fluid the group holds, or EMPTY. */
    public static Fluid fluidOf(List<FluidTankBlockEntity> group) {
        for (FluidTankBlockEntity tank : group) if (tank.amount > 0) return tank.fluid;
        return Fluids.EMPTY;
    }

    /** How much of the group's fluid it holds. */
    public static int totalOf(List<FluidTankBlockEntity> group) {
        Fluid held = fluidOf(group);
        int total = 0;
        for (FluidTankBlockEntity tank : group) if (tank.amount > 0 && tank.fluid.isSame(held)) total += tank.amount;
        return total;
    }

    /** Fills the group from the bottom up. Returns how much it took, or would take. */
    public int fill(Fluid fluid, int amount, float pressure, boolean simulate) {
        if (fluid.isSame(Fluids.EMPTY) || amount <= 0) return 0;
        List<FluidTankBlockEntity> group = group();
        Fluid held = fluidOf(group);
        if (!held.isSame(Fluids.EMPTY) && !held.isSame(fluid)) return 0;
        int left = amount;
        for (FluidTankBlockEntity tank : group) {
            // A tank joined on from another column may still hold something else; it is skipped until emptied.
            if (tank.amount > 0 && !tank.fluid.isSame(fluid)) continue;
            int take = Math.min(left, CAPACITY - tank.amount);
            if (take <= 0) continue;
            left -= take;
            if (!simulate) {
                tank.fluid = fluid;
                tank.amount += take;
                tank.pressure = Math.max(tank.pressure, pressure);
                tank.changed();
            }
            if (left == 0) break;
        }
        return amount - left;
    }

    /** Drains the group from the top down. Returns how much it gave, or would give. */
    public int drain(int amount, boolean simulate) {
        List<FluidTankBlockEntity> group = group();
        Fluid held = fluidOf(group);
        int left = amount;
        for (int i = group.size() - 1; i >= 0 && left > 0; i--) {
            FluidTankBlockEntity tank = group.get(i);
            if (tank.amount > 0 && !tank.fluid.isSame(held)) continue;
            int give = Math.min(left, tank.amount);
            if (give <= 0) continue;
            left -= give;
            if (!simulate) {
                tank.amount -= give;
                if (tank.amount == 0) {
                    tank.fluid = Fluids.EMPTY;
                    tank.pressure = 0;
                }
                tank.changed();
            }
        }
        return amount - left;
    }

    private void changed() {
        dirty = true;
        setChanged();
    }

    // ------------------------------------------------------------------ pipes

    @Override
    public boolean connectsFluid(Direction side) {
        return true;
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return fill(fluid, amount, fluid.isSame(Tier4Fluids.STEAM.get()) ? pressure : 0, simulate);
    }

    /** How hot a fluid leaves a tank: steam at its saturation temperature (spec 9.1), anything else cold. */
    public static float temperatureOf(Fluid fluid, float pressure) {
        return fluid.isSame(Tier4Fluids.STEAM.get()) ? 100 + 15 * pressure : 20;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity tank) {
        tank.tick(level, pos);
    }

    private void tick(Level level, BlockPos pos) {
        age++;
        if (dirty && age % SYNC_INTERVAL == 0) {
            dirty = false;
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
        BlockState below = level.getBlockState(pos.below());
        if (!(below.getBlock() instanceof ValveBlock) || !ValveBlock.passes(below)) {
            network = FluidPipes.Network.NONE;
            return;
        }
        List<FluidTankBlockEntity> group = group();
        if (age % NETWORK_REFRESH == 1 || network == FluidPipes.Network.NONE) network = outlet(level, pos, group);
        Fluid held = fluidOf(group);
        int total = totalOf(group);
        if (total <= 0 || network.isEmpty()) return;
        float out = 0;
        for (FluidTankBlockEntity t : group) out = Math.max(out, t.pressure);
        FluidPipes.Push push = FluidPipes.push(level, network, held, total, temperatureOf(held, out), out);
        if (push.moved() > 0) drain(push.moved(), false);
    }

    /** The pipes past the valve, leaving out this column so it never pours into itself. */
    private static FluidPipes.Network outlet(Level level, BlockPos pos, List<FluidTankBlockEntity> group) {
        FluidPipes.Network found = FluidPipes.find(level, pos, Direction.DOWN);
        List<FluidPipes.Endpoint> ports = new ArrayList<>();
        for (FluidPipes.Endpoint port : found.ports()) {
            boolean own = false;
            for (FluidTankBlockEntity tank : group) own |= tank.getBlockPos().equals(port.pos());
            if (!own) ports.add(port);
        }
        return new FluidPipes.Network(found.pipes(), ports, found.gauges(), found.maxTemperature(), found.throughput(), found.firstPipe());
    }

    /** Comparator output: how full the group is, 0 to 15. */
    public int signal() {
        List<FluidTankBlockEntity> group = group();
        int total = totalOf(group);
        if (total <= 0) return 0;
        return 1 + Math.round(14.0f * total / (group.size() * CAPACITY));
    }

    /** The sneak right-click readout. */
    public Component readout() {
        List<FluidTankBlockEntity> group = group();
        int total = totalOf(group);
        String key = StrataIndustria.MOD_ID + ".fluid_tank.";
        if (total <= 0) return Component.translatable(key + "empty", group.size() * CAPACITY, group.size());
        return Component.translatable(key + "holds", total, group.size() * CAPACITY, fluidOf(group).getFluidType().getDescription(), group.size());
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        amount = in.getIntOr("amount", 0);
        pressure = in.getFloatOr("pressure", 0.0f);
        fluid = in.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        if (fluid.isSame(Fluids.EMPTY)) amount = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (amount > 0) {
            out.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
            out.putInt("amount", amount);
            out.putFloat("pressure", pressure);
        }
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
