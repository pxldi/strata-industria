package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
import dev.strataindustria.transport.signal.SignalRegistry;
import dev.strataindustria.transport.signal.TimetablePayloads;
import dev.strataindustria.transport.signal.TimetableStops;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Eight lines of stop names (outposts spec 9.4). On a timetable each line also has a rule button that cycles through
 * the stop's own rule and a few common ones; on a route switch the lines are the stops the branch is for. The lines
 * are sent when the screen closes.
 */
public class TimetableScreen extends Screen {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/timetable.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".timetable.";
    private static final int WIDTH = 176, HEIGHT = 200;
    private static final int INK = 0xFF2E2A26, FADED_INK = 0xFF5E5646;
    private static final int ROW = 15, FIRST_ROW = 40;

    /** The rules a line's button steps through: the stop's own (empty), then these. */
    private static final List<Preset> PRESETS = List.of(
            new Preset(Optional.empty(), 10),
            new Preset(Optional.of(StopRule.WAIT), 5), new Preset(Optional.of(StopRule.WAIT), 10),
            new Preset(Optional.of(StopRule.WAIT), 30), new Preset(Optional.of(StopRule.WAIT), 60),
            new Preset(Optional.of(StopRule.FULL), 10), new Preset(Optional.of(StopRule.EMPTY), 10),
            new Preset(Optional.of(StopRule.REDSTONE), 10),
            new Preset(Optional.of(StopRule.IDLE), 10), new Preset(Optional.of(StopRule.IDLE), 30));

    private record Preset(Optional<StopRule> rule, int seconds) {
        Component text() {
            if (rule.isEmpty()) return Component.translatable(KEY + "stop_rule");
            Component name = Component.translatable(rule.get().langKey());
            return rule.get().usesSeconds() ? name.copy().append(" " + seconds + " s") : name;
        }
    }

    private final @Nullable BlockPos switchPos;
    private final String[] names = new String[TimetableStops.MAX_STOPS];
    private final int[] preset = new int[TimetableStops.MAX_STOPS];
    private final EditBox[] boxes = new EditBox[TimetableStops.MAX_STOPS];
    private final Button[] buttons = new Button[TimetableStops.MAX_STOPS];
    private final List<String> original = new ArrayList<>();
    private int left, top;

    private TimetableScreen(@Nullable BlockPos switchPos, Component title) {
        super(title);
        this.switchPos = switchPos;
        java.util.Arrays.fill(names, "");
    }

    public static TimetableScreen forTimetable(TimetableStops stops) {
        TimetableScreen screen = new TimetableScreen(null, Component.translatable("item." + StrataIndustria.MOD_ID + ".timetable"));
        int row = 0;
        for (TimetableStops.Entry entry : stops.entries()) {
            if (row >= TimetableStops.MAX_STOPS) break;
            screen.names[row] = entry.name();
            screen.preset[row] = presetOf(entry);
            row++;
        }
        screen.remember();
        return screen;
    }

    public static TimetableScreen forSwitch(BlockPos pos, List<String> stops) {
        TimetableScreen screen = new TimetableScreen(pos, Component.translatable("block." + StrataIndustria.MOD_ID + ".route_switch"));
        for (int row = 0; row < Math.min(stops.size(), TimetableStops.MAX_STOPS); row++) screen.names[row] = stops.get(row);
        screen.remember();
        return screen;
    }

    private static int presetOf(TimetableStops.Entry entry) {
        for (int i = 0; i < PRESETS.size(); i++) {
            Preset p = PRESETS.get(i);
            if (p.rule().equals(entry.rule()) && (entry.rule().isEmpty() || !entry.rule().get().usesSeconds() || p.seconds() == entry.seconds())) return i;
        }
        return 0;
    }

    private void remember() {
        original.clear();
        original.addAll(lines());
    }

    private boolean forSwitch() {
        return switchPos != null;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        for (int row = 0; row < TimetableStops.MAX_STOPS; row++) {
            int y = top + FIRST_ROW + row * ROW;
            int boxWidth = forSwitch() ? 136 : 88;
            EditBox box = new EditBox(font, left + 24, y + 2, boxWidth, 10, Component.translatable(KEY + "line", row + 1));
            box.setBordered(false);
            box.setMaxLength(StopData.MAX_NAME);
            box.setValue(names[row]);
            box.setTextColor(INK);
            box.setHint(Component.translatable(KEY + "name_hint"));
            boxes[row] = addRenderableWidget(box);
            if (!forSwitch()) {
                final int index = row;
                buttons[row] = addRenderableWidget(Button.builder(PRESETS.get(preset[row]).text(), b -> {
                    preset[index] = (preset[index] + 1) % PRESETS.size();
                    b.setMessage(PRESETS.get(preset[index]).text());
                }).bounds(left + 114, y, 48, 12).build());
            }
        }
        setInitialFocus(boxes[0]);
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose())
                .bounds(left + (WIDTH - 60) / 2, top + HEIGHT - 26, 60, 16).build());
    }

    /** The names as typed, one per line, blanks kept in place until the screen closes. */
    private List<String> lines() {
        List<String> out = new ArrayList<>();
        for (int row = 0; row < TimetableStops.MAX_STOPS; row++) {
            String text = boxes[row] != null ? boxes[row].getValue() : names[row];
            out.add(StopData.cleanName(text));
        }
        return out;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        List<String> now = lines();
        if (switchPos != null) {
            List<String> kept = now.stream().filter(name -> !name.isBlank()).toList();
            if (!kept.equals(original.stream().filter(name -> !name.isBlank()).toList())) {
                ClientPacketDistributor.sendToServer(new TimetablePayloads.UpdateSwitch(switchPos, kept));
            }
        } else {
            List<TimetableStops.Entry> entries = new ArrayList<>();
            for (int row = 0; row < TimetableStops.MAX_STOPS; row++) {
                if (now.get(row).isBlank()) continue;
                Preset p = PRESETS.get(preset[row]);
                entries.add(new TimetableStops.Entry(now.get(row), p.rule(), p.seconds()));
            }
            ClientPacketDistributor.sendToServer(new TimetablePayloads.UpdateItem(new TimetableStops(entries).cleaned()));
        }
        super.onClose();
    }

    /** Typing goes to the focused line, so the inventory key does not close the screen. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        for (EditBox box : boxes) {
            if (box != null && box.isFocused() && event.key() != InputConstants.KEY_ESCAPE && event.key() != InputConstants.KEY_RETURN
                    && event.key() != InputConstants.KEY_TAB) {
                return box.keyPressed(event) || true;
            }
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
        g.text(font, title, left + 14, top + 14, INK, false);
        g.text(font, Component.translatable(KEY + (forSwitch() ? "branch_for" : "stops_in_order")), left + 14, top + 27, FADED_INK, false);
        for (int row = 0; row < TimetableStops.MAX_STOPS; row++) {
            g.text(font, Component.literal(Integer.toString(row + 1)), left + 14, top + FIRST_ROW + row * ROW + 2, FADED_INK, false);
        }
    }
}
