package dev.strataindustria.structure;

import dev.strataindustria.geology.GeologyContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

/** Finds the rock provinces and vein cells of the level a structure is being planned or built in. */
final class StructureGeology {
    private StructureGeology() {}

    static @Nullable GeologyContext of(WorldGenLevel level) {
        return GeologyContext.get(level.getLevel());
    }

    /**
     * Structure starts are planned without a level at hand, so this finds the level by its chunk generator.
     * Vein cells depend only on the seed and the generator, so the answer matches the one {@link #of(WorldGenLevel)}
     * gives while building.
     */
    static @Nullable GeologyContext of(Structure.GenerationContext context) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getChunkSource().getGenerator() == context.chunkGenerator()) return GeologyContext.get(level);
        }
        return null;
    }
}
