package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricConductor;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.LoadListener;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.registry.Tier5Sounds;
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

/** An insulator's overhead spans (spec 8.4): up to four, each listed at both of its ends. */
public class PoleInsulatorBlockEntity extends BlockEntity implements ElectricConductor, LoadListener {
    public static final int MAX_SPANS = 4;
    /** How far the wire clamp sits from the middle of the block, along the way the insulator points. */
    private static final double TIP = 0.22;

    private final List<BlockPos> spans = new ArrayList<>();
    /** How hard the line is working, 0 to 3 (uniqueness 7.1): the spans sag more at 2 and glow at 3. */
    private int strain;

    public PoleInsulatorBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.POLE_INSULATOR.get(), pos, state);
    }

    /** Where the wire leaves an insulator. */
    public static Vec3 tip(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(PoleInsulatorBlock.FACING);
        return Vec3.atCenterOf(pos).add(facing.getStepX() * TIP, facing.getStepY() * TIP, facing.getStepZ() * TIP);
    }

    @Override
    public List<BlockPos> spans() {
        return List.copyOf(spans);
    }

    public int spanCount() {
        return spans.size();
    }

    public int strain() {
        return strain;
    }

    @Override
    public void gridLoad(int level) {
        if (level == strain) return;
        strain = level;
        setChanged();
        if (this.level != null) this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public boolean hums() {
        return true;
    }

    /** Lists a span at this end only; {@link OverheadLine#connect} does both ends. */
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

    /** Connection is only through the face on the block the insulator is mounted on. */
    @Override
    public boolean connectsElectric(Direction side) {
        return side == getBlockState().getValue(PoleInsulatorBlock.FACING).getOpposite();
    }

    @Override
    public ElectricTier cableTier() {
        return ElectricTier.MV;
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
    public void onLoad() {
        super.onLoad();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    /** Breaking an insulator frees the other end of each span and drops the conductors that made it (spec 8.4). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || spans.isEmpty()) return;
        if (level instanceof net.minecraft.server.level.ServerLevel server) dev.strataindustria.transport.outpost.RouteIndex.get(server).cut(server, pos);
        int conductors = 0;
        for (BlockPos other : spans) {
            conductors += OverheadLine.conductorsFor(pos, other);
            if (level.getBlockEntity(other) instanceof PoleInsulatorBlockEntity far) far.removeSpan(pos);
        }
        spans.clear();
        for (int left = conductors; left > 0; left -= 64) {
            Block.popResource(level, pos, new ItemStack(Tier5Items.ACSR_CONDUCTOR.get(), Math.min(64, left)));
        }
        level.playSound(null, pos, Tier5Sounds.POLE_INSULATOR_DISCONNECT.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        spans.clear();
        in.read("spans", BlockPos.CODEC.listOf()).ifPresent(spans::addAll);
        strain = in.getIntOr("strain", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("spans", BlockPos.CODEC.listOf(), List.copyOf(spans));
        out.putInt("strain", strain);
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
