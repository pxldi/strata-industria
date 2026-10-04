package dev.strataindustria.transport.rail;

import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.RouteIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The moving ticket of one driverless lead (outposts spec 6.5): opened when it leaves a stop inside a charter's area
 * with its owner about, followed as it runs, and let go half a minute after it rests in a station again.
 */
final class LineTicket {
    /** Ticks a lead may rest in a station before its chunks are let go. */
    static final int LINGER = 600;

    private boolean open;
    private int lingering;

    boolean open() {
        return open;
    }

    /** Lets the lead go from {@code stop}: false when its owner has no line free. A ridden lead needs no ticket. */
    boolean admit(ServerLevel server, MineTubEntity lead, @Nullable BlockPos stop) {
        if (stop == null || open || lead.hasPassenger(e -> e instanceof Player)) return true;
        Charter charter = TramwayRoutes.stationCharter(RouteIndex.get(server), stop);
        if (charter == null || !VehicleTickets.ownerAround(server, charter)) return true;
        if (!VehicleTickets.available(server, charter.owner())) return false;
        VehicleTickets.open(lead.getUUID(), charter.owner());
        open = true;
        return true;
    }

    /** Keeps the chunks around the lead loaded while it runs, and lets them go once it has rested in a station a while. */
    void tick(ServerLevel server, MineTubEntity lead) {
        if (!open) return;
        BlockPos heldAt = lead.heldAt();
        if (heldAt != null && TramwayRoutes.stationCharter(RouteIndex.get(server), heldAt) != null) {
            if (++lingering >= LINGER) {
                release(server, lead);
                return;
            }
        } else {
            lingering = 0;
        }
        if (lead.tickCount % 4 == 0) VehicleTickets.follow(server, lead.getUUID(), lead.chunkPosition());
    }

    void leaving() {
        lingering = 0;
    }

    void release(ServerLevel server, MineTubEntity lead) {
        VehicleTickets.release(server, lead.getUUID());
        open = false;
        lingering = 0;
    }
}
