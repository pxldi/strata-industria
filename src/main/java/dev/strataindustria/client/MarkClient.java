package dev.strataindustria.client;

import com.mojang.datafixers.util.Either;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.mark.MakerStamp;
import dev.strataindustria.mark.MakersMark;
import dev.strataindustria.mark.MarkRegistry;
import dev.strataindustria.mark.MarkTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** Draws a stamped part's mark in its tooltip, under the "made by" line. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class MarkClient {
    private MarkClient() {}

    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MarkTooltip.class, tooltip -> new Impression(tooltip.mark()));
    }

    @SubscribeEvent
    static void onGather(RenderTooltipEvent.GatherComponents event) {
        MakerStamp stamp = event.getItemStack().get(MarkRegistry.STAMP.get());
        if (stamp == null) return;
        String madeBy = Component.translatable(StrataIndustria.MOD_ID + ".mark.made_by", stamp.maker()).getString();
        var elements = event.getTooltipElements();
        int at = elements.size();
        for (int i = 0; i < elements.size(); i++) {
            Either<FormattedText, TooltipComponent> element = elements.get(i);
            if (element.left().filter(text -> text.getString().equals(madeBy)).isPresent()) {
                at = i + 1;
                break;
            }
        }
        elements.add(at, Either.right(new MarkTooltip(stamp.mark())));
    }

    /** The stamp as it looks struck into metal: dark cells on a pale patch. */
    private record Impression(MakersMark mark) implements ClientTooltipComponent {
        private static final int CELL = 2, PAD = 1, SIZE = MakersMark.SIZE * CELL + PAD * 2;

        @Override
        public int getHeight(Font font) {
            return SIZE + 2;
        }

        @Override
        public int getWidth(Font font) {
            return SIZE;
        }

        @Override
        public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor g) {
            g.fill(x, y, x + SIZE, y + SIZE, 0xFFB9B3A8);
            for (int cell = 0; cell < MakersMark.CELLS; cell++) {
                if (!mark.get(cell)) continue;
                int cx = x + PAD + cell % MakersMark.SIZE * CELL, cy = y + PAD + cell / MakersMark.SIZE * CELL;
                g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF2A2630);
            }
        }
    }
}
