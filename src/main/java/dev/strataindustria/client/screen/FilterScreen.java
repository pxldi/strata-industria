package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.automation.FilterContents;
import dev.strataindustria.automation.FilterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The filter: a 3 x 3 grid of ghost entries, and buttons for whitelist or blacklist and for ore grade.
 * Left-click an entry with an item to set it or with an empty hand to clear it; right-click steps
 * through the item's tags.
 */
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/filter.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".filter.";
    /** The tag badge drawn over an entry that matches a tag. */
    private static final int BADGE_U = 176, BADGE_V = 0;
    private static final int BUTTON_X = 122, BUTTON_W = 46;

    private Button mode;
    private Button grade;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = FilterMenu.INVENTORY_Y - 11;
    }

    private FilterContents contents() {
        return minecraft.player == null ? FilterContents.EMPTY : FilterContents.of(minecraft.player.getItemInHand(menu.hand()));
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(modeLabel(), b -> click(FilterMenu.BUTTON_WHITELIST))
                .bounds(leftPos + BUTTON_X, topPos + 17, BUTTON_W, 16).build());
        grade = addRenderableWidget(Button.builder(gradeLabel(), b -> click(FilterMenu.BUTTON_GRADE))
                .bounds(leftPos + BUTTON_X, topPos + 37, BUTTON_W, 16).build());
    }

    private Component modeLabel() {
        return Component.translatable(KEY + (contents().whitelist() ? "allow" : "deny"));
    }

    private Component gradeLabel() {
        return Component.translatable(KEY + (contents().matchGrade() ? "any_grade" : "exact_grade"));
    }

    private void click(int button) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (mode != null) mode.setMessage(modeLabel());
        if (grade != null) grade.setMessage(gradeLabel());
    }

    private int hoveredEntry(double mouseX, double mouseY) {
        int col = (int) Math.floor((mouseX - leftPos - FilterMenu.GRID_X + 1) / 18);
        int row = (int) Math.floor((mouseY - topPos - FilterMenu.GRID_Y + 1) / 18);
        if (col < 0 || row < 0 || col > 2 || row > 2) return -1;
        return row * 3 + col;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int entry = hoveredEntry(event.x(), event.y());
        if (entry >= 0) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) click(entry);
            else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) click(FilterMenu.BUTTON_TAG + entry);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        FilterContents contents = contents();
        for (int i = 0; i < FilterContents.SIZE; i++) {
            if (contents.item(i) == Items.AIR) continue;
            int x = leftPos + FilterMenu.GRID_X + (i % 3) * 18, y = topPos + FilterMenu.GRID_Y + (i / 3) * 18;
            g.item(new ItemStack(contents.item(i)), x, y);
            if (!contents.tag(i).isEmpty()) g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, BADGE_U, BADGE_V, 16, 16, 256, 256);
        }
        int hovered = hoveredEntry(mouseX, mouseY);
        if (hovered >= 0) {
            int x = leftPos + FilterMenu.GRID_X + (hovered % 3) * 18, y = topPos + FilterMenu.GRID_Y + (hovered / 3) * 18;
            g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int entry = hoveredEntry(mouseX, mouseY);
        if (entry < 0) return;
        FilterContents contents = contents();
        Component text;
        if (contents.item(entry) == Items.AIR) {
            text = Component.translatable(KEY + "set_hint");
        } else if (contents.tag(entry).isEmpty()) {
            text = Component.translatable(KEY + "item_hint", new ItemStack(contents.item(entry)).getHoverName());
        } else {
            text = Component.translatable(KEY + "tag_entry", contents.tag(entry));
        }
        g.setTooltipForNextFrame(text, mouseX, mouseY);
    }
}
