package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.logistics.StorageControllerMenu;
import dev.strataindustria.logistics.StorageEntry;
import dev.strataindustria.logistics.StoragePayloads;
import dev.strataindustria.logistics.Tier5Logistics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The storage controller's screen (tier 5 spec 12.3): a 9 x 6 scrolling grid of everything stored with counts, a search
 * box (a name, {@code #tag} or {@code @modid}), sorting by name or count, and the player's inventory. Click a kind to take
 * a stack (right-click for half), shift-click to take it straight into the inventory, click with something on the cursor
 * to store it. Without power the list can be read but not used.
 */
public class StorageControllerScreen extends AbstractContainerScreen<StorageControllerMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/storage_controller.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".storage_controller.";
    private static final int COLS = 9, ROWS = 6, SLOT = 18;
    private static final int GRID_X = 16, GRID_Y = 32;
    private static final int BAR_X = 180, BAR_Y = 32, BAR_W = 8, BAR_H = ROWS * SLOT, HANDLE_H = 15;
    private static final int SEARCH_X = 16, SEARCH_Y = 18, SEARCH_W = 112, SEARCH_H = 12;
    private static final int SORT_Y = 18, SORT_W = 22, SORT_H = 12, SORT_NAME_X = 130, SORT_COUNT_X = 154;
    /** Where the sort button faces sit in the texture: name, name pressed, count, count pressed. */
    private static final int SORT_V = 236;
    private static final long TICK_GAP = 100;

    private EditBox search;
    private boolean byCount;
    private boolean descending;
    private int scroll;
    private boolean dragging;
    private List<StorageEntry> source = List.of();
    private String lastQuery = "";
    private boolean lastByCount, lastDescending;
    private List<StorageEntry> view = List.of();
    private long lastSound;

    public StorageControllerScreen(StorageControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 234);
        this.titleLabelX = 16;
        this.inventoryLabelX = 16;
        this.inventoryLabelY = StorageControllerMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + SEARCH_X + 3, topPos + SEARCH_Y + 2, SEARCH_W - 6, SEARCH_H - 3, Component.translatable(KEY + "search"));
        search.setBordered(false);
        search.setMaxLength(40);
        search.setHint(Component.translatable(KEY + "search_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        search.setResponder(text -> scroll = 0);
        addRenderableWidget(search);
    }

    // ------------------------------------------------------------------ the list

    private void refresh() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (source == menu.entries() && query.equals(lastQuery) && byCount == lastByCount && descending == lastDescending) return;
        source = menu.entries();
        lastQuery = query;
        lastByCount = byCount;
        lastDescending = descending;
        List<StorageEntry> list = new ArrayList<>();
        for (StorageEntry entry : source) if (matches(entry.stack(), query)) list.add(entry);
        Comparator<StorageEntry> order = byCount
                ? Comparator.comparingLong(StorageEntry::count).thenComparing(e -> name(e.stack()))
                : Comparator.comparing(e -> name(e.stack()));
        list.sort(descending ? order.reversed() : order);
        view = list;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private static String name(ItemStack stack) {
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(ItemStack stack, String query) {
        if (query.isEmpty()) return true;
        if (query.startsWith("#")) {
            String tag = query.substring(1);
            return stack.typeHolder().tags().anyMatch(key -> key.location().toString().contains(tag));
        }
        if (query.startsWith("@")) return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(query.substring(1));
        return name(stack).contains(query);
    }

    private int maxScroll() {
        return Math.max(0, (view.size() + COLS - 1) / COLS - ROWS);
    }

    private StorageEntry entryAt(double mouseX, double mouseY) {
        int col = (int) Math.floor((mouseX - leftPos - GRID_X) / SLOT);
        int row = (int) Math.floor((mouseY - topPos - GRID_Y) / SLOT);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return null;
        int index = (scroll + row) * COLS + col;
        return index < view.size() ? view.get(index) : null;
    }

    private boolean overGrid(double mouseX, double mouseY) {
        return mouseX >= leftPos + GRID_X && mouseX < leftPos + GRID_X + COLS * SLOT && mouseY >= topPos + GRID_Y && mouseY < topPos + GRID_Y + ROWS * SLOT;
    }

    private boolean overBar(double mouseX, double mouseY) {
        return mouseX >= leftPos + BAR_X && mouseX < leftPos + BAR_X + BAR_W && mouseY >= topPos + BAR_Y && mouseY < topPos + BAR_Y + BAR_H;
    }

    private boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    /** "1 234" fits a slot as "1.2k"; stacks of 64 and under show as they are. */
    private static String shortCount(long count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1_000_000) return count < 10_000 ? String.format(Locale.ROOT, "%.1fk", count / 1000.0) : (count / 1000) + "k";
        return count < 10_000_000 ? String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0) : (count / 1_000_000) + "M";
    }

    // ------------------------------------------------------------------ input

    private void send(ItemStack stack, int button, boolean shift) {
        ClientPacketDistributor.sendToServer(new StoragePayloads.Click(menu.containerId, stack, button, shift));
        long now = System.currentTimeMillis();
        if (now - lastSound > TICK_GAP && minecraft != null) {
            lastSound = now;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Tier5Logistics.CONTROLLER_STORE.get(), 1.6f, 0.3f));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (over(x, y, SORT_NAME_X, SORT_Y, SORT_W, SORT_H)) return sort(false);
            if (over(x, y, SORT_COUNT_X, SORT_Y, SORT_W, SORT_H)) return sort(true);
            if (overBar(x, y)) {
                dragging = true;
                dragTo(y);
                return true;
            }
        }
        if (overGrid(x, y) && menu.powered() && (event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == InputConstants.MOUSE_BUTTON_RIGHT)) {
            StorageEntry entry = entryAt(x, y);
            boolean carrying = !menu.getCarried().isEmpty();
            if (carrying) send(ItemStack.EMPTY, event.button(), false);
            else if (entry != null) send(entry.stack(), event.button(), event.hasShiftDown());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean sort(boolean count) {
        if (byCount == count) descending = !descending;
        else {
            byCount = count;
            descending = count;
        }
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
    }

    private void dragTo(double mouseY) {
        int max = maxScroll();
        if (max <= 0) return;
        double fraction = (mouseY - topPos - BAR_Y - HANDLE_H / 2.0) / (BAR_H - HANDLE_H);
        scroll = Math.max(0, Math.min(max, (int) Math.round(fraction * max)));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            dragTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (overGrid(x, y) || overBar(x, y)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    /** Typing goes to the search box, so the inventory key does not close the screen while it is focused. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search != null && search.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
            return search.keyPressed(event) || true;
        }
        return super.keyPressed(event);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        refresh();
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        drawSort(g, mouseX, mouseY, SORT_NAME_X, 0, !byCount);
        drawSort(g, mouseX, mouseY, SORT_COUNT_X, 2, byCount);
        int shown = scroll * COLS;
        for (int i = 0; i < COLS * ROWS && shown + i < view.size(); i++) {
            StorageEntry entry = view.get(shown + i);
            int x = leftPos + GRID_X + (i % COLS) * SLOT + 1, y = topPos + GRID_Y + (i / COLS) * SLOT + 1;
            g.item(entry.stack(), x, y);
            g.itemDecorations(font, entry.stack(), x, y, entry.count() > 1 ? shortCount(entry.count()) : null);
        }
        StorageEntry hovered = entryAt(mouseX, mouseY);
        if (hovered != null) {
            int index = view.indexOf(hovered) - shown;
            int x = leftPos + GRID_X + (index % COLS) * SLOT + 1, y = topPos + GRID_Y + (index / COLS) * SLOT + 1;
            g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
        int max = maxScroll();
        int handleY = max == 0 ? 0 : (int) Math.round((BAR_H - HANDLE_H) * (scroll / (double) max));
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + BAR_X, topPos + BAR_Y + handleY, max == 0 ? 202 : 194, 0, BAR_W, HANDLE_H, 256, 256);
        if (!menu.powered()) {
            g.fill(leftPos + GRID_X, topPos + GRID_Y, leftPos + GRID_X + COLS * SLOT, topPos + GRID_Y + ROWS * SLOT, 0x90101010);
            g.centeredText(font, Component.translatable(KEY + "no_power"), leftPos + GRID_X + COLS * SLOT / 2, topPos + GRID_Y + ROWS * SLOT / 2 - 4, 0xFFE05040);
        } else if (view.isEmpty()) {
            Component empty = Component.translatable(KEY + (source.isEmpty() ? "empty" : "no_match"));
            g.centeredText(font, empty, leftPos + GRID_X + COLS * SLOT / 2, topPos + GRID_Y + ROWS * SLOT / 2 - 4, 0xFF8A8A8A);
        }
    }

    private void drawSort(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int face, boolean active) {
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + x, topPos + SORT_Y, (face + (active ? 1 : 0)) * SORT_W, SORT_V, SORT_W, SORT_H, 256, 256);
        if (over(mouseX, mouseY, x, SORT_Y, SORT_W, SORT_H)) g.fill(leftPos + x, topPos + SORT_Y, leftPos + x + SORT_W, topPos + SORT_Y + SORT_H, 0x30FFFFFF);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        Component inventories = Component.translatable(KEY + "inventories", menu.inventories());
        g.text(font, inventories, imageWidth - 16 - font.width(inventories), titleLabelY, -12566464, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        StorageEntry entry = entryAt(mouseX, mouseY);
        if (entry != null && menu.getCarried().isEmpty()) {
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(entry.stack()));
            lines.add(Component.translatable(KEY + "stored", String.format(Locale.ROOT, "%,d", entry.count()).replace(',', ' ')).withStyle(net.minecraft.ChatFormatting.GRAY));
            g.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), entry.stack(), mouseX, mouseY);
        } else if (over(mouseX, mouseY, SORT_NAME_X, SORT_Y, SORT_W, SORT_H)) {
            g.setTooltipForNextFrame(Component.translatable(KEY + "sort_name"), mouseX, mouseY);
        } else if (over(mouseX, mouseY, SORT_COUNT_X, SORT_Y, SORT_W, SORT_H)) {
            g.setTooltipForNextFrame(Component.translatable(KEY + "sort_count"), mouseX, mouseY);
        }
    }
}
