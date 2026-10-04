package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ironworks.BlastFurnaceBlockEntity;
import dev.strataindustria.ironworks.BlastFurnaceMenu;
import dev.strataindustria.ironworks.BlastFurnaceStructure;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Blast furnace: burden buffers, the hearth's heat, the tap, and what stops it. */
public class BlastFurnaceScreen extends AbstractContainerScreen<BlastFurnaceMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/blast_furnace.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".blast_furnace.";
    /** The three buffer gauges, one beside each charge slot, and their fills in the texture. */
    public static final int GAUGE_X = 30, GAUGE_W = 50, GAUGE_H = 6, FILL_U = 176;
    private static final int[] GAUGE_Y = {23, 43, 63};
    private static final int[] FILL_V = {0, 6, 12};
    /** The hearth's heat, filling from the bottom. */
    public static final int HEARTH_X = 88, HEARTH_Y = 18, HEARTH_W = 10, HEARTH_H = 56, HEARTH_U = 176, HEARTH_V = 18;
    public static final int ARROW_X = 104, ARROW_Y = 36, ARROW_W = 22, ARROW_H = 15, ARROW_U = 188, ARROW_V = 18;
    public static final int STATUS_Y = 79;

    public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 190);
        this.inventoryLabelY = BlastFurnaceMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        float[] fills = {
                menu.iron() / (float) BlastFurnaceBlockEntity.MAX_IRON,
                menu.fuel() / (float) BlastFurnaceBlockEntity.MAX_FUEL,
                menu.flux() / (float) BlastFurnaceBlockEntity.MAX_FLUX};
        for (int i = 0; i < 3; i++) {
            if (fills[i] <= 0) continue;
            int w = Math.max(1, Math.round(GAUGE_W * Math.min(1.0f, fills[i])));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + GAUGE_X, topPos + GAUGE_Y[i], FILL_U, FILL_V[i], w, GAUGE_H, 256, 256);
        }
        float warmth = menu.warmth();
        if (warmth > 0) {
            int h = Math.max(1, Math.round(HEARTH_H * warmth));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + HEARTH_X, topPos + HEARTH_Y + HEARTH_H - h,
                    HEARTH_U, HEARTH_V + HEARTH_H - h, HEARTH_W, h, 256, 256);
        }
        if (menu.status() == BlastFurnaceBlockEntity.Status.RUNNING) {
            int w = Math.round(ARROW_W * menu.progress());
            if (w > 0) g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, w, ARROW_H, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        BlastFurnaceBlockEntity.Status status = menu.status();
        Component line = switch (status) {
            case HEATING -> Component.translatable(status.key(), Math.round(menu.warmth() * 100));
            case RUNNING -> Component.translatable(status.key(), menu.air());
            default -> Component.translatable(status.key());
        };
        int colour = status == BlastFurnaceBlockEntity.Status.RUNNING || status == BlastFurnaceBlockEntity.Status.HEATING ? 0xFF404040 : 0xFF8A3A2A;
        g.text(font, line, imageWidth / 2 - font.width(line) / 2, STATUS_Y, colour, false);
        if (status == BlastFurnaceBlockEntity.Status.INCOMPLETE) {
            BlastFurnaceStructure.Problem problem = menu.problem();
            Component spot = Component.translatable(KEY + "spot." + menu.problemSpot());
            Component what = Component.translatable(problem.key(), menu.problemLayer(), spot);
            g.text(font, what, imageWidth / 2 - font.width(what) / 2, STATUS_Y + 10, 0xFF707070, false);
        } else if (menu.inlets() > 0) {
            int need = BlastFurnaceBlockEntity.HOT_BLAST_TEMPERATURE;
            Component hot = HeatLine.line("hot_blast", need, menu.hotTemperature(), menu.hotHeat(), menu.hotLimit());
            g.text(font, hot, imageWidth / 2 - font.width(hot) / 2, STATUS_Y + 10, HeatLine.colour(need, menu.hotTemperature()), false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (menu.inlets() > 0 && y >= STATUS_Y + 9 && y < STATUS_Y + 19 && x >= 8 && x < imageWidth - 8) {
            g.setTooltipForNextFrame(HeatLine.tooltip(BlastFurnaceBlockEntity.HOT_BLAST_TEMPERATURE, menu.hotTemperature(), menu.hotLimit()), mouseX, mouseY);
        }
        if (x >= GAUGE_X && x < GAUGE_X + GAUGE_W) {
            for (int i = 0; i < 3; i++) {
                if (y < GAUGE_Y[i] - 1 || y > GAUGE_Y[i] + GAUGE_H) continue;
                Component text = switch (i) {
                    case 0 -> Component.translatable(KEY + "iron", menu.iron(), BlastFurnaceBlockEntity.MAX_IRON);
                    case 1 -> Component.translatable(KEY + "fuel", quarters(menu.fuel()), BlastFurnaceBlockEntity.MAX_FUEL / 4);
                    default -> Component.translatable(KEY + "flux", halves(menu.flux()), BlastFurnaceBlockEntity.MAX_FLUX / 2);
                };
                g.setTooltipForNextFrame(text, mouseX, mouseY);
            }
        }
        if (x >= HEARTH_X && x < HEARTH_X + HEARTH_W && y >= HEARTH_Y && y < HEARTH_Y + HEARTH_H) {
            g.setTooltipForNextFrame(Component.translatable(KEY + "hearth", Math.round(menu.warmth() * 100)), mouseX, mouseY);
        }
    }

    private static String quarters(int amount) {
        return amount % 4 == 0 ? Integer.toString(amount / 4) : String.format(java.util.Locale.ROOT, "%.2f", amount / 4.0f);
    }

    private static String halves(int amount) {
        return amount % 2 == 0 ? Integer.toString(amount / 2) : amount / 2 + ".5";
    }
}
