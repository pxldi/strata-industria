package dev.strataindustria.client;

import dev.strataindustria.heat.HeatBand;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Heat for screens and status lines: the glow word, with the number after it only when advanced tooltips (F3+H) are on. */
public final class HeatWords {
    private HeatWords() {}

    public static Component of(float temperature) {
        Component words = HeatBand.words(temperature);
        if (!Minecraft.getInstance().options.advancedItemTooltips) return words;
        return words.copy().append(Component.literal(" (" + Math.round(temperature) + " °C)"));
    }
}
