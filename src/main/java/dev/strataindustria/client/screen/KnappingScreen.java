package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.knapping.GridPattern;
import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.knapping.KnappingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;

/** The knapping grid: click a cell to strike it out. Chips of stone fly off each strike. */
public class KnappingScreen extends AbstractContainerScreen<KnappingMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/knapping.png");
    private static final int CELL = KnappingMenu.CELL;
    /** Repeat-last button faces (normal, hovered, disabled) at the top of the sprite area right of the panel. */
    private static final int REPEAT_U = 176, REPEAT_V = 0;

    private final RandomSource random = RandomSource.create();
    private final List<Chip> chips = new ArrayList<>();

    public KnappingScreen(KnappingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 196);
        this.inventoryLabelY = KnappingMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        Identifier stone = Knapping.gridTexture(menu.material());
        int hovered = hoveredCell(mouseX, mouseY);
        for (int cell = 0; cell < GridPattern.CELLS; cell++) {
            if (!menu.isKept(cell)) continue;
            int cx = cell % GridPattern.SIZE, cy = cell / GridPattern.SIZE;
            int x = cellX(cx), y = cellY(cy);
            g.blit(RenderPipelines.GUI_TEXTURED, stone, x, y, 0, 0, CELL, CELL, 16, 16);
            // Shade the faces that border a struck cell, so the worked edge reads as depth.
            if (cy < GridPattern.SIZE - 1 && !menu.isKept(cell + GridPattern.SIZE)) g.fill(x, y + CELL - 2, x + CELL, y + CELL, 0x70000000);
            if (cx < GridPattern.SIZE - 1 && !menu.isKept(cell + 1)) g.fill(x + CELL - 2, y, x + CELL, y + CELL, 0x50000000);
            if (cy > 0 && !menu.isKept(cell - GridPattern.SIZE)) g.fill(x, y, x + CELL, y + 1, 0x40FFFFFF);
            if (cx > 0 && !menu.isKept(cell - 1)) g.fill(x, y, x + 1, y + CELL, 0x30FFFFFF);
            if (cell == hovered && !menu.isFinished()) g.fill(x, y, x + CELL, y + CELL, 0x40FFFFFF);
            // Hovering the repeat button shows which cells it will strike out.
            if (repeatReady() && overRepeat(mouseX, mouseY) && (menu.repeatMask() >> cell & 1) == 0) g.fill(x, y, x + CELL, y + CELL, 0x80C03020);
        }

        if (hasRepeat()) {
            int u = REPEAT_U + (!repeatReady() ? 36 : overRepeat(mouseX, mouseY) ? 18 : 0);
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + KnappingMenu.REPEAT_X, topPos + KnappingMenu.REPEAT_Y, u, REPEAT_V, 18, 18, 256, 256);
        }

        for (Chip chip : chips) {
            int x = Math.round(chip.x), y = Math.round(chip.y);
            g.fill(x, y, x + chip.size, y + chip.size, chip.colour);
        }
    }

    /** Clay and wood have the pattern casting; only stone and flint get a repeat button. */
    private boolean hasRepeat() {
        return !Knapping.isClay(menu.material()) && !Knapping.isWood(menu.material());
    }

    private boolean repeatReady() {
        return hasRepeat() && menu.repeatMask() != 0 && !menu.hasStarted() && !menu.isFinished();
    }

    private boolean overRepeat(double mouseX, double mouseY) {
        double x = mouseX - leftPos - KnappingMenu.REPEAT_X, y = mouseY - topPos - KnappingMenu.REPEAT_Y;
        return hasRepeat() && x >= 0 && x < 18 && y >= 0 && y < 18;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (overRepeat(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + (menu.repeatMask() != 0 ? ".knapping.repeat" : ".knapping.repeat.unknown")),
                    mouseX, mouseY);
        }
    }

    private int cellX(int cx) {
        return leftPos + KnappingMenu.GRID_X + cx * CELL;
    }

    private int cellY(int cy) {
        return topPos + KnappingMenu.GRID_Y + cy * CELL;
    }

    private int hoveredCell(double mouseX, double mouseY) {
        int gx = (int) Math.floor((mouseX - leftPos - KnappingMenu.GRID_X) / CELL);
        int gy = (int) Math.floor((mouseY - topPos - KnappingMenu.GRID_Y) / CELL);
        if (gx < 0 || gy < 0 || gx >= GridPattern.SIZE || gy >= GridPattern.SIZE) return -1;
        return gy * GridPattern.SIZE + gx;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cell = hoveredCell(event.x(), event.y());
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overRepeat(event.x(), event.y())) {
            if (repeatReady() && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, KnappingMenu.REPEAT_BUTTON);
            }
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && cell >= 0) {
            if (menu.isKept(cell) && !menu.isFinished() && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, cell);
                spawnChips(cell);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void spawnChips(int cell) {
        int cx = cell % GridPattern.SIZE, cy = cell / GridPattern.SIZE;
        float centreX = cellX(cx) + CELL / 2f, centreY = cellY(cy) + CELL / 2f;
        for (int i = 0; i < 7; i++) {
            float shade = 0.55f + random.nextFloat() * 0.35f;
            int colour = Knapping.chipColour(menu.material(), shade);
            chips.add(new Chip(centreX, centreY, (random.nextFloat() - 0.5f) * 3.2f, -1.0f - random.nextFloat() * 2.2f,
                    1 + random.nextInt(2), colour, 10 + random.nextInt(8)));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        chips.removeIf(Chip::tick);
        if (menu.isFinished()) onClose();
    }

    private static final class Chip {
        float x, y, vx, vy;
        final int size, colour;
        int life;

        Chip(float x, float y, float vx, float vy, int size, int colour, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.colour = colour;
            this.life = life;
        }

        /** Advances one tick; returns true when the chip is gone. */
        boolean tick() {
            x += vx;
            y += vy;
            vy += 0.35f;
            vx *= 0.92f;
            return --life <= 0;
        }
    }
}
