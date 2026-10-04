package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.telegraph.DispatchView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The full dispatch board (outposts spec 9.1): every outpost that reports on the line, two chalk lines each. The
 * first says how the charter stands, the second how its line runs, how full the crate is and when something last came.
 */
public class DispatchBoardScreen extends Screen {
    private static final Identifier SLATE = StrataIndustria.id("textures/gui/dispatch_board.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".dispatch.";
    private static final int WIDTH = 224, HEIGHT = 196;
    private static final int CHALK = 0xFFE9E5D8, FADED = 0xFF9DA2A8, TROUBLE = 0xFFD98B72, DIM = 0xFF6F7379;
    private static final int ROWS = 8, ROW_HEIGHT = 19;

    private final DispatchView view;
    private int left, top;

    public DispatchBoardScreen(DispatchView view) {
        super(Component.translatable("block." + StrataIndustria.MOD_ID + ".dispatch_board"));
        this.view = view;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose())
                .bounds(left + (WIDTH - 60) / 2, top + HEIGHT - 24, 60, 16).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, SLATE, left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
        g.text(font, title, left + 14, top + 12, CHALK, false);
        if (!view.onLine()) {
            g.text(font, Component.translatable(KEY + "no_pole"), left + 14, top + 34, TROUBLE, false);
            return;
        }
        if (view.lines().isEmpty()) {
            g.text(font, Component.translatable(KEY + "empty"), left + 14, top + 34, FADED, false);
            g.text(font, Component.translatable(KEY + "empty_hint"), left + 14, top + 46, DIM, false);
            return;
        }
        int y = top + 30;
        int shown = 0;
        for (DispatchView.Line line : view.lines()) {
            if (shown++ == ROWS) {
                g.text(font, Component.translatable(KEY + "more", view.lines().size() - ROWS), left + 18, y, FADED, false);
                break;
            }
            int colour = line.silent() ? DIM : line.state() == 0 ? CHALK : line.state() == 1 ? 0xFFC4C8CE : TROUBLE;
            g.text(font, line.name(), left + 14, y, colour, false);
            Component state = Component.translatable(KEY + "state." + line.state());
            g.text(font, state, left + WIDTH - 14 - font.width(state), y, colour, false);
            g.text(font, detail(line), left + 20, y + 9, line.silent() ? DIM : line.line() == 1 ? TROUBLE : FADED, false);
            y += ROW_HEIGHT;
        }
    }

    /** "tramway open, crate 48%, last tub 3 min" in the voice of someone writing it on a slate. */
    private Component detail(DispatchView.Line line) {
        Component text = switch (line.line()) {
            case 0 -> Component.translatable(KEY + "line.open", Component.translatable(line.kind()));
            case 1 -> Component.translatable(KEY + "line.cut", Component.translatable(line.kind()));
            case 2 -> Component.translatable(KEY + "line.telegraph_only");
            default -> Component.translatable(KEY + "line.none");
        };
        Component out = text;
        if (line.fill() >= 0) out = Component.translatable(KEY + "join", out, Component.translatable(KEY + "crate", line.fill()));
        if (line.line() == 0 && line.trafficAgo() >= 0) out = Component.translatable(KEY + "join", out, ago(line));
        if (line.silent()) out = Component.translatable(KEY + "join", out, Component.translatable(KEY + "silent", minutes(line.heardAgo())));
        return out;
    }

    private static Component ago(DispatchView.Line line) {
        long minutes = line.trafficAgo() / 1200;
        String last = KEY + "last." + line.kind().substring(line.kind().lastIndexOf('.') + 1);
        if (minutes < 1) return Component.translatable(KEY + "just_now", Component.translatable(last));
        if (minutes < 120) return Component.translatable(KEY + "minutes_ago", Component.translatable(last), minutes);
        return Component.translatable(KEY + "hours_ago", Component.translatable(last), minutes / 60);
    }

    private static long minutes(long ticks) {
        return Math.max(1, ticks / 1200);
    }
}
