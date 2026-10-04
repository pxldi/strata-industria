package dev.strataindustria.fluid;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.logistics.Tier5Logistics;
import dev.strataindustria.registry.Tier4Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A port on both ends of its pipe axis. What arrives on one end and matches goes out of the other end
 * into the pipe network there, at a bronze pipe's temperature rating and throughput.
 */
public class FluidFilterBlockEntity extends BlockEntity implements FluidPort {
    private Fluid fluid = Fluids.EMPTY;
    /** Set while this filter is pushing on, so two filters facing each other cannot pass fluid round in circles. */
    private boolean busy;

    public FluidFilterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5Logistics.FLUID_FILTER_BE.get(), pos, state);
    }

    public boolean isSet() {
        return !fluid.isSame(Fluids.EMPTY);
    }

    public Fluid fluid() {
        return fluid;
    }

    public void setFluid(@Nullable Fluid next) {
        fluid = next == null ? Fluids.EMPTY : next;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public Component report() {
        if (!isSet()) return Component.translatable(StrataIndustria.MOD_ID + ".fluid_filter.unset");
        return Component.translatable(StrataIndustria.MOD_ID + ".fluid_filter.set", fluid.getFluidType().getDescription());
    }

    private Direction facing() {
        return getBlockState().getValue(FluidFilterBlock.FACING);
    }

    @Override
    public boolean connectsFluid(Direction side) {
        return side.getAxis() == facing().getAxis();
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return fillHot(side, fluid, amount, pressure, 20.0f, simulate);
    }

    @Override
    public int fillHot(Direction side, Fluid fluid, int amount, float pressure, float temperature, boolean simulate) {
        if (level == null || busy || !connectsFluid(side)) return 0;
        if (isSet() && !this.fluid.isSame(fluid)) return 0;
        FluidPipeBlock bronze = Tier4Blocks.BRONZE_FLUID_PIPE.get();
        if (temperature > bronze.maxTemperature()) return 0;
        FluidPipes.Network onward = FluidPipes.find(level, worldPosition, side.getOpposite());
        int cap = Math.min(amount, bronze.throughput());
        if (simulate) return FluidPipes.room(level, onward, fluid, cap, temperature, pressure);
        busy = true;
        try {
            int moved = FluidPipes.push(level, onward, fluid, cap, temperature, pressure).moved();
            if (moved > 0 && !isSet()) setFluid(fluid);
            return moved;
        } finally {
            busy = false;
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        fluid = in.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (isSet()) out.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
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
