package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.rail.RailPayloads;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
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
 * The tub stop (outposts spec 6.3): its name, when it lets a consist go, and whether the consist turns back.
 * The settings are sent when the screen closes.
 */
public class TubStopScreen extends Screen {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/tub_stop.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".stop.";
    private static final int WIDTH = 176, HEIGHT = 166;
    private static final int INK = 0xFF2E2A26, FADED_INK = 0xFF5E5646;
    private static final int[] SECONDS = {1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 300, 600};

    private final StopData data;
    private final String station;
    private StopRule rule;
    private int seconds;
    private boolean reverse;
    private EditBox name;
    private Button ruleButton, secondsButton, reverseButton;
    private int left, top;

    public TubStopScreen(StopData data, String station) {
        super(Component.translatable("block." + StrataIndustria.MOD_ID + ".tub_stop"));
        this.data = data;
        this.station = station;
        this.rule = data.rule();
        this.seconds = data.seconds();
        this.reverse = data.reverse();
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        name = new EditBox(font, left + 14, top + 12, WIDTH - 28, 14, Component.translatable(KEY + "name"));
        name.setBordered(false);
        name.setMaxLength(StopData.MAX_NAME);
        name.setValue(data.name());
        name.setTextColor(INK);
        name.setHint(Component.translatable(KEY + "name_hint"));
        addRenderableWidget(name);
        setInitialFocus(name);

        ruleButton = addRenderableWidget(Button.builder(ruleText(), b -> {
            rule = rule.next();
            refresh();
        }).bounds(left + 14, top + 56, WIDTH - 28, 16).build());
        secondsButton = addRenderableWidget(Button.builder(secondsText(), b -> {
            int index = 0;
            for (int i = 0; i < SECONDS.length; i++) if (SECONDS[i] <= seconds) index = i;
            seconds = SECONDS[(index + 1) % SECONDS.length];
            refresh();
        }).bounds(left + 14, top + 76, WIDTH - 28, 16).build());
        reverseButton = addRenderableWidget(Button.builder(reverseText(), b -> {
            reverse = !reverse;
            refresh();
        }).bounds(left + 14, top + 98, WIDTH - 28, 16).build());
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose())
                .bounds(left + (WIDTH - 60) / 2, top + HEIGHT - 26, 60, 16).build());
        refresh();
    }

    private void refresh() {
        ruleButton.setMessage(ruleText());
        secondsButton.setMessage(secondsText());
        secondsButton.active = rule.usesSeconds();
        reverseButton.setMessage(reverseText());
    }

    private Component ruleText() {
        return Component.translatable(rule.langKey());
    }

    private Component secondsText() {
        return Component.translatable(KEY + "seconds", seconds);
    }

    private Component reverseText() {
        return Component.translatable(KEY + (reverse ? "reverse_on" : "reverse_off"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        StopData now = new StopData(data.pos(), StopData.cleanName(name.getValue()), rule, StopData.cleanSeconds(seconds), reverse);
        if (!now.equals(data)) ClientPacketDistributor.sendToServer(new RailPayloads.UpdateStop(now));
        super.onClose();
    }

    /** Typing goes to the name box, so the inventory key does not close the screen while it is focused. */
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
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
        if (!station.isEmpty()) {
            g.text(font, Component.translatable(KEY + "station", station), left + 14, top + 28, FADED_INK, false);
        }
        g.text(font, Component.translatable(KEY + "leaves"), left + 14, top + 45, FADED_INK, false);
        g.text(font, Component.translatable(KEY + "redstone"), left + 14, top + HEIGHT - 40, FADED_INK, false);
    }
}
