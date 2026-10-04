package dev.strataindustria.multiblock;

import dev.strataindustria.StrataIndustria;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The multiblocks the mod ships, by key, and how to find one in a level's registries. */
public final class Multiblocks {
    public static final ResourceKey<Multiblock> BLOOMERY = key("bloomery");
    public static final ResourceKey<Multiblock> COKE_OVEN = key("coke_oven");
    public static final ResourceKey<Multiblock> BLAST_FURNACE = key("blast_furnace");
    public static final ResourceKey<Multiblock> CONVERTER = key("converter");
    public static final ResourceKey<Multiblock> STEEL_BOILER = key("steel_boiler");

    private Multiblocks() {}

    private static ResourceKey<Multiblock> key(String name) {
        return ResourceKey.create(Multiblock.REGISTRY, StrataIndustria.id(name));
    }

    /** The pattern behind a key. A missing file is a broken datapack, so this throws. */
    public static Multiblock get(Level level, ResourceKey<Multiblock> key) {
        return level.registryAccess().lookupOrThrow(Multiblock.REGISTRY).getValueOrThrow(key);
    }

    public static Multiblock.Match check(Level level, ResourceKey<Multiblock> key, BlockPos controller, Direction facing) {
        return get(level, key).check(level, controller, facing);
    }

    /** The pattern whose controller is this block, if the level's registries hold one. */
    public static Optional<Multiblock> forController(Level level, Block block) {
        return level.registryAccess().lookup(Multiblock.REGISTRY).stream()
                .flatMap(registry -> registry.listElements())
                .map(Holder.Reference::value)
                .filter(multiblock -> multiblock.controller() == block)
                .findFirst();
    }

    /** Every pattern by its controller block. */
    public static Map<Block, Multiblock> controllers(Level level) {
        Map<Block, Multiblock> result = new HashMap<>();
        level.registryAccess().lookup(Multiblock.REGISTRY).stream()
                .flatMap(registry -> registry.listElements())
                .forEach(holder -> result.put(holder.value().controller(), holder.value()));
        return result;
    }

    public static Direction facing(BlockState controller) {
        return controller.getValue(HorizontalDirectionalBlock.FACING);
    }
}
