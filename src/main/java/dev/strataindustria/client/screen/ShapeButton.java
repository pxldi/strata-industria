package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.smithing.ShapeMachine;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The shape button of a hammer machine: a framed slot showing what the machine is making. Left click goes to
 * the next shape, right click to the one before. The frame itself is painted into the screen's background.
 */
final class ShapeButton {
    static final int SIZE = 16;
    private final int x, y;

    /** {@code x} and {@code y} are where the icon sits, like a slot's. */
    ShapeButton(int x, int y) {
        this.x = x;
        this.y = y;
    }

    boolean over(double mouseX, double mouseY, int left, int top) {
        double dx = mouseX - left - x, dy = mouseY - top - y;
        return dx >= 0 && dx < SIZE && dy >= 0 && dy < SIZE;
    }

    /** The icon, with the slot's own highlight while the pointer is on it. */
    void extract(GuiGraphicsExtractor g, int left, int top, ItemStack shape, int mouseX, int mouseY) {
        if (!shape.isEmpty()) g.item(shape, left + x, top + y);
        if (over(mouseX, mouseY, left, top)) g.fill(left + x, top + y, left + x + SIZE, top + y + SIZE, 0x80FFFFFF);
    }

    /** "Shape: Copper plate" and what the clicks do. */
    void tooltip(GuiGraphicsExtractor g, Font font, ItemStack shape, int mouseX, int mouseY) {
        Component name = shape.isEmpty() ? Component.translatable(StrataIndustria.MOD_ID + ".machine.shape.none") : shape.getHoverName();
        g.setTooltipForNextFrame(font, List.of(
                Component.translatable(StrataIndustria.MOD_ID + ".machine.shape", name),
                Component.translatable(StrataIndustria.MOD_ID + ".machine.shape.hint").withStyle(ChatFormatting.GRAY)),
                Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
    }

    /** Sends the click to the server when it is on the button. Returns whether it was. */
    boolean click(MouseButtonEvent event, int menuId, int left, int top) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!over(event.x(), event.y(), left, top) || minecraft.gameMode == null) return false;
        boolean back = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        if (!back && event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        minecraft.gameMode.handleInventoryButtonClick(menuId, back ? ShapeMachine.BUTTON_PREVIOUS : ShapeMachine.BUTTON_NEXT);
        return true;
    }
}
