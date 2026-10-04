package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.RouteIndex;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Proving the tramway (outposts spec 3.3): a lead tub that leaves a stop in one charter's area and comes to rest
 * at a stop in another charter's area, rolling only over this mod's track, makes a link between the two.
 */
public final class TramwayRoutes {
    /** Chunks either side of a charter's own chunk that always count as its area (the 3 x 3 of the smallest link). */
    public static final int STATION_RADIUS = 1;
    private static final double TELL_RANGE = 24;

    private TramwayRoutes() {}

    /** The charter whose area holds a station at {@code pos}; of several, the nearest post. */
    public static @Nullable Charter stationCharter(RouteIndex index, BlockPos pos) {
        Charter best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Charter charter : index.activeCharters()) {
            if (Math.abs((pos.getX() >> 4) - (charter.pos().getX() >> 4)) > STATION_RADIUS
                    || Math.abs((pos.getZ() >> 4) - (charter.pos().getZ() >> 4)) > STATION_RADIUS) continue;
            double dx = pos.getX() - charter.pos().getX(), dz = pos.getZ() - charter.pos().getZ();
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                best = charter;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The lead of a consist has come to a stop at {@code stop}. */
    public static void arrived(ServerLevel level, MineTubEntity lead, BlockPos stop) {
        TrackTrace trace = lead.trace();
        BlockPos origin = trace.origin();
        if (origin != null && !origin.equals(stop)) {
            Journal.awardNear(level, stop, Journal.TUB_ARRIVED);
            if (lead.pusher() != null && level.getPlayerByUUID(lead.pusher()) instanceof ServerPlayer pusher) {
                Journal.award(pusher, Journal.TUB_ARRIVED);
            }
            RouteIndex index = RouteIndex.get(level);
            Charter from = stationCharter(index, origin), to = stationCharter(index, stop);
            if (from != null && to != null && !from.id().equals(to.id())) {
                BlockPos vanilla = trace.vanilla();
                if (vanilla != null) {
                    tell(level, stop, Component.translatable(StrataIndustria.MOD_ID + ".outposts.vanilla_rails",
                            vanilla.getX() + " " + vanilla.getY() + " " + vanilla.getZ()));
                } else {
                    List<BlockPos> route = trace.route();
                    route.add(stop.immutable());
                    RouteIndex.Proof proof = index.prove(level, from.id(), to.id(), LinkKind.TRAMWAY, route);
                    if (proof.refusal() != null) tell(level, stop, proof.refusal());
                    else if (proof.link() != null) index.traffic(level, proof.link().id());
                }
            }
        }
        trace.restart(stop);
    }

    private static void tell(ServerLevel level, BlockPos near, Component message) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(near).inflate(TELL_RANGE))) {
            player.sendOverlayMessage(message);
        }
    }
}
