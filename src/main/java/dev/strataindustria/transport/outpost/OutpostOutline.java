package dev.strataindustria.transport.outpost;

import dev.strataindustria.StrataIndustria;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Sneak-using a charter draws its loaded area on the chunk borders for ten seconds (outposts spec 3.1). */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class OutpostOutline {
    private static final int SECONDS = 10;
    private static final int PULSE = 10;

    private record Show(UUID player, UUID charter, ServerLevel level, long until) {}

    private static final List<Show> SHOWS = new ArrayList<>();

    private OutpostOutline() {}

    public static void show(ServerLevel level, ServerPlayer player, Charter charter) {
        SHOWS.removeIf(s -> s.player().equals(player.getUUID()));
        SHOWS.add(new Show(player.getUUID(), charter.id(), level, level.getGameTime() + SECONDS * 20L));
        OutpostPlan.Entry entry = RouteIndex.get(level).plan(level).entry(charter.id());
        if (entry == null || !entry.loaded()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(StrataIndustria.MOD_ID + ".outposts.outline_none"));
        }
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (SHOWS.isEmpty()) return;
        for (Iterator<Show> it = SHOWS.iterator(); it.hasNext(); ) {
            Show show = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(show.player());
            if (player == null || show.level().getGameTime() > show.until()) {
                it.remove();
                continue;
            }
            if (show.level().getGameTime() % PULSE != 0) continue;
            OutpostPlan.Entry entry = RouteIndex.get(show.level()).plan(show.level()).entry(show.charter());
            if (entry != null && entry.loaded()) draw(show.level(), player, entry.chunks());
        }
    }

    /** Particles up the corners and along the edge of every outer chunk border, around the player's height. */
    private static void draw(ServerLevel level, ServerPlayer player, LongSet chunks) {
        double y0 = Math.floor(player.getY());
        for (long chunk : chunks) {
            int cx = ChunkPos.getX(chunk), cz = ChunkPos.getZ(chunk);
            for (int side = 0; side < 4; side++) {
                int dx = side == 0 ? -1 : side == 1 ? 1 : 0, dz = side == 2 ? -1 : side == 3 ? 1 : 0;
                if (chunks.contains(ChunkPos.pack(cx + dx, cz + dz))) continue;
                for (int i = 0; i < 16; i += 3) {
                    double x = dx == 0 ? cx * 16 + i + 0.5 : (dx < 0 ? cx * 16 : cx * 16 + 16);
                    double z = dz == 0 ? cz * 16 + i + 0.5 : (dz < 0 ? cz * 16 : cz * 16 + 16);
                    if (Math.abs(x - player.getX()) > 48 || Math.abs(z - player.getZ()) > 48) continue;
                    for (int dy = -1; dy <= 3; dy += 2) {
                        level.sendParticles(player, ParticleTypes.END_ROD, false, false, x, y0 + dy, z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }
}
