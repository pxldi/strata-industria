package dev.strataindustria.heat;

/**
 * A block entity that takes heat from a firebox under it (tier 4 spec 8.3): boilers now, and the
 * roaster, kiln, smelter and steam hammer later.
 */
public interface HeatConsumer {
    /**
     * Offers {@code heat} HU this tick at {@code temperature} °C and returns how much the consumer
     * takes. A consumer that is too cool to work takes nothing.
     */
    int offerHeat(float temperature, int heat);
}
