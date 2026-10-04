package dev.strataindustria.transport.outpost;

import dev.strataindustria.electric.PoleInsulatorBlockEntity;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.transport.rail.TramwayRoutes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Proving the power line (outposts spec 3.3 and 9.5): energy that flows over at least one span between pole
 * insulators standing in the areas of two charters makes a T5 link between them. The route is the insulators that
 * carry spans, so taking one down cuts the link like any other block of a route.
 */
public final class PowerLinks {
    /** Most insulators a route lists, so a huge grid does not make a huge route. */
    private static final int MAX_ROUTE = 256;

    private PowerLinks() {}

    /** A network that delivered power this tick: see whether it joins two charters. Cheap enough to run every few seconds. */
    public static void check(ServerLevel level, ElectricNetwork network) {
        if (network.delivered() <= 0) return;
        RouteIndex index = RouteIndex.get(level);
        if (index.activeCharters().size() < 2) return;
        Map<UUID, Charter> reached = new LinkedHashMap<>();
        List<BlockPos> route = new ArrayList<>();
        for (BlockPos pos : network.members()) {
            if (!(level.getBlockEntity(pos) instanceof PoleInsulatorBlockEntity insulator) || insulator.spanCount() == 0) continue;
            if (route.size() < MAX_ROUTE) route.add(pos);
            Charter charter = TramwayRoutes.stationCharter(index, pos);
            if (charter != null) reached.putIfAbsent(charter.id(), charter);
        }
        if (reached.size() < 2) return;
        List<Charter> sorted = new ArrayList<>(reached.values());
        sorted.sort(Comparator.comparing(Charter::id));
        for (int i = 0; i + 1 < sorted.size(); i++) prove(level, index, sorted.get(i), sorted.get(i + 1), route);
    }

    private static void prove(ServerLevel level, RouteIndex index, Charter a, Charter b, List<BlockPos> route) {
        for (Link link : index.linksOf(a.id())) {
            if (link.kind() == LinkKind.POWER && link.joins(b.id()) && link.open()) {
                index.traffic(level, link.id());
                return;
            }
        }
        index.prove(level, a.id(), b.id(), LinkKind.POWER, route);
    }
}
