package dev.strataindustria.power;

/** A generator, storage block or machine. A device below its network's tier stops with "Overvoltage" (spec 6.2). */
public interface ElectricDevice extends ElectricNode {
    ElectricTier tier();

    /** Most J/t this device gives or takes; its tier's limit unless it says otherwise (the transformer passes 128). */
    default double powerLimit() {
        return tier().maxPower();
    }
}
