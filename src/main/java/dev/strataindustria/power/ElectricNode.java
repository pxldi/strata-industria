package dev.strataindustria.power;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A block entity that is part of an electric network (tier 5 spec 6.1): cables, generators, storage
 * and machines. Networks are flood-filled over faces that both neighbours connect.
 */
public interface ElectricNode {
    /** Whether power passes through {@code side} of this block. */
    boolean connectsElectric(Direction side);

    /**
     * Which port of the block {@code side} belongs to. A block with several ports (the transformer) joins a
     * different network through each; {@link #port} gives the node that stands for a port in its network.
     */
    default int ports() {
        return 1;
    }

    default int portAt(Direction side) {
        return 0;
    }

    default ElectricNode port(int index) {
        return this;
    }

    /**
     * Blocks this node is joined to by an overhead span (tier 5 spec 8.4), in addition to the faces it touches.
     * A span only counts when the other end lists this block back.
     */
    default List<BlockPos> spans() {
        return List.of();
    }

    /** Whether the block this node belongs to has been removed from the world. */
    default boolean removed() {
        return this instanceof BlockEntity be && be.isRemoved();
    }
}
