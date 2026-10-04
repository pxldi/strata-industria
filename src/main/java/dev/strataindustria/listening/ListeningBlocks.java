package dev.strataindustria.listening;

import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Blocks of the listening kit, registered into the shared registers. */
public final class ListeningBlocks {
    public static final DeferredBlock<SteamWhistleBlock> STEAM_WHISTLE = ModBlocks.BLOCKS.registerBlock("steam_whistle",
            SteamWhistleBlock::new, p -> p.mapColor(MapColor.COLOR_BROWN).strength(2.0f, 4.0f).requiresCorrectToolForDrops()
                    .noOcclusion().sound(SoundType.COPPER));
    public static final DeferredItem<BlockItem> STEAM_WHISTLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(STEAM_WHISTLE);

    public static void init() {
        ListeningSounds.init();
    }

    private ListeningBlocks() {}
}
