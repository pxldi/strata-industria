package dev.strataindustria.transport.ropeway;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * Rules for riding a ropeway (outposts spec 8.3): a terminal with someone aboard is kept loaded however far its seat
 * has travelled, and nobody is shaken off a moving line by a stray sneak.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class RopewayRides {
    /** Tickets are not kept across a restart: the seat goes with the world, and so does the reason for the ticket. */
    public static final TicketController CONTROLLER = new TicketController(StrataIndustria.id("ropeway_ride"),
            (level, helper) -> {
                for (BlockPos pos : java.util.List.copyOf(helper.getBlockTickets().keySet())) helper.removeAllTickets(pos);
            });

    private RopewayRides() {}

    @SubscribeEvent
    static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /** Keeps the chunk of {@code terminal} loaded, or lets it go. */
    public static void hold(ServerLevel level, BlockPos terminal, boolean hold) {
        ChunkPos chunk = ChunkPos.containing(terminal);
        CONTROLLER.forceChunk(level, terminal, chunk.x(), chunk.z(), hold, true);
    }

    @SubscribeEvent
    static void mount(EntityMountEvent event) {
        if (!event.isDismounting() || event.getLevel().isClientSide()) return;
        if (event.getEntityBeingMounted() instanceof RopewaySeatEntity seat && !seat.mayLeave()) event.setCanceled(true);
    }
}
