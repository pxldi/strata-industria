package dev.strataindustria.power;

/**
 * Something that feeds a network (spec 6.6). Generators produce only what is taken: the network asks
 * how much could be given this tick, then takes some or all of it with {@link #extract}.
 */
public interface ElectricSource extends ElectricDevice {
    /** J this source can give this tick. */
    double maxOutput();

    /** The network takes {@code amount} J this tick, never more than {@link #maxOutput}. */
    void extract(double amount);
}
