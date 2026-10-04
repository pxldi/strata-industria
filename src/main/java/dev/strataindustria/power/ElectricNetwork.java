package dev.strataindustria.power;

import dev.strataindustria.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * One electric network (tier 5 spec 6): a connected set of cables and devices. Its shape, tier,
 * weakest cable and every path loss are worked out once when it is built; each tick it only sums
 * supply and demand and hands out power (spec 6.6).
 */
public final class ElectricNetwork {
    /** What the network did for one device on its last tick. */
    public record Report(ElectricStatus status, double requested, double drawn, double loss, double fraction) {
        static final Report NONE = new Report(ElectricStatus.NO_SOURCE, 0, 0, 0, 0);
    }

    private record Member<T extends ElectricNode>(PortKey pos, T node) {}

    private final Set<PortKey> keys;
    private final Set<BlockPos> members;
    private final List<Member<ElectricSource>> generators = new ArrayList<>();
    private final List<Member<ElectricConsumer>> consumers = new ArrayList<>();
    private final List<Member<ElectricStorage>> storages = new ArrayList<>();
    /** Loss on the path from the nearest source, for each consumer, and from the nearest generator, for each storage block. */
    private final Map<PortKey, Double> losses = new HashMap<>();
    private final Map<PortKey, Report> reports = new HashMap<>();
    private final @Nullable ElectricTier tier;
    private final @Nullable ElectricTier weakestCable;
    private final @Nullable BlockPos overvoltageCable;
    private final boolean tooLarge;

    private double supply, demand, delivered, stored, storageCapacity;
    private boolean capped, wasOvervoltage;

    ElectricNetwork(Map<PortKey, ElectricNode> nodes, Map<PortKey, List<PortKey>> edges, boolean tooLarge) {
        this.keys = Collections.unmodifiableSet(nodes.keySet());
        Set<BlockPos> positions = new java.util.LinkedHashSet<>();
        for (PortKey key : nodes.keySet()) positions.add(key.pos());
        this.members = Collections.unmodifiableSet(positions);
        this.tooLarge = tooLarge;
        ElectricTier top = null, weakest = null;
        for (var entry : nodes.entrySet()) {
            PortKey pos = entry.getKey();
            ElectricNode node = entry.getValue();
            if (node instanceof ElectricStorage storage) {
                storages.add(new Member<>(pos, storage));
            } else {
                // An energy adapter is a source and a consumer at once.
                if (node instanceof ElectricSource source) generators.add(new Member<>(pos, source));
                if (node instanceof ElectricConsumer consumer) consumers.add(new Member<>(pos, consumer));
            }
            if (node instanceof ElectricConductor cable && (weakest == null || cable.cableTier().ordinal() < weakest.ordinal())) {
                weakest = cable.cableTier();
            }
            if (entry.getValue() instanceof ElectricSource source && (top == null || source.tier().ordinal() > top.ordinal())) {
                top = source.tier();
            }
        }
        this.tier = top;
        this.weakestCable = weakest;
        PortKey fault = null;
        if (top != null) {
            for (var entry : nodes.entrySet()) {
                if (entry.getValue() instanceof ElectricConductor cable && cable.cableTier().ordinal() < top.ordinal()) {
                    fault = entry.getKey();
                    break;
                }
            }
        }
        this.overvoltageCable = fault == null ? null : fault.pos();

        List<PortKey> allSources = new ArrayList<>();
        generators.forEach(m -> allSources.add(m.pos()));
        storages.forEach(m -> allSources.add(m.pos()));
        Map<PortKey, Double> fromAny = pathLosses(nodes, edges, allSources);
        for (var m : consumers) losses.put(m.pos(), fromAny.getOrDefault(m.pos(), 1.0));
        Map<PortKey, Double> fromGenerators = pathLosses(nodes, edges, generators.stream().map(Member::pos).toList());
        for (var m : storages) losses.put(m.pos(), fromGenerators.getOrDefault(m.pos(), 1.0));
    }

