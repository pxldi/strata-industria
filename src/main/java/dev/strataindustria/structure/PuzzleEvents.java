package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Tells locked crates when something is built near them, for the puzzles that wait on a block being set. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class PuzzleEvents {
    private PuzzleEvents() {}

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) PuzzleLock.blockPlaced(level, event.getPos());
    }
}
