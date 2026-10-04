package dev.strataindustria.transport.rail;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.OutpostTickets;
import dev.strataindustria.transport.outpost.RouteIndex;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The moving chunk ticket of a driverless consist (outposts spec 6.5): the chunk under its lead and the eight around
 * it, followed as it moves. Each counts nine chunks against its owner's budget, and an owner has only so many
 * moving at once. Tickets are not kept across a restart; a consist left standing at a stop is loaded by its
 * charter's area.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class VehicleTickets {
    public static final TicketController CONTROLLER = new TicketController(StrataIndustria.id("consist"),
            (level, helper) -> {
                for (UUID vehicle : List.copyOf(helper.getEntityTickets().keySet())) helper.removeAllTickets(vehicle);
            });
    /** Chunks a ticket holds around the lead. */
    public static final int CHUNKS = 9;

    private record Ticket(UUID owner, LongSet chunks) {}

    private static final Map<UUID, Ticket> BY_VEHICLE = new HashMap<>();

    private VehicleTickets() {}

    @SubscribeEvent
    static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        BY_VEHICLE.clear();
    }

    /** Consists this owner has moving now. */
    public static int moving(UUID owner) {
        int count = 0;
        for (Ticket ticket : BY_VEHICLE.values()) if (ticket.owner.equals(owner)) count++;
        return count;
    }

    public static boolean holds(UUID vehicle) {
        return BY_VEHICLE.containsKey(vehicle);
    }

    /**
     * Whether {@code owner} can have one more consist moving: under the per-owner limit and with room in the chunk
     * budget beside what the charter areas already hold.
     */
    public static boolean available(ServerLevel level, UUID owner) {
        int moving = moving(owner);
        if (moving >= Config.TRANSPORT_MAX_TICKETED_CONSISTS.getAsInt()) return false;
        int held = 0;
        for (Charter charter : RouteIndex.get(level).activeCharters()) {
            if (charter.owner().equals(owner)) held += OutpostTickets.held(level, charter.pos()).size();
        }
        return held + CHUNKS * (moving + 1) <= Config.OUTPOSTS_MAX_CHUNKS_PER_OWNER.getAsInt();
    }

    /** Whether the owner is around to keep an area loaded for; with the setting off, always. */
    public static boolean ownerAround(ServerLevel level, Charter charter) {
        if (!Config.OUTPOSTS_REQUIRE_OWNER_ONLINE.getAsBoolean() || OutpostPlan.ALSO_ONLINE.contains(charter.owner())) return true;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (OutpostPlan.isOwner(level, player, charter)) return true;
        }
        return false;
    }

    /** Opens a ticket for {@code vehicle} under {@code owner}. */
    public static void open(UUID vehicle, UUID owner) {
        BY_VEHICLE.computeIfAbsent(vehicle, id -> new Ticket(owner, new LongOpenHashSet()));
    }

    /** Moves the ticket to the nine chunks around {@code centre}; a vehicle without a ticket is left alone. */
    public static void follow(ServerLevel level, UUID vehicle, ChunkPos centre) {
        Ticket ticket = BY_VEHICLE.get(vehicle);
        if (ticket == null) return;
        LongSet wanted = new LongOpenHashSet();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) wanted.add(ChunkPos.pack(centre.x() + dx, centre.z() + dz));
        for (long chunk : ticket.chunks.toLongArray()) {
            if (wanted.contains(chunk)) continue;
            CONTROLLER.forceChunk(level, vehicle, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
            ticket.chunks.remove(chunk);
        }
        for (long chunk : wanted) {
            if (ticket.chunks.contains(chunk)) continue;
            CONTROLLER.forceChunk(level, vehicle, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
            ticket.chunks.add(chunk);
        }
    }

    /** Lets the chunks go. */
    public static void release(ServerLevel level, UUID vehicle) {
        Ticket ticket = BY_VEHICLE.remove(vehicle);
        if (ticket == null) return;
        for (long chunk : ticket.chunks.toLongArray()) {
            CONTROLLER.forceChunk(level, vehicle, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
        }
    }
}
