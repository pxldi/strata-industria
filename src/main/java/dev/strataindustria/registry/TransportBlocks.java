package dev.strataindustria.registry;

import dev.strataindustria.transport.outpost.CharterItem;
import dev.strataindustria.transport.outpost.OutpostCharterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Outposts and transport blocks and items (outposts spec 12), registered into the shared registers. */
public final class TransportBlocks {
    public static final DeferredBlock<OutpostCharterBlock> OUTPOST_CHARTER = ModBlocks.BLOCKS.registerBlock("outpost_charter",
            OutpostCharterBlock::new, p -> p.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.5f)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<CharterItem> OUTPOST_CHARTER_ITEM = ModItems.ITEMS.registerItem("outpost_charter",
            p -> new CharterItem(OUTPOST_CHARTER.get(), p), p -> p.useBlockDescriptionPrefix());

    public static void init() {
        TransportDataComponents.init();
        TransportSounds.init();
    }

    private TransportBlocks() {}
}
