package dev.strataindustria.power;

/** A cable (spec 8.1): it carries a network, caps its throughput and loses a little per block. */
public interface ElectricConductor extends ElectricNode {
    ElectricTier cableTier();

    /** Fraction of the power lost entering this block; a cable's tier decides it, a utility pole loses nothing (spec 8.3). */
    default double blockLoss() {
        return cableTier().cableLoss();
    }

    /** J/t this block lets through; the network carries at most the least of them (spec 6.2). */
    default int capacity() {
        return cableTier().cableCapacity();
    }
}
