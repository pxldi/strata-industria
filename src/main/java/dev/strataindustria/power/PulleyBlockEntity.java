package dev.strataindustria.power;

import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A pulley and, when belted, the pulley at the other end of its belt. */
public class PulleyBlockEntity extends KineticBlockEntity {
    /** Furthest a belt reaches (spec 7.3). */
    public static final double MAX_BELT = 8.0;

    private @Nullable BlockPos link;

    public PulleyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULLEY.get(), pos, state);
    }

    public @Nullable BlockPos link() {
        return link;
    }

    public void setLink(@Nullable BlockPos link) {
        this.link = link == null ? null : link.immutable();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            KineticNetworks.markDirty(level, worldPosition);
        }
    }

    @Override
    public List<BlockPos> links() {
        return link == null ? List.of() : List.of(link);
    }

    /** Why two pulleys cannot be belted, as a translation key suffix, or null if they can. */
    public static @Nullable String problem(BlockState a, BlockPos posA, BlockState b, BlockPos posB) {
        if (posA.equals(posB)) return "same";
        var axis = a.getValue(PulleyBlock.AXIS);
        if (b.getValue(PulleyBlock.AXIS) != axis) return "not_parallel";
        if (posA.get(axis) != posB.get(axis)) return "not_in_line";
        if (Math.sqrt(posA.distSqr(posB)) > MAX_BELT) return "too_far";
        return null;
    }

    /** Breaking either end drops the belt and frees the other pulley. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || link == null) return;
        if (level.getBlockEntity(link) instanceof PulleyBlockEntity other && worldPosition.equals(other.link())) other.setLink(null);
        Block.popResource(level, worldPosition, ModItems.LEATHER_BELT.get().getDefaultInstance());
        link = null;
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        link = in.read("link", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (link != null) out.store("link", BlockPos.CODEC, link);
    }
}
