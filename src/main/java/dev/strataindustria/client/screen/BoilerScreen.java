package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.steam.BoilerBlockEntity;
import dev.strataindustria.steam.BoilerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Bronze boiler: the water and steam tanks on the left, the pressure dial with its green, amber and
 * red arcs in the middle, the shell's integrity and the fire on the right, and the status underneath.
 */
public class BoilerScreen extends AbstractContainerScreen<BoilerMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/bronze_boiler.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".boiler.";
    /** Tank insides; their fill strips are at (WATER_U, 0) and (STEAM_U, 0) in the texture. */
    public static final int WATER_X = 8, STEAM_X = 30, TANK_Y = 17, TANK_W = 16, TANK_H = 52, WATER_U = 176, STEAM_U = 192;
    /** Dial centre and needle length; the dial runs from 0 bar on the left to the rated pressure on the right. */
    public static final int DIAL_X = 88, DIAL_Y = 44, NEEDLE = 18;
    /** Integrity bar inside. */
    public static final int BAR_X = 120, BAR_Y = 19, BAR_W = 48, BAR_H = 4;
    public static final int STATUS_Y = 62, COLUMN_X = 120;

    public BoilerScreen(BoilerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = BoilerMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        tank(g, WATER_X, WATER_U, menu.water() / (float) BoilerBlockEntity.WATER_CAPACITY);
        tank(g, STEAM_X, STEAM_U, menu.steam() / (float) BoilerBlockEntity.STEAM_CAPACITY);

        // The needle, from 180 degrees (empty) round to 0 (rated pressure).
        float share = Math.min(1.0f, menu.pressure() / BoilerBlockEntity.RATED_PRESSURE);
        double angle = Math.PI * (1.0 - share);
        int cx = leftPos + DIAL_X, cy = topPos + DIAL_Y;
        for (int r = 2; r <= NEEDLE; r++) {
            int x = cx + (int) Math.round(Math.cos(angle) * r), y = cy - (int) Math.round(Math.sin(angle) * r);
            g.fill(x, y, x + 1, y + 1, r > NEEDLE - 4 ? 0xFFB02A1E : 0xFF2A2626);
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF3A3230);

        float integrity = menu.integrity();
        int w = Math.round(BAR_W * Math.max(0.0f, Math.min(1.0f, integrity / 100.0f)));
        if (w > 0) {
            int colour = integrity > 50 ? 0xFF4C9A3A : integrity > 25 ? 0xFFD8A030 : 0xFFC8342A;
            g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + w, topPos + BAR_Y + BAR_H, colour);
        }
    }

    private void tank(GuiGraphicsExtractor g, int x, int u, float share) {
        if (share <= 0) return;
        int h = Math.max(1, Math.round(TANK_H * Math.min(1.0f, share)));
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + x, topPos + TANK_Y + TANK_H - h, u, TANK_H - h, TANK_W, h, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        Component bar = Component.translatable(KEY + "pressure", String.format(java.util.Locale.ROOT, "%.1f", menu.pressure()));
        g.text(font, bar, DIAL_X - font.width(bar) / 2, DIAL_Y + 4, 0xFF404040, false);

        if (menu.heat() > 0) g.text(font, Component.translatable(KEY + "heat", menu.heat()), COLUMN_X, 30, 0xFF404040, false);
        if (menu.temperature() > 0) {
            HeatBand band = HeatBand.of(menu.temperature());
            int colour = band == HeatBand.NONE ? 0xFF404040 : 0xFF000000 | band.colour();
            g.text(font, Component.translatable(KEY + "fire", menu.temperature()), COLUMN_X, 40, colour, false);
        }

        BoilerBlockEntity.Status status = menu.status();
        Component line = switch (status) {
            case HEATING -> Component.translatable(status.key(), menu.warmth());
            case TOO_COOL -> Component.translatable(status.key(), BoilerBlockEntity.MIN_TEMPERATURE, menu.temperature());
            case DRY_FIRING -> Component.translatable(status.key(), Math.round(menu.integrity()));
            default -> Component.translatable(status.key());
        };
        int colour = status.warning() ? 0xFF8A3A2A : 0xFF404040;
        int centre = (52 + 168) / 2;
        g.text(font, line, centre - font.width(line) / 2, STATUS_Y, colour, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (y >= TANK_Y && y < TANK_Y + TANK_H) {
            if (x >= WATER_X && x < WATER_X + TANK_W) {
                g.setTooltipForNextFrame(Component.translatable(KEY + "water", menu.water(), BoilerBlockEntity.WATER_CAPACITY), mouseX, mouseY);
            } else if (x >= STEAM_X && x < STEAM_X + TANK_W) {
                g.setTooltipForNextFrame(Component.translatable(KEY + "steam", menu.steam(), BoilerBlockEntity.STEAM_CAPACITY), mouseX, mouseY);
            }
        }
        if (x >= BAR_X - 1 && x < BAR_X + BAR_W + 1 && y >= BAR_Y - 1 && y < BAR_Y + BAR_H + 1) {
            g.setTooltipForNextFrame(Component.translatable(KEY + "integrity", Math.round(menu.integrity())), mouseX, mouseY);
        }
    }
}
