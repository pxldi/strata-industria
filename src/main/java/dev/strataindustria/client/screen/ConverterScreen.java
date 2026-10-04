package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ironworks.BlastFurnaceStructure;
import dev.strataindustria.ironworks.ConverterBlockEntity;
import dev.strataindustria.ironworks.ConverterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Converter: the charge, the blow's flame rising and dropping, and the steel it gives. */
public class ConverterScreen extends AbstractContainerScreen<ConverterMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/converter.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".converter.";
    /** What is in the vessel during a blow: pig iron, scrap, and the preheat coke. */
    public static final int COUNT_X = 30, COUNT_W = 30, COUNT_H = 6, COUNT_U = 192;
    private static final int[] COUNT_Y = {23, 43, 63};
    private static final int[] COUNT_V = {16, 22, 28};
    public static final int FLAME_X = 80, FLAME_Y = 18, FLAME_W = 14, FLAME_H = 56, FLAME_U = 176, FLAME_V = 0;
    public static final int ARROW_X = 104, ARROW_Y = 36, ARROW_W = 22, ARROW_H = 15, ARROW_U = 192, ARROW_V = 0;
    public static final int STATUS_Y = 79;

    public ConverterScreen(ConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 190);
        this.inventoryLabelY = ConverterMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        boolean blowing = menu.chargePig() > 0;
        float[] fills = {
                menu.chargePig() / (float) ConverterBlockEntity.MAX_PIG_IRON,
                menu.chargeScrap() / (float) ConverterBlockEntity.MAX_SCRAP,
                blowing ? 1 : 0};
        for (int i = 0; i < 3; i++) {
            if (fills[i] <= 0) continue;
            int w = Math.max(1, Math.round(COUNT_W * Math.min(1.0f, fills[i])));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + COUNT_X, topPos + COUNT_Y[i], COUNT_U, COUNT_V[i], w, COUNT_H, 256, 256);
        }
        float flame = menu.flame();
        if (flame > 0) {
            int h = Math.max(1, Math.round(FLAME_H * flame));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + FLAME_X, topPos + FLAME_Y + FLAME_H - h,
                    FLAME_U, FLAME_V + FLAME_H - h, FLAME_W, h, 256, 256);
        }
        if (blowing) {
            int w = Math.round(ARROW_W * menu.progress());
            if (w > 0) g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, w, ARROW_H, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        ConverterBlockEntity.Status status = menu.status();
        Component line = status == ConverterBlockEntity.Status.BLOWING
                ? Component.translatable(status.key(), Math.round(menu.progress() * 100))
                : Component.translatable(status.key());
        boolean fine = status == ConverterBlockEntity.Status.BLOWING || status == ConverterBlockEntity.Status.CHARGING
                || status == ConverterBlockEntity.Status.EMPTY;
        g.text(font, line, imageWidth / 2 - font.width(line) / 2, STATUS_Y, fine ? 0xFF404040 : 0xFF8A3A2A, false);
        if (status == ConverterBlockEntity.Status.INCOMPLETE) {
            BlastFurnaceStructure.Problem problem = menu.problem();
            Component spot = Component.translatable(StrataIndustria.MOD_ID + ".blast_furnace.spot." + menu.problemSpot());
            Component what = Component.translatable(problem.key(), menu.problemLayer(), spot);
            g.text(font, what, imageWidth / 2 - font.width(what) / 2, STATUS_Y + 10, 0xFF707070, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= COUNT_X && x < COUNT_X + COUNT_W) {
            for (int i = 0; i < 3; i++) {
                if (y < COUNT_Y[i] - 1 || y > COUNT_Y[i] + COUNT_H) continue;
                Component text = switch (i) {
                    case 0 -> Component.translatable(KEY + "charge_pig", menu.chargePig(), ConverterBlockEntity.MAX_PIG_IRON);
                    case 1 -> Component.translatable(KEY + "charge_scrap", menu.chargeScrap(), ConverterBlockEntity.MAX_SCRAP);
                    default -> Component.translatable(menu.chargePig() > 0 ? KEY + "preheated" : KEY + "not_preheated");
                };
                g.setTooltipForNextFrame(text, mouseX, mouseY);
            }
        }
        if (x >= FLAME_X && x < FLAME_X + FLAME_W && y >= FLAME_Y && y < FLAME_Y + FLAME_H) {
            g.setTooltipForNextFrame(Component.translatable(KEY + "flame"), mouseX, mouseY);
        }
    }
}
