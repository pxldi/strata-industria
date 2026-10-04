package dev.strataindustria.structure;

import dev.strataindustria.geology.Rock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import dev.strataindustria.StrataIndustria;

/**
 * The blocks every structure draws on (structures v2 section 5): crate, miner's lamp, ore cart, tool rack,
 * rubble, cracked and mossy rock, smouldering log pile, windlass and sluice box. They register into
 * {@link StructureContent}'s registers; {@link #init()} just makes sure this class is loaded first.
 */
public final class SharedBlocks {
    // ---------------------------------------------------------------- sounds

    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_OPEN = sound("block.crate.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_CLOSE = sound("block.crate.close");
    /** A shut crate rattling when someone pulls at the lid. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_LOCKED = sound("block.crate.locked");
    /** The latch giving when a puzzle is solved. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_UNLOCK = sound("block.crate.unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINERS_LAMP_LIGHT = sound("block.miners_lamp.light");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINERS_LAMP_SNUFF = sound("block.miners_lamp.snuff");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINERS_LAMP_FLUTTER = sound("block.miners_lamp.flutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORE_CART_RATTLE = sound("block.ore_cart.rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOOL_RACK_CLINK = sound("block.tool_rack.clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUBBLE_BREAK = sound("block.rubble.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUBBLE_STEP = sound("block.rubble.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRACKED_ROCK_BREAK = sound("block.cracked_rock.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMOULDERING_CRACKLE = sound("block.smouldering_log_pile.crackle");
    /** Water trickling through a sluice box. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUICE_BOX_WATER = sound("block.sluice_box.water");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDLASS_CREAK = sound("block.windlass.creak");

    private static final class Sounds {
        static final SoundType RUBBLE = new DeferredSoundType(1.0f, 1.0f, RUBBLE_BREAK, RUBBLE_STEP,
                () -> SoundEvents.GRAVEL_PLACE, () -> SoundEvents.GRAVEL_HIT, () -> SoundEvents.GRAVEL_FALL);
        static final SoundType CRACKED_ROCK = new DeferredSoundType(1.0f, 1.0f, CRACKED_ROCK_BREAK,
                () -> SoundEvents.STONE_STEP, () -> SoundEvents.STONE_PLACE, () -> SoundEvents.STONE_HIT, () -> SoundEvents.STONE_FALL);
    }

    // ---------------------------------------------------------------- blocks

    public static final DeferredBlock<CrateBlock> CRATE = StructureContent.BLOCKS.registerBlock("crate", CrateBlock::new,
            p -> p.mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(1.5f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<MinersLampBlock> MINERS_LAMP = StructureContent.BLOCKS.registerBlock("miners_lamp", MinersLampBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(0.5f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(MinersLampBlock::light));
    public static final DeferredBlock<OreCartBlock> ORE_CART = StructureContent.BLOCKS.registerBlock("ore_cart", OreCartBlock::new,
            p -> p.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<ToolRackBlock> TOOL_RACK = StructureContent.BLOCKS.registerBlock("tool_rack", ToolRackBlock::new,
            p -> p.mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(1.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<SmoulderingLogPileBlock> SMOULDERING_LOG_PILE = StructureContent.BLOCKS.registerBlock(
            "smouldering_log_pile", SmoulderingLogPileBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASS).strength(1.2f).sound(SoundType.WOOD).lightLevel(state -> 7));
    public static final DeferredBlock<WindlassBlock> WINDLASS = StructureContent.BLOCKS.registerBlock("windlass", WindlassBlock::new,
            p -> p.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<SluiceBoxBlock> SLUICE_BOX = StructureContent.BLOCKS.registerBlock("sluice_box", SluiceBoxBlock::new,
            p -> p.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());

    /** Broken rock, in each rock's colour. */
    public static final Map<Rock, DeferredBlock<RubbleBlock>> RUBBLE = new EnumMap<>(Rock.class);
    /** The raw rock, cracked through (weathering for ruins). */
    public static final Map<Rock, DeferredBlock<Block>> CRACKED = new EnumMap<>(Rock.class);
    /** Cobbled rock with moss over it. */
    public static final Map<Rock, DeferredBlock<Block>> MOSSY_COBBLED = new EnumMap<>(Rock.class);
    public static final Map<Rock, DeferredItem<BlockItem>> RUBBLE_ITEMS = new EnumMap<>(Rock.class);
    public static final Map<Rock, DeferredItem<BlockItem>> CRACKED_ITEMS = new EnumMap<>(Rock.class);
    public static final Map<Rock, DeferredItem<BlockItem>> MOSSY_COBBLED_ITEMS = new EnumMap<>(Rock.class);

    static {
        for (Rock rock : Rock.values()) {
            var rubble = StructureContent.BLOCKS.registerBlock("rubble_" + rock.id(), RubbleBlock::new,
                    p -> p.mapColor(rock.mapColor()).noCollision().noOcclusion().instabreak().sound(Sounds.RUBBLE)
                            .pushReaction(PushReaction.POPPED));
            RUBBLE.put(rock, rubble);
            RUBBLE_ITEMS.put(rock, StructureContent.ITEMS.registerSimpleBlockItem(rubble));
            var cracked = StructureContent.BLOCKS.registerSimpleBlock("cracked_" + rock.id(),
                    p -> p.mapColor(rock.mapColor()).instrument(NoteBlockInstrument.BASEDRUM).strength(rock.hardness(), rock.hardness() * 4)
                            .requiresCorrectToolForDrops().sound(Sounds.CRACKED_ROCK));
            CRACKED.put(rock, cracked);
            CRACKED_ITEMS.put(rock, StructureContent.ITEMS.registerSimpleBlockItem(cracked));
            var mossy = StructureContent.BLOCKS.registerSimpleBlock("mossy_cobbled_" + rock.id(),
                    p -> p.mapColor(rock.mapColor()).instrument(NoteBlockInstrument.BASEDRUM).strength(rock.hardness() + 0.4f, rock.hardness() * 4)
                            .requiresCorrectToolForDrops().sound(SoundType.STONE));
            MOSSY_COBBLED.put(rock, mossy);
            MOSSY_COBBLED_ITEMS.put(rock, StructureContent.ITEMS.registerSimpleBlockItem(mossy));
        }
    }

    public static final DeferredItem<BlockItem> CRATE_ITEM = StructureContent.ITEMS.registerSimpleBlockItem(CRATE);
    public static final DeferredItem<BlockItem> MINERS_LAMP_ITEM = StructureContent.ITEMS.registerSimpleBlockItem(MINERS_LAMP);
    public static final DeferredItem<BlockItem> ORE_CART_ITEM = StructureContent.ITEMS.registerSimpleBlockItem(ORE_CART);
    public static final DeferredItem<BlockItem> TOOL_RACK_ITEM = StructureContent.ITEMS.registerSimpleBlockItem(TOOL_RACK);
    public static final DeferredItem<BlockItem> SMOULDERING_LOG_PILE_ITEM = StructureContent.ITEMS.registerSimpleBlockItem(SMOULDERING_LOG_PILE);
    public static final DeferredItem<BlockItem> WINDLASS_ITEM = StructureContent.ITEMS.registerSimpleBlockItem(WINDLASS);
    public static final DeferredItem<BlockItem> SLUICE_BOX_ITEM = StructureContent.ITEMS.registerSimpleBlockItem(SLUICE_BOX);

    // ---------------------------------------------------------------- block entities

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE_ENTITY =
            StructureContent.BLOCK_ENTITIES.register("crate", () -> new BlockEntityType<>(CrateBlockEntity::new, CRATE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreCartBlockEntity>> ORE_CART_ENTITY =
            StructureContent.BLOCK_ENTITIES.register("ore_cart", () -> new BlockEntityType<>(OreCartBlockEntity::new, ORE_CART.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolRackBlockEntity>> TOOL_RACK_ENTITY =
            StructureContent.BLOCK_ENTITIES.register("tool_rack", () -> new BlockEntityType<>(ToolRackBlockEntity::new, TOOL_RACK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindlassBlockEntity>> WINDLASS_ENTITY =
            StructureContent.BLOCK_ENTITIES.register("windlass", () -> new BlockEntityType<>(WindlassBlockEntity::new, WINDLASS.get()));

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return StructureContent.SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    /** Loads the class, which declares everything above into the structure registers. */
    static void init() {}

    private SharedBlocks() {}
}
