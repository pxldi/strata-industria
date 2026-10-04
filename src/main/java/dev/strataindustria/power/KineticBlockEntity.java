package dev.strataindustria.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A plain kinetic block: axles and gearboxes, and the base of the sources. Which faces connect comes
 * from the block ({@link KineticBlock}).
 */
public class KineticBlockEntity extends BlockEntity implements Kinetic {
    private final KineticState kinetic = new KineticState();

    public KineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean connects(net.minecraft.core.Direction side) {
        return getBlockState().getBlock() instanceof KineticBlock block && block.connects(getBlockState(), side);
    }

    @Override
    public KineticState kinetic() {
        return kinetic;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        KineticNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        KineticNetworks.markDirty(level, worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        kinetic.load(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        kinetic.save(out);
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
