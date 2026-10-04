package dev.strataindustria.client;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.screen.SurveyNotesScreen;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.survey.SurveyNotes;
import dev.strataindustria.survey.SurveyNotesItem;
import dev.strataindustria.survey.Surveyor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jspecify.annotations.Nullable;

/** Client half of the survey notes: the screen and the mineral swatches. */
public final class SurveyClient {
    private SurveyClient() {}

    public static void open(ItemStack stack) {
        Minecraft.getInstance().gui.setScreen(new SurveyNotesScreen(stack));
    }

    static void registerTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(StrataIndustria.id("survey_mineral"), MineralTint.CODEC);
        event.register(StrataIndustria.id("specimen_mineral"), SpecimenTint.CODEC);
    }

    /** Tints the facet layer of a mineral specimen in its mineral's colour. */
    public record SpecimenTint() implements ItemTintSource {
        public static final MapCodec<SpecimenTint> CODEC = MapCodec.unit(new SpecimenTint());

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            String mineral = dev.strataindustria.structure.MineralSpecimenItem.mineral(stack);
            return 0xFF000000 | colour(mineral == null ? "" : mineral);
        }

        @Override
        public MapCodec<SpecimenTint> type() {
            return CODEC;
        }
    }

    /** Tints the swatch on the notes icon in the colour of the mineral they describe. */
    public record MineralTint() implements ItemTintSource {
        public static final MapCodec<MineralTint> CODEC = MapCodec.unit(new MineralTint());
        /** Unread or blank notes show a plain charcoal smudge. */
        private static final int SMUDGE = 0xFF4A4A52;

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            SurveyNotes notes = SurveyNotesItem.notes(stack);
            if (notes.entry().isEmpty()) return SMUDGE;
            OreMineral mineral = Surveyor.mineral(notes.entry().get().mineral());
            return mineral == null ? SMUDGE : 0xFF000000 | colour(mineral.id());
        }

        @Override
        public MapCodec<MineralTint> type() {
            return CODEC;
        }
    }

    /** Step 4 of each mineral's ramp in the texture generator: light enough to read on the paper. */
    static int colour(String mineral) {
        return switch (mineral) {
            case "native_copper" -> 0xCF7A3E;
            case "malachite" -> 0x4A9466;
            case "tennantite" -> 0x5C6672;
            case "cassiterite" -> 0x6E5444;
            case "bismuthinite" -> 0x7A8292;
            case "hematite" -> 0x8E4A34;
            case "magnetite" -> 0x4A4E58;
            case "limonite" -> 0xA6762C;
            case "native_gold" -> 0xC08A26;
            default -> 0x6E625A;
        };
    }
}
