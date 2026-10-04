package dev.strataindustria.power;

/**
 * Something that draws from a network (spec 6.6). It asks for J this tick and is handed what the
 * network can give, after path losses, which the network pays for out of its sources.
 */
public interface ElectricConsumer extends ElectricDevice {
    /** J wanted this tick. */
    double request();

    /** J delivered this tick, at most {@link #request}. */
    void receive(double amount);
}
