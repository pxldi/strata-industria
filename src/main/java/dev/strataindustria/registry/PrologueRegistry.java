package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.BrickKilnBlock;
import dev.strataindustria.ceramics.BrickKilnBlockEntity;
import dev.strataindustria.ceramics.BrickKilnMenu;
import dev.strataindustria.metal.CastingTableBlock;
import dev.strataindustria.metal.CastingTableBlockEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** The brick kiln and the casting table: the hand age's first machines, kept apart from the tier registries. */
public final class PrologueRegistry {
    /** Tier 2: a kiln of plain bricks that sits on a lit forge. Light 8 while it fires. */
    public static final DeferredBlock<BrickKilnBlock> BRICK_KILN = ModBlocks.BLOCKS.registerBlock("brick_kiln", BrickKilnBlock::new,
            p -> p.mapColor(MapColor.COLOR_RED).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(BrickKilnBlock.LIT) ? 8 : 0));
    /** Tier 2: a low stone table that holds a row of molds; a crucible beside it pours into all of them. */
    public static final DeferredBlock<CastingTableBlock> CASTING_TABLE = ModBlocks.BLOCKS.registerBlock("casting_table", CastingTableBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion());

    public static final DeferredItem<BlockItem> BRICK_KILN_ITEM = ModItems.ITEMS.registerSimpleBlockItem(BRICK_KILN);
    public static final DeferredItem<BlockItem> CASTING_TABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(CASTING_TABLE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrickKilnBlockEntity>> BRICK_KILN_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("brick_kiln", () -> new BlockEntityType<>(BrickKilnBlockEntity::new, BRICK_KILN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CastingTableBlockEntity>> CASTING_TABLE_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("casting_table", () -> new BlockEntityType<>(CastingTableBlockEntity::new, CASTING_TABLE.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<BrickKilnMenu>> BRICK_KILN_MENU =
            ModMenus.MENUS.register("brick_kiln", () -> IMenuTypeExtension.create(BrickKilnMenu::new));

    /** The low roar of bricks holding a fire. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BRICK_KILN_WORK = sound("brick_kiln.work");
    /** A batch comes out of the brick kiln. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BRICK_KILN_DONE = sound("brick_kiln.done");
    /** A mold set down on the casting table. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLE_SET = sound("casting_table.set");
    /** Castings knocked out of their molds on the table. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLE_KNOCK = sound("casting_table.knock");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private PrologueRegistry() {}
}
