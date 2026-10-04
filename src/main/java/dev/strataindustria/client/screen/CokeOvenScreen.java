package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.coking.CokeOvenBlockEntity;
import dev.strataindustria.coking.CokeOvenMenu;
import dev.strataindustria.coking.CokeOvenStructure;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Coke oven: the charge, the bake, and the creosote tank with its bucket slots. */
public class CokeOvenScreen extends AbstractContainerScreen<CokeOvenMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/coke_oven.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".coke_oven.";
    /** The tank's inside and the creosote fill strip in the texture. */
    public static final int TANK_X = 130, TANK_Y = 17, TANK_W = 16, TANK_H = 52, FLUID_U = 176, FLUID_V = 0;
    /** The flame that burns down while a charge bakes, between input and output. */
    public static final int FLAME_X = 63, FLAME_Y = 27, FLAME_W = 14, FLAME_H = 14, FLAME_U = 192, FLAME_V = 0;
    public static final int STATUS_Y = 50;

    public CokeOvenScreen(CokeOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = CokeOvenMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int amount = menu.creosote();
        if (amount > 0) {
            int h = Math.max(1, Math.round(TANK_H * Math.min(1.0f, amount / (float) CokeOvenBlockEntity.CAPACITY)));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + TANK_X, topPos + TANK_Y + TANK_H - h,
                    FLUID_U, FLUID_V + TANK_H - h, TANK_W, h, 256, 256);
        }
        if (menu.status() == CokeOvenBlockEntity.Status.WORKING) {
            // The flame shrinks from the top as the bake runs.
            int h = Math.max(1, Math.round(FLAME_H * (1 - menu.progress())));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + FLAME_X, topPos + FLAME_Y + FLAME_H - h,
                    FLAME_U, FLAME_V + FLAME_H - h, FLAME_W, h, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        CokeOvenBlockEntity.Status status = menu.status();
        Component line = status == CokeOvenBlockEntity.Status.WORKING
                ? Component.translatable(status.key(), Math.round(menu.progress() * 100))
                : Component.translatable(status.key());
        int colour = switch (status) {
            case WORKING, EMPTY -> 0xFF404040;
            default -> 0xFF8A3A2A;
        };
        int centre = (8 + TANK_X) / 2;
        g.text(font, line, centre - font.width(line) / 2, STATUS_Y, colour, false);
        if (status == CokeOvenBlockEntity.Status.INCOMPLETE) {
            BlockPos at = menu.problemOffset();
            Component what = Component.translatable(KEY + "problem." + (menu.problem() == CokeOvenStructure.Problem.NEEDS_AIR ? "air" : "brick"),
                    at.getX(), at.getY(), at.getZ());
            g.text(font, what, centre - font.width(what) / 2, STATUS_Y + 10, 0xFF707070, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= TANK_X && x < TANK_X + TANK_W && y >= TANK_Y && y < TANK_Y + TANK_H) {
            Component text = menu.creosote() <= 0
                    ? Component.translatable(KEY + "tank_empty")
                    : Component.translatable(KEY + "tank", menu.creosote(), CokeOvenBlockEntity.CAPACITY);
            g.setTooltipForNextFrame(text, mouseX, mouseY);
        }
    }
}
