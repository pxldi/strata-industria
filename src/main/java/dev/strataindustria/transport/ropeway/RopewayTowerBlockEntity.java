package dev.strataindustria.transport.ropeway;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What a tower knows about its line: which terminal owns it, which node it is, and where its span ends. Clients draw
 * the span from it, so the rope is there the moment the chunk is, before the first picture of the buckets arrives.
 */
public class RopewayTowerBlockEntity extends BlockEntity {
    private @Nullable BlockPos terminal;
    private int index;
    private @Nullable BlockPos next;

    public RopewayTowerBlockEntity(BlockPos pos, BlockState state) {
        super(RopewayRegistry.TOWER_ENTITY.get(), pos, state);
    }

    public boolean attached() {
        return terminal != null;
    }

    public @Nullable BlockPos terminal() {
        return terminal;
    }

    /** Which node of the line this is: 1 for the first tower. */
    public int index() {
        return index;
    }

    /** The node the span leaving this tower ends at. */
    public @Nullable BlockPos next() {
        return next;
    }

    public void attach(BlockPos terminal, int index, BlockPos next) {
        this.terminal = terminal;
        this.index = index;
        this.next = next;
        changed();
    }

    /** The line is gone, if it was {@code from}'s. */
    public void detach(BlockPos from) {
        if (!from.equals(terminal)) return;
        terminal = null;
        next = null;
        changed();
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel server) server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (terminal != null && level instanceof ServerLevel server && server.hasChunkAt(terminal)
                && server.getBlockEntity(terminal) instanceof RopewayTerminalBlockEntity owner) {
            owner.cutLine(server, pos, true);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        terminal = in.read("terminal", BlockPos.CODEC).orElse(null);
        next = in.read("next", BlockPos.CODEC).orElse(null);
        index = in.getIntOr("index", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (terminal != null) out.store("terminal", BlockPos.CODEC, terminal);
        if (next != null) out.store("next", BlockPos.CODEC, next);
        out.putInt("index", index);
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
