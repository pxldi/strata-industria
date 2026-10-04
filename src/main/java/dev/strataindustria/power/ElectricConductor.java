package dev.strataindustria.power;

/** A cable (spec 8.1): it carries a network, caps its throughput and loses a little per block. */
public interface ElectricConductor extends ElectricNode {
    ElectricTier cableTier();
}
