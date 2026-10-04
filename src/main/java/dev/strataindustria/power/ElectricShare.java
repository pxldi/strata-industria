package dev.strataindustria.power;

/**
 * The per-tick arithmetic of an electric network (tier 5 spec 6.1 to 6.3), kept apart from the
 * world so it can be checked against the spec's worked example. Every amount is in J for one tick.
 *
 * <p>Demand is already grossed up for path loss. Generators cover demand first; if they cannot,
 * storage discharges; if that is still short, every consumer gets the same fraction. Surplus
 * generation charges storage. No more than the weakest cable's capacity flows in total.
 */
public final class ElectricShare {
    private ElectricShare() {}

    /**
     * @param fraction   share of its request every consumer is given, 0 to 1
     * @param generated  J taken from generators
     * @param discharged J taken from storage
     * @param charged    J sent towards storage (before its path loss)
     * @param capped     whether the cable capacity held the flow back
     */
    public record Result(double fraction, double generated, double discharged, double charged, boolean capped) {}

    /**
     * @param generation  J the generators can give
     * @param storageOut  J the storage blocks can discharge
     * @param storageRoom J the storage blocks can take, grossed up for their path loss
     * @param demand      J the consumers ask for, grossed up for their path loss
     * @param capacity    J the network's weakest cable carries
     */
    public static Result share(double generation, double storageOut, double storageRoom, double demand, double capacity) {
        double served = Math.min(demand, capacity);
        boolean capped = demand > capacity;
        if (generation >= served) {
            double charge = Math.max(0.0, Math.min(Math.min(generation - served, storageRoom), capacity - served));
            if (generation - served > charge && storageRoom > charge) capped = true;
            return new Result(demand <= 0 ? 1.0 : served / demand, served + charge, 0.0, charge, capped);
        }
        double discharge = Math.min(served - generation, storageOut);
        return new Result(demand <= 0 ? 1.0 : (generation + discharge) / demand, generation, discharge, 0.0, capped);
    }

    /** Grosses a request up for its path loss: what the sources pay for {@code request} to arrive. */
    public static double gross(double request, double loss) {
        return request / (1.0 - loss);
    }
}
