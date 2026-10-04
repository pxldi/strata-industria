package dev.strataindustria.power;

/**
 * Storage that feeds a network (spec 6.4 and 7.4): a source when the network falls short and a
 * consumer of surplus. As a source, {@link #maxOutput} is what it may discharge this tick; as a
 * consumer, {@link #request} is the room it may fill this tick.
 */
public interface ElectricStorage extends ElectricSource, ElectricConsumer {
    double stored();

    double capacity();
}
