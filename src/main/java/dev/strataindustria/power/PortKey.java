package dev.strataindustria.power;

import net.minecraft.core.BlockPos;

/**
 * One connection point of an electric block: the block's position and which of its ports. Nearly every
 * block has a single port 0; a transformer has two, one in each network it joins.
 */
public record PortKey(BlockPos pos, int port) {
    public PortKey(BlockPos pos, int port) {
        this.pos = pos.immutable();
        this.port = port;
    }
}
