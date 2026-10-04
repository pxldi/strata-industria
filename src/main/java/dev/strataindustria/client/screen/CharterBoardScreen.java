package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.outpost.BoardView;
import dev.strataindustria.transport.outpost.OutpostPayloads;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.RouteIndex;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The charter board (outposts spec 3.1): the name, how much it loads and why, the lines that run to it, and
 * when something last travelled them. An owner can rewrite the name; it is sent when the board closes.
 */
public class CharterBoardScreen extends Screen {
    private static final Identifier BOARD = StrataIndustria.id("textures/gui/charter_board.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".outposts.board.";
    private static final int WIDTH = 176, HEIGHT = 166;
    private static final int INK = 0xFF2E2A26, FADED_INK = 0xFF5E5646, RED_INK = 0xFF8A2E22;
    private static final int MAX_LINES = 5;

    private final BoardView view;
    private EditBox name;
    private int left, top;

    public CharterBoardScreen(BoardView view) {
        super(Component.translatable("block." + StrataIndustria.MOD_ID + ".outpost_charter"));
        this.view = view;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        if (view.mine()) {
            name = new EditBox(font, left + 14, top + 12, WIDTH - 28, 14, Component.translatable(KEY + "name"));
            name.setBordered(false);
            name.setMaxLength(RouteIndex.MAX_NAME);
            name.setValue(view.name());
            name.setTextColor(INK);
            addRenderableWidget(name);
            setInitialFocus(name);
        }
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose())
                .bounds(left + (WIDTH - 60) / 2, top + HEIGHT - 26, 60, 16).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (name != null && !name.getValue().strip().isEmpty() && !name.getValue().strip().equals(view.name())) {
            ClientPacketDistributor.sendToServer(new OutpostPayloads.Rename(view.pos(), name.getValue()));
        }
        super.onClose();
    }

    /** Typing goes to the name box, so the inventory key does not close the board while it is focused. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (name != null && name.isFocused() && event.key() != InputConstants.KEY_ESCAPE && event.key() != InputConstants.KEY_RETURN) {
            return name.keyPressed(event) || true;
        }
        if (event.key() == InputConstants.KEY_RETURN) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BOARD, left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
        if (name == null) g.text(font, view.name(), left + 14, top + 15, INK, false);
        g.text(font, Component.translatable(KEY + "owner", view.ownerName()), left + 14, top + 28, FADED_INK, false);

        int y = top + 42;
        g.text(font, area(), left + 14, y, view.state() == OutpostPlan.State.CHUNK_LIMIT ? RED_INK : INK, false);
        y += 12;
        if (view.state() == OutpostPlan.State.LOADED && view.side() < view.wanted()) {
            g.text(font, Component.translatable(KEY + "cut_to_fit", view.side(), view.side()), left + 14, y, RED_INK, false);
            y += 12;
        }
        if (!view.lines().isEmpty()) {
            g.text(font, Component.translatable(KEY + "lines"), left + 14, y, FADED_INK, false);
            y += 10;
            int shown = 0;
            for (BoardView.Line line : view.lines()) {
                if (shown++ == MAX_LINES) {
                    g.text(font, Component.translatable(KEY + "more", view.lines().size() - MAX_LINES), left + 18, y, FADED_INK, false);
                    y += 10;
                    break;
                }
                g.text(font, Component.translatable(KEY + "line", Component.translatable(line.kind()), line.other()), left + 18, y, INK, false);
                Component status = line.cutAt() == null
                        ? Component.translatable(KEY + "open", line.length())
                        : Component.translatable(KEY + "cut", line.cutAt().getX() + " " + line.cutAt().getY() + " " + line.cutAt().getZ());
                g.text(font, status, left + 24, y + 9, line.cutAt() == null ? FADED_INK : RED_INK, false);
                y += 20;
            }
        }
        g.text(font, traffic(), left + 14, top + HEIGHT - 40, FADED_INK, false);
    }

    private Component area() {
        return switch (view.state()) {
            case LOADED -> Component.translatable(KEY + "area_loaded", view.side(), view.side());
            case NO_LINE -> Component.translatable(KEY + "no_line");
            case OWNER_AWAY -> Component.translatable(KEY + "area_waiting", view.wanted(), view.wanted());
            case CHUNK_LIMIT -> Component.translatable(KEY + "chunk_limit");
        };
    }

    private Component traffic() {
        long ago = view.lastTrafficAgo();
        if (ago < 0) return Component.translatable(KEY + "traffic_none");
        long minutes = ago / 1200;
        if (minutes < 1) return Component.translatable(KEY + "traffic_now");
        if (minutes < 120) return Component.translatable(KEY + "traffic_minutes", minutes);
        return Component.translatable(KEY + "traffic_hours", minutes / 60);
    }
}
