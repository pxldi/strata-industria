package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.survey.SurveyNotes;
import dev.strataindustria.survey.SurveyNotesItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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
                    .ignitedByLava()
                    .pushReaction(PushReaction.NORMAL));

    public static final DeferredItem<BlockItem> FIBRE_CANVAS_ITEM = ITEMS.registerSimpleBlockItem(FIBRE_CANVAS);
    public static final DeferredItem<BlockItem> FIBRE_CANVAS_CARPET_ITEM = ITEMS.registerSimpleBlockItem(FIBRE_CANVAS_CARPET);
    public static final DeferredItem<BlockItem> PIT_PROP_ITEM = ITEMS.registerSimpleBlockItem(PIT_PROP);

    // ---------------------------------------------------------------- survey notes

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SurveyNotes>> SURVEY =
            COMPONENTS.registerComponentType("survey", b -> b
                    .persistent(SurveyNotes.CODEC)
                    .networkSynchronized(SurveyNotes.STREAM_CODEC));
    public static final DeferredItem<SurveyNotesItem> SURVEY_NOTES = ITEMS.registerItem("survey_notes", SurveyNotesItem::new,
            p -> p.stacksTo(1));

    // ---------------------------------------------------------------- sounds

    /** A timber groaning under the weight of a mine roof. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PIT_PROP_CREAK = sound("block.pit_prop.creak");
    /** Stiff paper unfolding. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SURVEY_NOTES_OPEN = sound("item.survey_notes.open");
    /** The reader reaches the deposit the notes describe. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SURVEY_NOTES_FOUND = sound("item.survey_notes.found");
    /** A new place written into the journal. */
    public static final DeferredHolder<SoundEvent, SoundEvent> JOURNAL_PLACE = sound("ui.journal.place");

    // ---------------------------------------------------------------- worldgen

    public static final DeferredHolder<StructureType<?>, StructureType<CampStructure>> CAMP =
            STRUCTURE_TYPES.register("camp", () -> () -> CampStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PLAN_PIECE =
            PIECE_TYPES.register("plan", () -> (StructurePieceType.ContextlessType) PlanPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ADIT_PIECE =
            PIECE_TYPES.register("adit", () -> (StructurePieceType.ContextlessType) AditPiece::new);

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        PIECE_TYPES.register(modBus);
    }

    private StructureContent() {}
}
