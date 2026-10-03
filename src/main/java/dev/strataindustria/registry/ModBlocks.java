package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StrataIndustria.MOD_ID);

    // Placeholder content so every datagen provider has something to emit. Replaced as tiers land.
    public static final DeferredBlock<Block> FIRE_BRICKS = BLOCKS.registerSimpleBlock("fire_bricks", p -> p
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(2.0f, 6.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE));

    private ModBlocks() {}
}
