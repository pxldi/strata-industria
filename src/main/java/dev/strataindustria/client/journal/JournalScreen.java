package dev.strataindustria.client.journal;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.JournalClient;
import dev.strataindustria.journal.JournalContent;
import dev.strataindustria.journal.JournalState;
import dev.strataindustria.journal.Leads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The leads notebook (journal leads spec): open leads on the left page, the notes log on the right, a corkboard of
 * everything found so far, and the old checklist behind a config switch.
 */
public class JournalScreen extends Screen {
    static final Identifier NOTEBOOK = StrataIndustria.id("textures/gui/journal/notebook.png");
    static final Identifier CORKBOARD = StrataIndustria.id("textures/gui/journal/corkboard.png");
    static final Identifier WIDGETS = JournalToast.WIDGETS;
    static final int W = 292, H = 180;
    static final int INK = 0xFF2E2A26, FADED_INK = 0xFF6E6250, RED_INK = 0xFF8A3A2A, TWINE = 0xFFC8A870, TWINE_SHADOW = 0xFF5A4630;

    /** Text columns of the two pages, relative to the spread. */
    static final int LEFT_X = 18, RIGHT_X = 160, PAGE_W = 114, TEXT_TOP = 30, TEXT_BOTTOM = 160;
    static final int ARROW_Y = 162, ARROW_W = 12, ARROW_H = 9;
    static final int TAB_X = W - 6, TAB_Y = 14, TAB_W = 22, TAB_H = 18, TAB_STEP = 21;
    static final int CARD = 26, CARD_STEP_X = 36, CARD_STEP_Y = 32;
    /** The corkboard's inner edge inside its frame. */
    static final int CORK_INSET = 10;

    enum View { LEADS, CORKBOARD }

    private View view = View.LEADS;
    private int left, top;
    private int leadPage, notePage;
    private double panX, panY;
    private boolean panned;
    private JournalState state;

    public JournalScreen() {
        super(Component.translatable("item." + StrataIndustria.MOD_ID + ".field_journal"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private JournalState state() {
        return minecraft.player.getData(JournalContent.STATE);
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        state = state();
        drawTabs(g, mouseX, mouseY);
        if (view == View.LEADS) {
            g.blit(RenderPipelines.GUI_TEXTURED, NOTEBOOK, left, top, 0, 0, W, H, 512, 256);
            drawLeads(g, mouseX, mouseY);
            drawNotes(g, mouseX, mouseY);
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, CORKBOARD, left, top, 0, 0, W, H, 512, 256);
            drawCorkboard(g, mouseX, mouseY);
        }
    }

    private List<View> tabs() {
        return List.of(View.LEADS, View.CORKBOARD);
    }

    private boolean checklistTab() {
        return Config.JOURNAL_CHECKLIST.get();
    }

    private void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int count = tabs().size() + (checklistTab() ? 1 : 0);
        for (int i = 0; i < count; i++) {
            boolean selected = i < tabs().size() && tabs().get(i) == view;
            int x = left + TAB_X + (selected ? 2 : 0), y = top + TAB_Y + i * TAB_STEP;
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, x, y, i * TAB_W, selected ? 82 : 64, TAB_W, TAB_H, 256, 256);
            if (inside(mouseX, mouseY, x, y, TAB_W, TAB_H)) {
                String key = i == 0 ? "tab.leads" : i == 1 ? "tab.corkboard" : "tab.checklist";
                g.setTooltipForNextFrame(JournalText.ui(key), mouseX, mouseY);
            }
        }
    }

    /** A block of lines drawn as one unit, so a lead never splits across pages. */
    private record Entry(int height, EntryDrawer drawer) {}

    private interface EntryDrawer {
        void draw(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY);
    }

    private static List<List<Entry>> paginate(List<Entry> entries) {
        List<List<Entry>> pages = new ArrayList<>();
        List<Entry> page = new ArrayList<>();
        int used = 0;
        for (Entry entry : entries) {
            if (!page.isEmpty() && used + entry.height() > TEXT_BOTTOM - TEXT_TOP) {
                pages.add(page);
                page = new ArrayList<>();
                used = 0;
            }
            page.add(entry);
            used += entry.height();
        }
        if (!page.isEmpty()) pages.add(page);
        return pages;
    }

