package dev.strataindustria.client.screen;

import dev.strataindustria.client.HeatWords;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.registry.Tier4Blocks;
import net.minecraft.network.chat.Component;

/**
 * The line a multiblock screen shows about the heat coming in through its heat inlets (tier 4 spec 8.3):
 * what it gets, what it needs, and the pipe that holds it back.
 */
final class HeatLine {
    private static final String KEY = StrataIndustria.MOD_ID + ".heat_line.";
    static final int FINE = 0xFF404040, SHORT = 0xFF8A3A2A;

    private HeatLine() {}

    /**
     * @param what        the name of the use, "hot_blast" or "preheat"
     * @param need        the temperature it needs
     * @param temperature what it was offered last tick, 0 for nothing
     * @param heat        HU it took last tick
     * @param limit       the rating of the pipe that held the temperature down, 0 for none
     */
    static Component line(String what, int need, int temperature, int heat, int limit) {
        Component name = Component.translatable(KEY + what);
        if (temperature <= 0) return Component.translatable(KEY + "none", name);
        if (temperature < need && limit > 0) return Component.translatable(KEY + "limited", name, HeatWords.of(limit));
        if (temperature < need) return Component.translatable(KEY + "cold", name, HeatWords.of(need), HeatWords.of(temperature));
        return Component.translatable(KEY + "fine", name, HeatWords.of(temperature), heat);
    }

    /** Under the line: which pipe holds the temperature down, when one does. */
    static Component tooltip(int need, int temperature, int limit) {
        if (temperature > 0 && temperature < need && limit > 0) return Component.translatable(KEY + "limited_by", pipe(limit), HeatWords.of(limit));
        return Component.translatable(KEY + "needs", HeatWords.of(need));
    }

    static int colour(int need, int temperature) {
        return temperature >= need ? FINE : SHORT;
    }

    /** The pipe with this rating, by name. */
    private static Component pipe(int limit) {
        for (var pipe : java.util.List.of(Tier4Blocks.COPPER_HEAT_PIPE, Tier4Blocks.REFRACTORY_HEAT_DUCT)) {
            HeatPipeBlock block = pipe.get();
            if (block.maxTemperature() == limit) return block.getName();
        }
        return Component.translatable(KEY + "pipes");
    }
}
