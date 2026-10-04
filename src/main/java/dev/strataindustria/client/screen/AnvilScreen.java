package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilMenu;
import dev.strataindustria.smithing.HitType;
import dev.strataindustria.smithing.Rule;
import dev.strataindustria.smithing.Smithing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Smithing: pick a plan, then hammer the workpiece along the bar until the green marker sits on the
 * red one with the last three hits matching the rules.
 */
public class AnvilScreen extends AbstractContainerScreen<AnvilMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/anvil.png");
    public static final int BAR_X = 13, BAR_Y = 61, BAR_W = 150;
    public static final int BUTTONS_Y = 74;
    public static final int[] BUTTON_X = {8, 28, 48, 68, 90, 110, 130, 150};
    public static final int RULES_X = 8, RECENT_X = 116, BOXES_Y = 106;
    public static final int STATUS_Y = 151;
    public static final int HEAT_X = 26, HEAT_Y = 27;
    // Sprite sheet, to the right of the panel.
    private static final int BUTTON_U = 176, BUTTON_V = 0, ICON_U = 176, ICON_V = 18, ANY_HIT_U = 240, ANY_HIT_V = 18;
    /** Weld button faces (normal, hovered, disabled), below the hit icons. */
    private static final int WELD_U = 176, WELD_V = 52;
    /** Quick-smith button faces (normal, hovered, disabled), below the weld button. */
    private static final int QUICK_U = 176, QUICK_V = 72;

    public AnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 252);
        this.inventoryLabelY = AnvilMenu.INVENTORY_Y - 11;
    }

    private int hoveredButton(double mouseX, double mouseY) {
        for (int i = 0; i < BUTTON_X.length; i++) {
            double x = mouseX - leftPos - BUTTON_X[i], y = mouseY - topPos - BUTTONS_Y;
            if (x >= 0 && x < 18 && y >= 0 && y < 18) return i;
        }
        return -1;
    }

    private boolean overWeld(double mouseX, double mouseY) {
        double x = mouseX - leftPos - AnvilMenu.WELD_BUTTON_X, y = mouseY - topPos - AnvilMenu.WELD_Y + 1;
        return x >= 0 && x < 18 && y >= 0 && y < 18;
    }

    private boolean overQuick(double mouseX, double mouseY) {
        double x = mouseX - leftPos - AnvilMenu.QUICK_BUTTON_X, y = mouseY - topPos - AnvilMenu.WELD_Y + 1;
        return x >= 0 && x < 18 && y >= 0 && y < 18;
    }

    private boolean quickReady() {
        return menu.status() == AnvilBlockEntity.Status.READY && menu.quickKnown();
    }

    private int hoveredPlan(double mouseX, double mouseY) {
        double x = mouseX - leftPos - AnvilMenu.PLANS_X, y = mouseY - topPos - AnvilMenu.PLANS_Y;
        if (x < 0 || y < 0) return -1;
        int col = (int) (x / 18), row = (int) (y / 18);
        if (col >= AnvilMenu.PLANS_PER_ROW || row * AnvilMenu.PLANS_PER_ROW + col >= AnvilBlockEntity.MAX_PLANS) return -1;
        return row * AnvilMenu.PLANS_PER_ROW + col;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && minecraft.gameMode != null) {
            int button = hoveredButton(event.x(), event.y());
            if (button >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
                return true;
            }
            if (overWeld(event.x(), event.y())) {
                if (menu.weldStatus() == AnvilBlockEntity.WeldStatus.READY) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AnvilMenu.WELD_BUTTON);
                } else if (minecraft.player != null) {
                    minecraft.player.playSound(dev.strataindustria.registry.ModSounds.ANVIL_WELD_FAIL.get(), 0.6f, 1.0f);
                }
                return true;
            }
            if (overQuick(event.x(), event.y())) {
                if (quickReady()) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AnvilMenu.QUICK_BUTTON);
                } else if (minecraft.player != null) {
                    minecraft.player.playSound(dev.strataindustria.registry.ModSounds.ANVIL_WELD_FAIL.get(), 0.6f, 1.0f);
                }
                return true;
            }
            int plan = hoveredPlan(event.x(), event.y());
            if (plan >= 0 && !menu.slots.get(AnvilBlockEntity.SLOTS + plan).getItem().isEmpty()) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AnvilMenu.PLAN_BUTTON + plan);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        // The chosen plan gets a lit frame.
        int selected = menu.selectedPlan();
        if (selected >= 0) {
            int x = leftPos + AnvilMenu.PLANS_X + (selected % AnvilMenu.PLANS_PER_ROW) * 18 - 1;
            int y = topPos + AnvilMenu.PLANS_Y + (selected / AnvilMenu.PLANS_PER_ROW) * 18 - 1;
            g.fill(x, y, x + 18, y + 1, 0xFFF0C040);
            g.fill(x, y + 17, x + 18, y + 18, 0xFFF0C040);
            g.fill(x, y, x + 1, y + 18, 0xFFF0C040);
            g.fill(x + 17, y, x + 18, y + 18, 0xFFF0C040);
        }

        // Workpiece heat: fills up to the working temperature in the band's colour.
        ItemStack piece = menu.workpiece();
        if (!piece.isEmpty() && minecraft.level != null && menu.workingTemperature() > 0) {
            float t = Heat.get(piece, minecraft.level);
            int h = Math.round(16 * Math.min(1.0f, t / menu.workingTemperature()));
            if (h > 0) {
                int bottom = topPos + HEAT_Y + 16;
                g.fill(leftPos + HEAT_X, bottom - h, leftPos + HEAT_X + 2, bottom, 0xFF000000 | HeatBand.of(t).colour());
            }
        }

        // Bar markers: the target below the bar in red, the workpiece above it in green.
        int target = menu.target();
        if (target >= 0) {
            int x = leftPos + BAR_X + Math.round(Smithing.barFraction(target) * (BAR_W - 1));
            g.fill(x, topPos + BAR_Y, x + 1, topPos + BAR_Y + 6, 0xFFD23A1E);
            triangle(g, x, topPos + BAR_Y + 7, false, 0xFFD23A1E);
        }
        if (!piece.isEmpty()) {
            int x = leftPos + BAR_X + Math.round(Smithing.barFraction(menu.position()) * (BAR_W - 1));
            g.fill(x, topPos + BAR_Y, x + 1, topPos + BAR_Y + 6, 0xFF4AC04A);
            triangle(g, x, topPos + BAR_Y - 2, true, 0xFF4AC04A);
        }

        boolean ready = menu.status() == AnvilBlockEntity.Status.READY;
        int hovered = hoveredButton(mouseX, mouseY);
        for (int i = 0; i < BUTTON_X.length; i++) {
            int u = BUTTON_U + (!ready ? 36 : i == hovered ? 18 : 0);
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + BUTTON_X[i], topPos + BUTTONS_Y, u, BUTTON_V, 18, 18, 256, 256);
            icon(g, HitType.VALUES[i], leftPos + BUTTON_X[i] + 1, topPos + BUTTONS_Y + 1);
        }

        boolean weldReady = menu.weldStatus() == AnvilBlockEntity.WeldStatus.READY;
        int weldU = WELD_U + (!weldReady ? 36 : overWeld(mouseX, mouseY) ? 18 : 0);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + AnvilMenu.WELD_BUTTON_X, topPos + AnvilMenu.WELD_Y - 1, weldU, WELD_V,
                18, 18, 256, 256);

        int quickU = QUICK_U + (!quickReady() ? 36 : overQuick(mouseX, mouseY) ? 18 : 0);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + AnvilMenu.QUICK_BUTTON_X, topPos + AnvilMenu.WELD_Y - 1, quickU, QUICK_V,
                18, 18, 256, 256);

        for (int i = 0; i < 3; i++) {
            Rule rule = menu.rule(i);
            int x = leftPos + RULES_X + i * 20, y = topPos + BOXES_Y;
            if (rule == null) continue;
            boolean met = rule.test(menu.recent(0), menu.recent(1), menu.recent(2));
            if (rule.hit() == Rule.Kind.HIT) {
                g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 1, y + 1, ANY_HIT_U, ANY_HIT_V, 16, 16, 256, 256);
            } else {
                icon(g, kindIcon(rule.hit()), x + 1, y + 1);
            }
            // Three pips: third last, second last, last. Lit where the hit has to land.
            for (int p = 0; p < 3; p++) {
                boolean wanted = switch (rule.where()) {
                    case LAST -> p == 2;
                    case SECOND_LAST -> p == 1;
                    case THIRD_LAST -> p == 0;
                    case NOT_LAST -> p < 2;
                    case ANY -> true;
                };
                int px = x + 2 + p * 5, py = y + 19;
                g.fill(px, py, px + 4, py + 3, wanted ? (met ? 0xFF4AC04A : 0xFFF0C040) : 0xFF555555);
            }
        }
        // The last three hits, oldest on the left so they read towards the newest.
        for (int i = 0; i < 3; i++) {
            HitType hit = menu.recent(2 - i);
            if (hit != null) icon(g, hit, leftPos + RECENT_X + i * 18 + 1, topPos + BOXES_Y + 1);
        }
    }

    private void icon(GuiGraphicsExtractor g, HitType type, int x, int y) {
        int i = type.ordinal();
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, ICON_U + (i % 4) * 16, ICON_V + (i / 4) * 16, 16, 16, 256, 256);
    }

    private static HitType kindIcon(Rule.Kind kind) {
        return switch (kind) {
            case HIT -> HitType.MEDIUM;
            case DRAW -> HitType.DRAW;
            case PUNCH -> HitType.PUNCH;
            case BEND -> HitType.BEND;
            case UPSET -> HitType.UPSET;
            case SHRINK -> HitType.SHRINK;
        };
    }

    /** A 5 px wide marker triangle with its tip at (x, y), pointing down or up. */
    private static void triangle(GuiGraphicsExtractor g, int x, int y, boolean down, int colour) {
        for (int row = 0; row < 3; row++) {
            int yy = down ? y - row : y + row;
            g.fill(x - row, yy, x + row + 1, yy + 1, colour);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, Component.translatable(StrataIndustria.MOD_ID + ".anvil.rules"), RULES_X, BOXES_Y - 10, 0xFF404040, false);
        g.text(font, Component.translatable(StrataIndustria.MOD_ID + ".anvil.recent"), RECENT_X, BOXES_Y - 10, 0xFF404040, false);
        g.text(font, Component.translatable(StrataIndustria.MOD_ID + ".anvil.pattern"), AnvilMenu.PATTERN_X - 4
                - font.width(Component.translatable(StrataIndustria.MOD_ID + ".anvil.pattern")), AnvilMenu.WELD_Y + 4, 0xFF404040, false);
        // While two pieces sit on the anvil the line speaks for the weld; otherwise for the smithing.
        AnvilBlockEntity.WeldStatus weld = menu.weldStatus();
        if (weld != AnvilBlockEntity.WeldStatus.NONE) {
            g.text(font, Component.translatable(weld.key(), menu.weldingTemperature()), 8, STATUS_Y,
                    weld == AnvilBlockEntity.WeldStatus.READY ? 0xFF404040 : 0xFF7A4A20, false);
            return;
        }
        AnvilBlockEntity.Status status = menu.status();
        Component line = status == AnvilBlockEntity.Status.READY
                ? Component.translatable(status.key(), menu.hits())
                : Component.translatable(status.key());
        g.text(font, line, 8, STATUS_Y, status == AnvilBlockEntity.Status.READY ? 0xFF404040 : 0xFF7A4A20, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (overWeld(mouseX, mouseY)) {
            AnvilBlockEntity.WeldStatus weld = menu.weldStatus();
            Component tip = weld == AnvilBlockEntity.WeldStatus.READY || weld == AnvilBlockEntity.WeldStatus.NONE
                    ? Component.translatable(StrataIndustria.MOD_ID + ".anvil.weld")
                    : Component.translatable(weld.key(), menu.weldingTemperature());
            g.setTooltipForNextFrame(tip, mouseX, mouseY);
            return;
        }
        if (overQuick(mouseX, mouseY)) {
            Component tip = menu.selectedPlan() >= 0 && !menu.quickKnown()
                    ? Component.translatable(StrataIndustria.MOD_ID + ".anvil.quick.unknown")
                    : Component.translatable(StrataIndustria.MOD_ID + ".anvil.quick.tip");
            g.setTooltipForNextFrame(tip, mouseX, mouseY);
            return;
        }
        int button = hoveredButton(mouseX, mouseY);
        if (button >= 0) {
            HitType hit = HitType.VALUES[button];
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".anvil.hit." + hit.id(),
                    (hit.delta() > 0 ? "+" : "") + hit.delta()), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < 3; i++) {
            Rule rule = menu.rule(i);
            int x = mouseX - leftPos - RULES_X - i * 20, y = mouseY - topPos - BOXES_Y;
            if (rule != null && x >= 0 && x < 18 && y >= 0 && y < 23) {
                g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".anvil.rule",
                        Component.translatable(StrataIndustria.MOD_ID + ".anvil.kind." + rule.hit().getSerializedName()),
                        Component.translatable(StrataIndustria.MOD_ID + ".anvil.where." + rule.where().getSerializedName())), mouseX, mouseY);
                return;
            }
        }
    }
}