    private void drawLeads(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = left + LEFT_X;
        g.centeredText(font, JournalText.ui("leads"), x + PAGE_W / 2, top + 14, INK);
        List<Entry> entries = new ArrayList<>();
        int tier = -1;
        for (JournalState.Lead lead : sortedOpenLeads()) {
            int leadTier = JournalText.tier(lead);
            if (leadTier != tier) {
                tier = leadTier;
                Component chapter = JournalText.chapter(tier);
                entries.add(new Entry(12, (gg, ex, ey, mx, my) -> {
                    gg.text(font, chapter, ex, ey + 1, RED_INK, false);
                    gg.fill(ex, ey + 10, ex + font.width(chapter), ey + 11, 0x808A3A2A);
                }));
            }
            List<FormattedCharSequence> question = font.split(JournalText.question(lead), PAGE_W - 20);
            List<FormattedCharSequence> hint = lead.hinted()
                    ? font.split(JournalText.ui("remember", JournalText.hint(lead)).copy().withStyle(ChatFormatting.ITALIC), PAGE_W - 4)
                    : List.of();
            int height = Math.max(18, question.size() * 9 + 2) + hint.size() * 9 + 5;
            ItemStack icon = JournalText.icon(lead.icon());
            entries.add(new Entry(height, (gg, ex, ey, mx, my) -> {
                gg.item(icon, ex, ey);
                int ty = ey + (question.size() == 1 ? 4 : 0);
                for (FormattedCharSequence line : question) {
                    gg.text(font, line, ex + 20, ty, INK, false);
                    ty += 9;
                }
                ty = Math.max(ty, ey + 18);
                for (FormattedCharSequence line : hint) {
                    gg.text(font, line, ex + 4, ty, FADED_INK, false);
                    ty += 9;
                }
                if (inside(mx, my, ex, ey, 16, 16)) gg.setTooltipForNextFrame(JournalText.title(lead), mx, my);
            }));
        }
        if (entries.isEmpty()) {
            wrapped(g, JournalText.ui(state.leads().isEmpty() ? "no_leads" : "all_done"), x, top + TEXT_TOP, PAGE_W, FADED_INK);
            return;
        }
        List<List<Entry>> pages = paginate(entries);
        leadPage = Math.clamp(leadPage, 0, pages.size() - 1);
        drawPage(g, pages.get(leadPage), x, mouseX, mouseY);
        drawArrows(g, x, leadPage, pages.size(), mouseX, mouseY);
    }

    /** Open leads by chapter, then in the order they were opened. */
    private List<JournalState.Lead> sortedOpenLeads() {
        List<JournalState.Lead> open = Leads.open(state);
        open.sort((a, b) -> JournalText.tier(a) != JournalText.tier(b)
                ? Integer.compare(JournalText.tier(a), JournalText.tier(b)) : Integer.compare(a.openedSeq(), b.openedSeq()));
        return open;
    }

