package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.survey.SurveyNotes;
import dev.strataindustria.survey.SurveyNotesItem;
import dev.strataindustria.survey.SurveyText;
import dev.strataindustria.survey.Surveyor;
import dev.strataindustria.structure.Ledgers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * One parchment page of survey notes (structures spec 5.2): what was found, a compass arrow that turns as
 * the reader turns, how far and how deep, and the writer's own words. Read-only; Escape closes it.
 */
public class SurveyNotesScreen extends Screen {
    private static final Identifier PAGE = StrataIndustria.id("textures/gui/survey_notes.png");
    /** Eight hand-drawn arrows (ahead, then clockwise) and a tick mark, 16 px each. */
    private static final Identifier ARROWS = StrataIndustria.id("textures/gui/survey_arrows.png");
    private static final int WIDTH = 176, HEIGHT = 166;
    private static final int INK = 0xFF2E2A26, FADED_INK = 0xFF5E5646;
    /** Top left of the compass rose drawn on the page. */
    private static final int ROSE_X = 10, ROSE_Y = 40, ROSE_SIZE = 64;
    private static final int TEXT_X = 82, TEXT_WIDTH = 84;

    private final ItemStack stack;
    private int left, top;

    public SurveyNotesScreen(ItemStack stack) {
        super(Component.translatable("item." + StrataIndustria.MOD_ID + ".survey_notes"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, PAGE, left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
        String ledger = SurveyNotesItem.ledger(stack);
        g.centeredText(font, ledger != null ? Component.translatable("item." + StrataIndustria.MOD_ID + ".survey_notes.ledger") : title,
                left + WIDTH / 2, top + 8, INK);

        if (ledger != null) {
            // A handwritten page: the place it came from, then what the writer put down.
            g.centeredText(font, Ledgers.place(ledger), left + WIDTH / 2, top + 24, FADED_INK);
            wrapped(g, Ledgers.text(ledger), left + 14, top + 40, WIDTH - 28, INK);
            return;
        }
        SurveyNotes notes = SurveyNotesItem.notes(stack);
        if (notes.entry().isEmpty()) {
            String key = notes.targets().isEmpty() ? "blank" : "unread";
            wrapped(g, Component.translatable("item." + StrataIndustria.MOD_ID + ".survey_notes." + key), left + 14, top + 60,
                    WIDTH - 28, FADED_INK);
            return;
        }
        SurveyNotes.Entry entry = notes.entry().get();
        LocalPlayer player = minecraft.player;

        // Heading: the mineral, with the pebble a prospector would have picked up.
        OreMineral mineral = Surveyor.mineral(entry.mineral());
        Component name = SurveyText.mineralName(entry.mineral());
        int nameWidth = font.width(name) + 18;
        int nameX = left + (WIDTH - nameWidth) / 2;
        if (mineral != null) g.item(new ItemStack(ModItems.SMALL_ORES.get(mineral).get()), nameX, top + 19);
        g.text(font, name, nameX + 18, top + 23, INK, false);

        // Compass: the arrow points toward the deposit relative to where the reader faces. Once the reader
        // has been to the deposit it shows a tick instead, and the bearing is no longer needed.
        double dx = entry.pos().getX() + 0.5 - player.getX(), dz = entry.pos().getZ() + 0.5 - player.getZ();
        int sprite;
        if (notes.found()) {
            sprite = 8;
        } else {
            double bearing = Math.toDegrees(Math.atan2(dx, -dz));
            double facing = player.getYRot() + 180;
            sprite = Math.floorMod((int) Math.round((bearing - facing) / 45.0), 8);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, ARROWS, left + ROSE_X + ROSE_SIZE / 2 - 8, top + ROSE_Y + ROSE_SIZE / 2 - 8,
                sprite * 16, 0, 16, 16, 160, 16);

        // The field note, one fact per line: bearing, then host rock and depth.
        List<Component> lines = new ArrayList<>();
        if (!notes.found()) lines.add(SurveyText.bearing(dx, dz));
        lines.add(SurveyText.where(entry.host(), entry.depth()));
        int y = top + ROSE_Y;
        for (Component line : lines) {
            y = wrapped(g, line, left + TEXT_X, y, TEXT_WIDTH, INK) + 2;
        }

        // The writer's own remark, below the compass.
        int handY = Math.max(y + 2, top + ROSE_Y + ROSE_SIZE + 4);
        List<FormattedCharSequence> hand = font.split(
                SurveyText.hand(entry.mineral(), entry.hand()).copy().withStyle(ChatFormatting.ITALIC), WIDTH - 28);
        int room = Math.max(1, (top + HEIGHT - 10 - handY) / font.lineHeight);
        if (hand.size() > room) {
            // Never run off the page: the last line that fits is cut short and ends in an ellipsis.
            hand = new ArrayList<>(hand.subList(0, room));
            FormattedCharSequence last = hand.get(room - 1);
            hand.set(room - 1, FormattedCharSequence.composite(last, FormattedCharSequence.forward("...", Style.EMPTY)));
        }
        for (FormattedCharSequence line : hand) {
            g.text(font, line, left + 14, handY, FADED_INK, false);
            handY += font.lineHeight;
        }
    }

    /** Draws word-wrapped text and returns the y below it. */
    private int wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int width, int colour) {
        for (FormattedCharSequence line : font.split(text, width)) {
            g.text(font, line, x, y, colour, false);
            y += font.lineHeight;
        }
        return y;
    }
}