    /** Sum of loss rates along the cheapest path from any of {@code starts} to every block (one Dijkstra, spec 6.6). */
    private static Map<PortKey, Double> pathLosses(Map<PortKey, ElectricNode> nodes, Map<PortKey, List<PortKey>> edges, List<PortKey> starts) {
        Map<PortKey, Double> best = new HashMap<>();
        record Step(PortKey pos, double loss) {}
        PriorityQueue<Step> queue = new PriorityQueue<>((a, b) -> Double.compare(a.loss(), b.loss()));
        for (PortKey start : starts) {
            best.put(start, 0.0);
            queue.add(new Step(start, 0.0));
        }
        while (!queue.isEmpty()) {
            Step step = queue.poll();
            if (step.loss() > best.getOrDefault(step.pos(), Double.MAX_VALUE)) continue;
            for (PortKey next : edges.getOrDefault(step.pos(), List.of())) {
                double cost = nodes.get(next) instanceof ElectricConductor cable ? cable.cableTier().cableLoss() : 0.0;
                double loss = step.loss() + cost;
                if (loss < best.getOrDefault(next, Double.MAX_VALUE)) {
                    best.put(next, loss);
                    queue.add(new Step(next, loss));
                }
            }
        }
        return best;
    }

    /** One tick of supply, demand and sharing. Returns true on the tick a cable overvoltage starts. */
    public boolean tick() {
        reports.clear();
        supply = demand = delivered = 0;
        capped = false;
        stored = storageCapacity = 0;
        for (var m : storages) {
            if (alive(m)) {
                stored += m.node().stored();
                storageCapacity += m.node().capacity();
            }
        }
        if (tooLarge || tier == null || overvoltageCable != null) {
            ElectricStatus status = tooLarge ? ElectricStatus.TOO_LARGE : tier == null ? ElectricStatus.NO_SOURCE : ElectricStatus.CABLE_OVERVOLTAGE;
            Report report = new Report(status, 0, 0, 0, 0);
            for (var m : generators) reports.put(m.pos(), report);
            for (var m : consumers) reports.put(m.pos(), report);
            for (var m : storages) reports.put(m.pos(), report);
            boolean started = status == ElectricStatus.CABLE_OVERVOLTAGE && !wasOvervoltage;
            wasOvervoltage = status == ElectricStatus.CABLE_OVERVOLTAGE;
            return started;
        }
        wasOvervoltage = false;
        double maxLoss = Config.ELECTRIC_MAX_PATH_LOSS.get();

        double generation = 0;
        List<Member<ElectricSource>> liveGenerators = new ArrayList<>();
        for (var m : generators) {
            if (!alive(m)) continue;
            if (below(m.node())) {
                reports.put(m.pos(), new Report(ElectricStatus.OVERVOLTAGE, 0, 0, 0, 0));
                continue;
            }
            double out = Math.max(0, Math.min(m.node().maxOutput(), m.node().powerLimit()));
            generation += out;
            liveGenerators.add(m);
        }
        double storageOut = 0, storageRoom = 0;
        List<Member<ElectricStorage>> liveStorage = new ArrayList<>();
        for (var m : storages) {
            if (!alive(m)) continue;
            if (below(m.node())) {
                reports.put(m.pos(), new Report(ElectricStatus.OVERVOLTAGE, 0, 0, 0, 0));
                continue;
            }
            liveStorage.add(m);
            storageOut += Math.max(0, m.node().maxOutput());
            double loss = losses.getOrDefault(m.pos(), 1.0);
            if (loss < maxLoss) storageRoom += ElectricShare.gross(Math.max(0, m.node().request()), loss);
        }
        List<Member<ElectricConsumer>> served = new ArrayList<>();
        for (var m : consumers) {
            if (!alive(m)) continue;
            ElectricConsumer consumer = m.node();
            double loss = losses.getOrDefault(m.pos(), 1.0);
            double request = Math.max(0, Math.min(consumer.request(), consumer.powerLimit()));
            if (below(consumer)) {
                reports.put(m.pos(), new Report(ElectricStatus.OVERVOLTAGE, request, 0, loss, 0));
            } else if (loss >= maxLoss) {
                reports.put(m.pos(), new Report(ElectricStatus.TOO_FAR, request, 0, loss, 0));
            } else {
                demand += ElectricShare.gross(request, loss);
                served.add(m);
            }
        }
        double capacity = weakestCable == null ? Double.MAX_VALUE : weakestCable.cableCapacity();
        ElectricShare.Result result = ElectricShare.share(generation, storageOut, storageRoom, demand, capacity);
        supply = generation + storageOut;
        capped = result.capped();
        delivered = result.generated() + result.discharged();

        // Generators and discharging storage each give in proportion to what they offered.
        for (var m : liveGenerators) {
            double out = Math.min(m.node().maxOutput(), m.node().powerLimit());
            double taken = generation <= 0 ? 0 : result.generated() * out / generation;
            m.node().extract(taken);
            reports.put(m.pos(), new Report(taken > 0 ? ElectricStatus.RUNNING : ElectricStatus.IDLE, 0, taken, 0, 1));
        }
        for (var m : served) {
            double request = Math.max(0, Math.min(m.node().request(), m.node().powerLimit()));
            double loss = losses.getOrDefault(m.pos(), 0.0);
            double given = request * result.fraction();
            m.node().receive(given);
            ElectricStatus status = request <= 0 ? ElectricStatus.IDLE : result.fraction() >= 0.999 ? ElectricStatus.RUNNING : ElectricStatus.LOW_POWER;
            reports.put(m.pos(), new Report(status, request, ElectricShare.gross(given, loss), loss, result.fraction()));
        }
        share(liveStorage, result, storageOut, maxLoss);
        return false;
    }

