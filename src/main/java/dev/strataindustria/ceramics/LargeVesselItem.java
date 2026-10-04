package dev.strataindustria.ceramics;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** The large vessel as an item, carrying its contents; it cannot go inside another container item. */
public class LargeVesselItem extends BlockItem {
    public LargeVesselItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
