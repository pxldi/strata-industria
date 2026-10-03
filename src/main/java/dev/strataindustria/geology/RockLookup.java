package dev.strataindustria.geology;

import dev.strataindustria.registry.ModBlocks;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Fast block to rock lookups for worldgen. Built on first use, after registries are frozen. */
public final class RockLookup {
    private static volatile Map<Block, Rock> rawRocks;
    private static volatile Set<Block> replaced;

    private RockLookup() {}

    /** The rock of a raw rock block, or null. */
    public static Rock rawRock(BlockState state) {
        Map<Block, Rock> map = rawRocks;
        if (map == null) {
            map = new IdentityHashMap<>();
            for (Rock rock : Rock.values()) map.put(ModBlocks.RAW_ROCK.get(rock).get(), rock);
            rawRocks = map;
        }
        return map.get(state.getBlock());
    }

    /**
     * Vanilla blocks the strata pass swaps for rock: the stones of worldgen spec 4.5, plus the ore and
     * raw ore blocks of vanilla's large noise veins, which have no place in the strata.
     */
    public static boolean isReplaced(BlockState state) {
        Set<Block> set = replaced;
        if (set == null) {
            set = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            set.addAll(java.util.List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
                    Blocks.TUFF, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK, Blocks.IRON_ORE,
                    Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK));
            replaced = set;
        }
        return set.contains(state.getBlock());
    }
}
