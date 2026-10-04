package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.GroundCoverBlock;
import dev.strataindustria.survey.SurveyNotes;
import dev.strataindustria.survey.SurveyNotesItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Everything the world structures add (structures spec 9 and 14): the camp and mine structure type and
 * pieces, canvas and pit props, the survey notes and their sounds. Kept in its own registers so the
 * structures stay one self-contained package.
 */
public final class StructureContent {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StrataIndustria.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StrataIndustria.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, StrataIndustria.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StrataIndustria.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, StrataIndustria.MOD_ID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, StrataIndustria.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, StrataIndustria.MOD_ID);

    // ---------------------------------------------------------------- blocks

    public static final DeferredBlock<CanvasBlock> FIBRE_CANVAS = BLOCKS.registerBlock("fibre_canvas", CanvasBlock::new,
            p -> p.mapColor(MapColor.SAND)
                    .strength(0.8f)
                    .sound(SoundType.WOOL)
                    .ignitedByLava());
    public static final DeferredBlock<CanvasBlock.Carpet> FIBRE_CANVAS_CARPET = BLOCKS.registerBlock("fibre_canvas_carpet",
            CanvasBlock.Carpet::new,
            p -> p.mapColor(MapColor.SAND)
                    .strength(0.1f)
                    .sound(SoundType.WOOL)
                    .ignitedByLava());
    public static final DeferredBlock<PitPropBlock> PIT_PROP = BLOCKS.registerBlock("pit_prop", PitPropBlock::new,
            p -> p.mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava());

    /** Fire bricks from an old bloomery, cracked by heat; they can no longer hold one (structures spec 9). */
    public static final DeferredBlock<Block> CRACKED_FIRE_BRICKS = BLOCKS.registerSimpleBlock("cracked_fire_bricks",
            p -> p.mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(1.8f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(Sounds.CRACKED_FIRE_BRICKS));
    /** A few lumps of bloomery slag lying on the ground. Worldgen only; it has no item. */
    public static final DeferredBlock<GroundCoverBlock> SLAG_HEAP = BLOCKS.registerBlock("slag_heap",
            p -> new GroundCoverBlock(Block.box(2, 0, 2, 14, 4, 14), p),
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .noCollision()
                    .noOcclusion()
                    .instabreak()
                    .sound(Sounds.SLAG_HEAP)
                    .pushReaction(PushReaction.POPPED));

    public static final DeferredItem<BlockItem> FIBRE_CANVAS_ITEM = ITEMS.registerSimpleBlockItem(FIBRE_CANVAS);
    public static final DeferredItem<BlockItem> FIBRE_CANVAS_CARPET_ITEM = ITEMS.registerSimpleBlockItem(FIBRE_CANVAS_CARPET);
    public static final DeferredItem<BlockItem> PIT_PROP_ITEM = ITEMS.registerSimpleBlockItem(PIT_PROP);
    public static final DeferredItem<BlockItem> CRACKED_FIRE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(CRACKED_FIRE_BRICKS);

    // ---------------------------------------------------------------- survey notes

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SurveyNotes>> SURVEY =
            COMPONENTS.registerComponentType("survey", b -> b
                    .persistent(SurveyNotes.CODEC)
                    .networkSynchronized(SurveyNotes.STREAM_CODEC));
    public static final DeferredItem<SurveyNotesItem> SURVEY_NOTES = ITEMS.registerItem("survey_notes", SurveyNotesItem::new,
            p -> p.stacksTo(1));

    /** A sheet written by someone who worked there instead of a prospector's notes: the text key, such as {@code mining_camp.1}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LEDGER =
            COMPONENTS.registerComponentType("ledger", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // ---------------------------------------------------------------- collectibles (structures v2 section 4.1)

    /** The mineral id of a {@link MineralSpecimenItem}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MINERAL =
            COMPONENTS.registerComponentType("mineral", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredItem<MineralSpecimenItem> MINERAL_SPECIMEN = ITEMS.registerItem("mineral_specimen",
            MineralSpecimenItem::new, p -> p.stacksTo(16));

    public static final DeferredBlock<SpecimenShelfBlock> SPECIMEN_SHELF = BLOCKS.registerBlock("specimen_shelf",
            SpecimenShelfBlock::new,
            p -> p.mapColor(MapColor.WOOD)
                    .strength(1.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava());
    public static final DeferredItem<BlockItem> SPECIMEN_SHELF_ITEM = ITEMS.registerSimpleBlockItem(SPECIMEN_SHELF);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpecimenShelfBlockEntity>> SPECIMEN_SHELF_ENTITY =
            BLOCK_ENTITIES.register("specimen_shelf", () -> new BlockEntityType<>(SpecimenShelfBlockEntity::new, SPECIMEN_SHELF.get()));

    /** One sherd per place; a decorated pot of them records where its owner has been. The ids are also the pot pattern ids. */
    public static final List<String> SHERD_PLACES = List.of("charcoal_burners", "prospector", "mining_camp", "collapsed_adit",
            "ruined_bloomery", "placer_workings");
    public static final Map<String, DeferredItem<Item>> SHERDS = new LinkedHashMap<>();
    /** Armour trims "miner" and "smith": the templates, and the trim pattern each applies. */
    public static final DeferredItem<SmithingTemplateItem> MINER_TRIM_TEMPLATE = ITEMS.registerItem("miner_armor_trim_smithing_template",
            SmithingTemplateItem::createArmorTrimTemplate, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SmithingTemplateItem> SMITH_TRIM_TEMPLATE = ITEMS.registerItem("smith_armor_trim_smithing_template",
            SmithingTemplateItem::createArmorTrimTemplate, p -> p.rarity(Rarity.UNCOMMON));
    public static final ResourceKey<net.minecraft.world.item.equipment.trim.TrimPattern> MINER_TRIM = trim("miner");
    public static final ResourceKey<net.minecraft.world.item.equipment.trim.TrimPattern> SMITH_TRIM = trim("smith");

    /** Banner pattern: crossed pick and hammer. The tag lists the patterns the item grants. */
    public static final TagKey<BannerPattern> PICK_AND_HAMMER_TAG =
            TagKey.create(Registries.BANNER_PATTERN, StrataIndustria.id("pattern_item/pick_and_hammer"));
    public static final ResourceKey<BannerPattern> PICK_AND_HAMMER =
            ResourceKey.create(Registries.BANNER_PATTERN, StrataIndustria.id("pick_and_hammer"));
    public static final DeferredItem<Item> PICK_AND_HAMMER_BANNER_PATTERN = ITEMS.registerItem("pick_and_hammer_banner_pattern",
            Item::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)
                    .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(PICK_AND_HAMMER_TAG)));

    static {
        for (String place : SHERD_PLACES) {
            SHERDS.put(place, ITEMS.registerItem(place + "_pottery_sherd", Item::new,
                    p -> p.rarity(Rarity.UNCOMMON).potPattern(potPattern(place))));
        }
    }

    public static ResourceKey<DecoratedPotPattern> potPattern(String place) {
        return ResourceKey.create(Registries.DECORATED_POT_PATTERN, StrataIndustria.id(place + "_pottery_pattern"));
    }

    private static ResourceKey<net.minecraft.world.item.equipment.trim.TrimPattern> trim(String id) {
        return ResourceKey.create(Registries.TRIM_PATTERN, StrataIndustria.id(id));
    }

    // ---------------------------------------------------------------- sounds

    /** A timber groaning under the weight of a mine roof. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PIT_PROP_CREAK = sound("block.pit_prop.creak");
    /** Stiff paper unfolding. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SURVEY_NOTES_OPEN = sound("item.survey_notes.open");
    /** The reader reaches the deposit the notes describe. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SURVEY_NOTES_FOUND = sound("item.survey_notes.found");
    /** A new place written into the journal. */
    public static final DeferredHolder<SoundEvent, SoundEvent> JOURNAL_PLACE = sound("ui.journal.place");
    /** Heat-cracked brick crumbling apart. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRACKED_FIRE_BRICKS_BREAK = sound("block.cracked_fire_bricks.break");
    /** Gravel with a glassy clink. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SLAG_HEAP_BREAK = sound("block.slag_heap.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLAG_HEAP_STEP = sound("block.slag_heap.step");

    /** Sound types of the new blocks, kept apart so the blocks above can refer to sounds declared below them. */
    private static final class Sounds {
        /** Fire bricks a little lower than intact ones (which ring at 1.1), with their own crumble. */
        static final SoundType CRACKED_FIRE_BRICKS = new DeferredSoundType(1.0f, 0.95f, CRACKED_FIRE_BRICKS_BREAK,
                () -> SoundEvents.DEEPSLATE_BRICKS_STEP, () -> SoundEvents.DEEPSLATE_BRICKS_PLACE,
                () -> SoundEvents.DEEPSLATE_BRICKS_HIT, () -> SoundEvents.DEEPSLATE_BRICKS_FALL);
        static final SoundType SLAG_HEAP = new DeferredSoundType(1.0f, 1.0f, SLAG_HEAP_BREAK, SLAG_HEAP_STEP,
                () -> SoundEvents.GRAVEL_PLACE, () -> SoundEvents.GRAVEL_HIT, () -> SoundEvents.GRAVEL_FALL);
    }

    // ---------------------------------------------------------------- worldgen

    public static final DeferredHolder<StructureType<?>, StructureType<CampStructure>> CAMP =
            STRUCTURE_TYPES.register("camp", () -> () -> CampStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PLAN_PIECE =
            PIECE_TYPES.register("plan", () -> (StructurePieceType.ContextlessType) PlanPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ADIT_PIECE =
            PIECE_TYPES.register("adit", () -> (StructurePieceType.ContextlessType) AditPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SLUICE_PIECE =
            PIECE_TYPES.register("sluice", () -> (StructurePieceType.ContextlessType) SluicePiece::new);

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void register(IEventBus modBus) {
        SharedBlocks.init();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        PIECE_TYPES.register(modBus);
    }

    private StructureContent() {}
}
