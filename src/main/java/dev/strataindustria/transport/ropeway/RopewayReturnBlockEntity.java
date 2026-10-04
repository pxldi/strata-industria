package dev.strataindustria.transport.ropeway;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The return station: which terminal owns the line, and where the buckets tip their loads. */
public class RopewayReturnBlockEntity extends BlockEntity {
    private @Nullable BlockPos terminal;

    public RopewayReturnBlockEntity(BlockPos pos, BlockState state) {
        super(RopewayRegistry.RETURN_ENTITY.get(), pos, state);
    }

    public boolean attached() {
        return terminal != null;
    }

    public @Nullable BlockPos terminal() {
        return terminal;
    }

    public void attach(BlockPos terminal) {
        this.terminal = terminal;
        setChanged();
    }

    public void detach(BlockPos from) {
        if (!from.equals(terminal)) return;
        terminal = null;
        setChanged();
    }

    public Direction facing() {
        return getBlockState().getValue(RopewayReturnBlock.FACING);
    }

    /** Where a load tips out: at the lip of the wheel house, over the back. */
    public Vec3 lip() {
        Direction back = facing().getOpposite();
        return new Vec3(worldPosition.getX() + 0.5 + back.getStepX() * 0.55, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5 + back.getStepZ() * 0.55);
    }

    /** Tips {@code stack} into the container behind the station, else the one beneath; returns what would not go. */
    public ItemStack accept(ServerLevel level, ItemStack stack) {
        ItemStack rest = stack;
        for (Direction side : new Direction[] {facing().getOpposite(), Direction.DOWN}) {
            if (rest.isEmpty()) break;
            Container target = HopperBlockEntity.getContainerAt(level, worldPosition.relative(side));
            if (target == null) continue;
            rest = HopperBlockEntity.addItem(null, target, rest, side.getOpposite());
            target.setChanged();
        }
        return rest;
    }

    public Component status() {
        String id = dev.strataindustria.StrataIndustria.MOD_ID + ".ropeway.return.";
        if (terminal == null) return Component.translatable(id + "bare");
        if (level instanceof ServerLevel server && server.hasChunkAt(terminal) && server.getBlockEntity(terminal) instanceof RopewayTerminalBlockEntity owner && owner.stalled()) {
            return Component.translatable(id + "backed_up");
        }
        return Component.translatable(id + "line");
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
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (terminal != null) out.store("terminal", BlockPos.CODEC, terminal);
    }
}
