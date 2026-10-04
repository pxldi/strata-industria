package dev.strataindustria.heat;

import org.jspecify.annotations.Nullable;

/**
 * A block entity that takes heat from a firebox under it or over heat pipes (tier 4 spec 8.3): boilers,
 * and through heat inlets the blast furnace and the converter.
 */
public interface HeatConsumer {
    /**
     * How many HU it would take this tick at {@code temperature} °C. A consumer that is too cool to work,
     * or has had its fill this tick, wants nothing.
     */
    int heatDemand(float temperature);

    /**
     * Offers {@code heat} HU this tick at {@code temperature} °C and returns how much the consumer takes.
     * It is called every tick a source is burning, with no heat when the consumer wants none, so the
     * consumer always knows how hot its supply is.
     */
    int offerHeat(float temperature, int heat);

    /**
     * Called before {@link #offerHeat} when the heat comes over pipes: how many pipe blocks it crossed, and
     * the pipe that holds the temperature down when the source is hotter than its rating.
     */
    default void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {}
}
