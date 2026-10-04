package dev.strataindustria.client.journal;

import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * A torn notebook slip in the corner (journal leads spec): what kind of entry was written, its icon, and the
 * first words of it. Replaces the vanilla advancement toast for journal goals.
 */
public class JournalToast implements Toast {
    static final Identifier WIDGETS = StrataIndustria.id("textures/gui/journal/widgets.png");
    /** Where the slip sits in the widgets sheet. */
    static final int U = 0, V = 96;
    static final int DISPLAY_TIME = 6000;
    static final int INK = 0xFF2E2A26, FADED_INK = 0xFF6E6250, RED_INK = 0xFF8A3A2A;

    private final Component heading;
    private final Component text;
    private final ItemStack icon;
    private final boolean red;
    private Toast.Visibility visibility = Toast.Visibility.HIDE;

    public JournalToast(Component heading, Component text, ItemStack icon, boolean red) {
        this.heading = heading;
        this.text = text;
        this.icon = icon;
        this.red = red;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        visibility = fullyVisibleForMs >= DISPLAY_TIME * manager.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
        g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, 0, 0, U, V, width(), height(), 256, 256);
        g.text(font, heading, 30, 7, red ? RED_INK : FADED_INK, false);
        List<FormattedCharSequence> lines = font.split(text, 122);
        if (!lines.isEmpty()) {
            FormattedCharSequence first = lines.getFirst();
            if (lines.size() > 1) {
                // Cut the line short and trail off, as a slip torn from a longer page.
                String plain = font.plainSubstrByWidth(text.getString(), 112);
                g.text(font, plain.stripTrailing() + "...", 30, 18, INK, false);
            } else {
                g.text(font, first, 30, 18, INK, false);
            }
        }
        if (!icon.isEmpty()) g.fakeItem(icon, 8, 8);
    }
}
