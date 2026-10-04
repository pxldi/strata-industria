package dev.strataindustria.ledger;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** The builder's ledger (uniqueness 2.5): the book, the crate it builds from, and their sounds. */
public final class LedgerRegistry {
    public static final DeferredItem<Item> BUILDERS_LEDGER = ModItems.ITEMS.registerItem("builders_ledger",
            Item::new, p -> p.stacksTo(1));

    public static final DeferredBlock<BuilderCrateBlock> BUILDERS_CRATE = ModBlocks.BLOCKS.registerBlock("builders_crate",
            BuilderCrateBlock::new, p -> p.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.5f)
                    .sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> BUILDERS_CRATE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(BUILDERS_CRATE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuilderCrateBlockEntity>> CRATE_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("builders_crate",
                    () -> new BlockEntityType<>(BuilderCrateBlockEntity::new, BUILDERS_CRATE.get()));

    /** A multiblock written into the ledger. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTRY = sound("ledger.entry");
    /** The ledger striking a block into place. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STAMP = sound("ledger.stamp");
    /** The ledger shut again because the crates ran dry. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SHORT = sound("ledger.short");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private LedgerRegistry() {}
}
