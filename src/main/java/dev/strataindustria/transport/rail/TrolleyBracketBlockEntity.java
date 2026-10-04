package dev.strataindustria.transport.rail;

import dev.strataindustria.power.ElectricConductor;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A trolley bracket's place in the electric network (outposts spec 9.2). It is an MV conductor like the pole it hangs
 * on, joined to the pole by its back face and to other brackets by trolley wire spans. It is also the one place a
 * tram draws from: the tram asks for joules with {@link #pull}, the network hands them over at the end of the tick,
 * and the tram takes them with {@link #takeDelivered}. As a device it is LV, so a tram on MV power reads Overvoltage.
 */
public class TrolleyBracketBlockEntity extends BlockEntity implements ElectricConductor, ElectricConsumer {
    /** Most J/t one bracket hands a tram: its accelerating draw. */
    public static final double PULL_LIMIT = ElectricTramEntity.ACCEL_DRAW;
    /** How far out along its arm the wire clamp sits, and how high in the block. */
    private static final double TIP_OUT = 0.375;

    private final List<BlockPos> spans = new ArrayList<>();
    private double pending;
    private long pulledAt = -100;
    private double delivered;

    public TrolleyBracketBlockEntity(BlockPos pos, BlockState state) {
        super(TramRegistry.TROLLEY_BRACKET_ENTITY.get(), pos, state);
    }

    /** Where the wire hangs from a bracket. */
    public static Vec3 tip(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(TrolleyBracketBlock.FACING);
        return Vec3.atCenterOf(pos).add(facing.getStepX() * TIP_OUT, 0, facing.getStepZ() * TIP_OUT);
    }

    // ---------------------------------------------------------------- the network

    @Override
    public boolean connectsElectric(Direction side) {
        return side == getBlockState().getValue(TrolleyBracketBlock.FACING).getOpposite();
    }

    @Override
    public List<BlockPos> spans() {
        return List.copyOf(spans);
    }

    @Override
    public ElectricTier cableTier() {
        return ElectricTier.MV;
    }

    @Override
    public ElectricTier tier() {
        return ElectricTier.LV;
    }

    @Override
    public double blockLoss() {
        return 0;
    }

    @Override
    public int capacity() {
        return ElectricNetwork.SPAN_CAPACITY;
    }

    @Override
    public double powerLimit() {
        return PULL_LIMIT;
    }

    /** A tram below asks for {@code joules} this tick. */
    public void pull(double joules) {
        long now = level == null ? 0 : level.getGameTime();
        if (pulledAt != now) pending = 0;
        pulledAt = now;
        pending += joules;
    }

    @Override
    public double request() {
        long now = level == null ? 0 : level.getGameTime();
        return now - pulledAt <= 1 ? pending : 0;
    }

    @Override
    public void receive(double amount) {
        delivered += amount;
    }

    /** What the network gave since the tram last took it. */
    public double takeDelivered() {
        double amount = delivered;
        delivered = 0;
        return amount;
    }

    // ---------------------------------------------------------------- spans

    public int spanCount() {
        return spans.size();
    }

    void addSpan(BlockPos other) {
        if (spans.contains(other)) return;
        spans.add(other.immutable());
        changed();
    }

    void removeSpan(BlockPos other) {
        if (spans.remove(other)) changed();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            ElectricNetworks.markDirty(level, worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ElectricNetworks.markDirty(level, worldPosition);
        if (level != null && !level.isClientSide()) TrolleyWires.track(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ElectricNetworks.markDirty(level, worldPosition);
        if (level != null && !level.isClientSide()) TrolleyWires.untrack(level, worldPosition);
    }

    /** Breaking a bracket lets go of each wire it holds and drops the wire that made it (outposts spec 9.2). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || spans.isEmpty()) return;
        int wire = 0;
        for (BlockPos other : spans) {
            wire += TrolleyWires.wireFor(pos, other);
            if (level.getBlockEntity(other) instanceof TrolleyBracketBlockEntity far) far.removeSpan(pos);
        }
        spans.clear();
        for (int left = wire; left > 0; left -= 64) {
            Block.popResource(level, pos, new ItemStack(TramRegistry.TROLLEY_WIRE.get(), Math.min(64, left)));
        }
        level.playSound(null, pos, TramRegistry.WIRE_CUT.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        spans.clear();
        in.read("spans", BlockPos.CODEC.listOf()).ifPresent(spans::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("spans", BlockPos.CODEC.listOf(), List.copyOf(spans));
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
