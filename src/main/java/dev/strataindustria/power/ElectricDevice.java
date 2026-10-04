package dev.strataindustria.power;

/** A generator, storage block or machine. A device below its network's tier stops with "Overvoltage" (spec 6.2). */
public interface ElectricDevice extends ElectricNode {
    ElectricTier tier();
}
