package dev.strataindustria.client.hud;

import dev.strataindustria.fire.FirestarterItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/** A small ember bar under the crosshair while a firestarter is being worked. */
public final class FirestarterHud {
    private static final int WIDTH = 32, HEIGHT = 3;

    private FirestarterHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof FirestarterItem)) return;
        float progress = FirestarterItem.progress(player);
        int x = (g.guiWidth() - WIDTH) / 2, y = g.guiHeight() / 2 + 9;
        g.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xC0000000);
        int filled = Math.round(progress * WIDTH);
        // Colour runs from a dull ember to bright flame as the tinder catches.
        int r = 0x6e + Math.round(progress * (0xf0 - 0x6e));
        int gr = 0x1e + Math.round(progress * (0x7a - 0x1e));
        int b = 0x14 + Math.round(progress * (0x22 - 0x14));
        g.fill(x, y, x + filled, y + HEIGHT, 0xFF000000 | r << 16 | gr << 8 | b);
    }
}