    /** Storage discharges in proportion to what each offered and charges evenly among blocks with room (spec 6.1). */
    private void share(List<Member<ElectricStorage>> live, ElectricShare.Result result, double storageOut, double maxLoss) {
        Map<PortKey, Double> charge = new HashMap<>();
        if (result.charged() > 0) {
            List<Member<ElectricStorage>> open = new ArrayList<>();
            for (var m : live) {
                if (losses.getOrDefault(m.pos(), 1.0) < maxLoss && m.node().request() > 0) open.add(m);
            }
            open.sort((a, b) -> Double.compare(room(a), room(b)));
            double left = result.charged();
            for (int i = 0; i < open.size(); i++) {
                double give = Math.min(room(open.get(i)), left / (open.size() - i));
                charge.put(open.get(i).pos(), give);
                left -= give;
            }
        }
        for (var m : live) {
            double taken = storageOut <= 0 ? 0 : result.discharged() * Math.max(0, m.node().maxOutput()) / storageOut;
            double gross = charge.getOrDefault(m.pos(), 0.0);
            double loss = losses.getOrDefault(m.pos(), 0.0);
            if (taken > 0) m.node().extract(taken);
            if (gross > 0) m.node().receive(gross * (1.0 - loss));
            ElectricStatus status = taken > 0 || gross > 0 ? ElectricStatus.RUNNING : ElectricStatus.IDLE;
            reports.put(m.pos(), new Report(status, m.node().request(), gross - taken, loss, 1));
        }
    }

    /** Grossed-up room of a storage block. */
    private double room(Member<ElectricStorage> m) {
        return ElectricShare.gross(Math.max(0, m.node().request()), losses.getOrDefault(m.pos(), 0.0));
    }

    private boolean below(ElectricDevice device) {
        return tier != null && device.tier().ordinal() < tier.ordinal();
    }

    private static boolean alive(Member<? extends ElectricNode> m) {
        return !m.node().removed();
    }

    public Set<BlockPos> members() {
        return members;
    }

    public Report report(BlockPos pos) {
        return report(new PortKey(pos, 0));
    }

    public Report report(PortKey key) {
        return reports.getOrDefault(key, Report.NONE);
    }

    /** Every port of this network, for the level's lookup. */
    Set<PortKey> keys() {
        return keys;
    }

    public @Nullable ElectricTier tier() {
        return tier;
    }

    public @Nullable ElectricTier weakestCable() {
        return weakestCable;
    }

    public @Nullable BlockPos overvoltageCable() {
        return overvoltageCable;
    }

    public boolean tooLarge() {
        return tooLarge;
    }

    /** J/t the sources could give on the last tick. */
    public double supply() {
        return supply;
    }

    /** J/t the consumers asked for on the last tick, grossed up for loss. */
    public double demand() {
        return demand;
    }

    /** J/t taken from sources on the last tick. */
    public double delivered() {
        return delivered;
    }

    public double stored() {
        return stored;
    }

    public double storageCapacity() {
        return storageCapacity;
    }

    /** Whether the weakest cable held the flow back on the last tick. */
    public boolean capped() {
        return capped;
    }

    /** The largest path loss among consumers, for the cable line. */
    public double worstLoss() {
        double worst = 0;
        for (var m : consumers) worst = Math.max(worst, Math.min(1.0, losses.getOrDefault(m.pos(), 0.0)));
        return worst;
    }

    /** Path loss of a consumer or storage block, as worked out when the network was built. */
    public double loss(BlockPos pos) {
        return loss(new PortKey(pos, 0));
    }

    public double loss(PortKey key) {
        return losses.getOrDefault(key, 0.0);
    }
}