    private void drawNotes(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = left + RIGHT_X;
        g.centeredText(font, JournalText.ui("notes"), x + PAGE_W / 2, top + 14, INK);
        record Item(int seq, Entry entry) {}
        List<Item> items = new ArrayList<>();
        for (JournalState.Lead lead : state.leads().values()) {
            if (!lead.closed()) continue;
            Component closing = JournalText.closing(lead);
            List<FormattedCharSequence> title = font.split(JournalText.title(lead).copy().withStyle(ChatFormatting.STRIKETHROUGH), PAGE_W - 26);
            List<FormattedCharSequence> body = closing == null ? List.of() : font.split(closing, PAGE_W - 26);
            Component day = JournalText.ui("day", lead.closedDay());
            int height = Math.max(24, 9 + (title.size() + body.size()) * 9) + 6;
            ItemStack icon = JournalText.icon(lead.icon());
            items.add(new Item(lead.closedSeq(), new Entry(height, (gg, ex, ey, mx, my) -> {
                gg.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, ex, ey, 0, 40, 22, 22, 256, 256);
                gg.item(icon, ex + 3, ey + 3);
                gg.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, ex + 14, ey + 14, 8, 26, 10, 10, 256, 256);
                gg.text(font, day, ex + 26, ey, FADED_INK, false);
                int ty = ey + 9;
                for (FormattedCharSequence line : title) {
                    gg.text(font, line, ex + 26, ty, INK, false);
                    ty += 9;
                }
                for (FormattedCharSequence line : body) {
                    gg.text(font, line, ex + 26, ty, FADED_INK, false);
                    ty += 9;
                }
            })));
        }
        for (JournalState.Note note : state.notes()) {
            List<FormattedCharSequence> body = font.split(JournalText.note(note), PAGE_W - 20);
            Component day = JournalText.ui("day", note.day());
            ItemStack icon = note.icon().map(JournalText::icon).orElse(ItemStack.EMPTY);
            int height = Math.max(18, 9 + body.size() * 9) + 6;
            items.add(new Item(note.seq(), new Entry(height, (gg, ex, ey, mx, my) -> {
                if (!icon.isEmpty()) gg.item(icon, ex, ey + 1);
                gg.text(font, day, ex + 20, ey, FADED_INK, false);
                int ty = ey + 9;
                for (FormattedCharSequence line : body) {
                    gg.text(font, line, ex + 20, ty, INK, false);
                    ty += 9;
                }
            })));
        }
        if (items.isEmpty()) {
            wrapped(g, JournalText.ui("no_notes"), x, top + TEXT_TOP, PAGE_W, FADED_INK);
            return;
        }
        items.sort((a, b) -> Integer.compare(b.seq(), a.seq()));
        List<Entry> entries = new ArrayList<>();
        items.forEach(item -> entries.add(item.entry()));
        List<List<Entry>> pages = paginate(entries);
        notePage = Math.clamp(notePage, 0, pages.size() - 1);
        drawPage(g, pages.get(notePage), x, mouseX, mouseY);
        drawArrows(g, x, notePage, pages.size(), mouseX, mouseY);
    }

    private void drawPage(GuiGraphicsExtractor g, List<Entry> page, int x, int mouseX, int mouseY) {
        int y = top + TEXT_TOP;
        for (Entry entry : page) {
            entry.drawer().draw(g, x, y, mouseX, mouseY);
            y += entry.height();
        }
    }

    private void drawArrows(GuiGraphicsExtractor g, int x, int page, int pages, int mouseX, int mouseY) {
        if (pages <= 1) return;
        int y = top + ARROW_Y;
        if (page > 0) {
            boolean hover = inside(mouseX, mouseY, x, y, ARROW_W, ARROW_H);
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, x, y, 22, hover ? 49 : 40, ARROW_W, ARROW_H, 256, 256);
        }
        if (page < pages - 1) {
            int ax = x + PAGE_W - ARROW_W;
            boolean hover = inside(mouseX, mouseY, ax, y, ARROW_W, ARROW_H);
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, ax, y, 34, hover ? 49 : 40, ARROW_W, ARROW_H, 256, 256);
        }
        g.centeredText(font, JournalText.ui("page", page + 1, pages), x + PAGE_W / 2, y + 1, FADED_INK);
    }

    // ---------------------------------------------------------------- corkboard

    /** Leads found so far, open or closed: the part of the tree the player has uncovered. */
    private List<JournalState.Lead> pinned() {
        List<JournalState.Lead> pinned = new ArrayList<>();
        for (JournalState.Lead lead : state.leads().values()) {
            if (lead.openedSeq() >= 0) pinned.add(lead);
        }
        return pinned;
    }

    private int cardX(JournalState.Lead lead) {
        return (int) Math.round(lead.x() / 28.0 * CARD_STEP_X + panX);
    }

    private int cardY(JournalState.Lead lead) {
        return (int) Math.round(lead.y() / 27.0 * CARD_STEP_Y + panY);
    }

    private void drawCorkboard(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<JournalState.Lead> pinned = pinned();
        int x0 = left + CORK_INSET, y0 = top + CORK_INSET, x1 = left + W - CORK_INSET, y1 = top + H - CORK_INSET;
        if (pinned.isEmpty()) {
            g.centeredText(font, JournalText.ui("no_leads"), left + W / 2, top + H / 2 - 4, 0xFFE8DCC0);
            return;
        }
        if (!panned) centreOnNewest(pinned);
        Map<String, JournalState.Lead> byPath = new HashMap<>();
        pinned.forEach(lead -> byPath.put(lead.path(), lead));

        g.enableScissor(x0, y0, x1, y1);
        // String first, so the cards sit on top of it.
        for (JournalState.Lead lead : pinned) {
            JournalState.Lead parent = byPath.get(lead.parent());
            if (parent == null) continue;
            int ax = x0 + cardX(parent) + CARD / 2, ay = y0 + cardY(parent) + 4;
            int bx = x0 + cardX(lead) + CARD / 2, by = y0 + cardY(lead) + 4;
            line(g, ax, ay + 1, bx, by + 1, TWINE_SHADOW);
            line(g, ax, ay, bx, by, TWINE);
        }
        JournalState.Lead hovered = null;
        for (JournalState.Lead lead : pinned) {
            int cx = x0 + cardX(lead), cy = y0 + cardY(lead);
            if (cx > x1 || cy > y1 || cx + CARD < x0 || cy + CARD < y0) continue;
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, cx, cy, lead.closed() ? CARD : 0, 0, CARD, CARD, 256, 256);
            g.item(JournalText.icon(lead.icon()), cx + 5, cy + 6);
            if (lead.closed()) {
                g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, cx + CARD - 11, cy + CARD - 11, 8, 26, 10, 10, 256, 256);
            }
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, cx + CARD / 2 - 4, cy - 2, 0, 26, 8, 8, 256, 256);
            if (inside(mouseX, mouseY, cx, cy, CARD, CARD) && inside(mouseX, mouseY, x0, y0, x1 - x0, y1 - y0)) hovered = lead;
        }
        g.disableScissor();
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(JournalText.title(hovered).copy().withStyle(hovered.closed() ? ChatFormatting.STRIKETHROUGH : ChatFormatting.WHITE));
            if (hovered.closed()) {
                Component closing = JournalText.closing(hovered);
                if (closing != null) lines.add(closing.copy().withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(JournalText.question(hovered).copy().withStyle(ChatFormatting.GRAY));
                if (hovered.hinted()) lines.add(JournalText.hint(hovered).copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            List<FormattedCharSequence> wrappedLines = new ArrayList<>();
            for (Component line : lines) wrappedLines.addAll(font.split(line, 200));
            g.setTooltipForNextFrame(wrappedLines, mouseX, mouseY);
        }
        g.text(font, JournalText.ui("corkboard.drag"), x0 + 2, y1 - 10, 0xC0E8DCC0, true);
    }

    /** Starts the board on the lead opened last, so the player sees where they are. */
    private void centreOnNewest(List<JournalState.Lead> pinned) {
        JournalState.Lead newest = pinned.getFirst();
        for (JournalState.Lead lead : pinned) {
            if (lead.open() && (!newest.open() || lead.openedSeq() > newest.openedSeq())) newest = lead;
        }
        panX = 0;
        panY = 0;
        panX = (W - 2 * CORK_INSET) / 2.0 - CARD / 2.0 - cardX(newest);
        panY = (H - 2 * CORK_INSET) / 2.0 - CARD / 2.0 - cardY(newest);
        panned = true;
    }

    /** A 1 px line of twine between two pins. */
    private static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int colour) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        for (int steps = 0; steps < 4000; steps++) {
            g.fill(x0, y0, x0 + 1, y0 + 1, colour);
            if (x0 == x1 && y0 == y1) return;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int count = tabs().size() + (checklistTab() ? 1 : 0);
        for (int i = 0; i < count; i++) {
            int x = left + TAB_X, y = top + TAB_Y + i * TAB_STEP;
            if (!inside(mx, my, x, y, TAB_W + 2, TAB_H)) continue;
            if (i >= tabs().size()) {
                JournalClient.openChecklist();
            } else if (tabs().get(i) != view) {
                view = tabs().get(i);
                play(view == View.CORKBOARD ? JournalContent.PIN.get() : JournalContent.PAGE.get());
            }
            return true;
        }
        if (view == View.LEADS && event.button() == 0) {
            int y = top + ARROW_Y;
            if (turn(mx, my, left + LEFT_X, y, true)) return true;
            if (turn(mx, my, left + RIGHT_X, y, false)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Clicks on one page's arrows; true if a page turned. */
    private boolean turn(double mx, double my, int x, int y, boolean leads) {
        int delta = inside(mx, my, x, y, ARROW_W, ARROW_H) ? -1 : inside(mx, my, x + PAGE_W - ARROW_W, y, ARROW_W, ARROW_H) ? 1 : 0;
        if (delta == 0) return false;
        if (leads) leadPage = Math.max(0, leadPage + delta);
        else notePage = Math.max(0, notePage + delta);
        play(JournalContent.PAGE.get());
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (view == View.CORKBOARD && event.button() == 0) {
            panX += dx;
            panY += dy;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (view == View.LEADS && scrollY != 0) {
            int delta = scrollY > 0 ? -1 : 1;
            if (x < left + W / 2.0) leadPage = Math.max(0, leadPage + delta);
            else notePage = Math.max(0, notePage + delta);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private void play(SoundEvent sound) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f, 0.7f));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private void wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int width, int colour) {
        for (FormattedCharSequence line : font.split(text, width)) {
            g.text(font, line, x, y, colour, false);
            y += font.lineHeight;
        }
    }
}
