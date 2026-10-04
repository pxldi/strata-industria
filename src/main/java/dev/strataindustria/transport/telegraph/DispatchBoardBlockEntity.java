package dev.strataindustria.transport.telegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.journal.Journal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** The chalked rows of a dispatch board: what the slate shows from across the room, kept in step with the telegraph index. */
public class DispatchBoardBlockEntity extends BlockEntity {
    /** One chalked row: the charter and how it stands ({@link TelegraphIndex.Report#state}), dimmed when it has gone quiet. */
    public record Row(String name, int state, boolean silent) {
        static final Codec<Row> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Row::name),
                Codec.INT.fieldOf("state").forGetter(Row::state),
                Codec.BOOL.fieldOf("silent").forGetter(Row::silent)
        ).apply(i, Row::new));
    }

    private static final int LOOK_EVERY = 20;

    private List<Row> rows = List.of();

    public DispatchBoardBlockEntity(BlockPos pos, BlockState state) {
        super(TelegraphRegistry.BOARD_ENTITY.get(), pos, state);
    }

    public List<Row> rows() {
        return rows;
    }

    public void serverTick(ServerLevel level) {
        if (Math.floorMod(level.getGameTime() + worldPosition.hashCode(), LOOK_EVERY) != 0) return;
        TelegraphIndex index = TelegraphIndex.get(level);
        if (index.device(worldPosition) == null) index.register(worldPosition, TelegraphIndex.Kind.BOARD);
        if (index.dropOf(worldPosition) == null) {
            BlockPos pole = index.attach(level, worldPosition, TelegraphIndex.Kind.BOARD);
            if (pole != null) TelegraphLine.dropConnected(level, worldPosition, pole);
        }
        refresh(level);
    }

    /** Reads the line now; a changed board is chalked up with a scratch, a puff of dust and (for the goal) a count. */
    public void refresh(ServerLevel level) {
        DispatchView view = DispatchView.of(level, worldPosition);
        List<Row> fresh = new ArrayList<>();
        for (DispatchView.Line line : view.lines()) fresh.add(new Row(line.name(), line.state(), line.silent()));
        if (fresh.equals(rows)) return;
        if (fresh.size() >= Journal.DISPATCH_LINES) Journal.awardNear(level, worldPosition, Journal.DISPATCH_BOARD_LINES);
        boolean grew = fresh.size() > rows.size();
        rows = List.copyOf(fresh);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        level.playSound(null, worldPosition, TelegraphRegistry.CHALK.get(), SoundSource.BLOCKS, grew ? 0.7f : 0.45f, grew ? 1.0f : 1.15f);
        Direction face = getBlockState().getValue(DispatchBoardBlock.FACING);
        Vec3 at = Vec3.atCenterOf(worldPosition).add(face.getStepX() * 0.4, 0.1, face.getStepZ() * 0.4);
        level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, grew ? 8 : 4, 0.25, 0.2, 0.25, 0.0);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        rows = List.copyOf(in.read("rows", Row.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("rows", Row.CODEC.listOf(), rows);
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
