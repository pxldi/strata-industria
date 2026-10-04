package dev.strataindustria.transport.outpost;

import dev.strataindustria.StrataIndustria;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Keeps the chunks of every charter's area loaded (outposts spec 3.4) with ticking tickets, one owner per
 * charter post. Tickets are rebuilt from the {@link RouteIndex} on server start (the validation callback drops
 * whatever the last run left behind) and whenever the plan changes: a link proved or cut, a post placed or
 * taken down, an owner joining or leaving.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class OutpostTickets {
    public static final TicketController CONTROLLER = new TicketController(StrataIndustria.id("outpost"),
            (level, helper) -> {
                for (BlockPos owner : java.util.List.copyOf(helper.getBlockTickets().keySet())) helper.removeAllTickets(owner);
            });

    /** Chunks held per dimension and charter post, so a refresh only changes the difference. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, LongSet>> HELD = new HashMap<>();
    private static final int CHECK_INTERVAL = 200;
    /** While set, {@link #refresh} works out the plan but sets no tickets. For tests that look at plans only. */
    public static volatile boolean paused;

    private OutpostTickets() {}

    @SubscribeEvent
    static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /** Chunks currently held for the charter at {@code post}. */
    public static LongSet held(ServerLevel level, BlockPos post) {
        Map<BlockPos, LongSet> map = HELD.get(level.dimension());
        LongSet set = map == null ? null : map.get(post);
        return set == null ? new LongOpenHashSet() : new LongOpenHashSet(set);
    }

    /** Total chunks held in a dimension, over every charter. */
    public static int heldCount(ServerLevel level) {
        Map<BlockPos, LongSet> map = HELD.get(level.dimension());
        return map == null ? 0 : map.values().stream().mapToInt(LongSet::size).sum();
    }

    public static void refresh(ServerLevel level) {
        refresh(level, p -> true);
    }

    private static void refresh(ServerLevel level, Predicate<ServerPlayer> online) {
        RouteIndex index = RouteIndex.get(level);
        OutpostPlan plan = OutpostPlan.compute(level, index, online);
        index.setPlan(plan);
        if (paused) return;
        Map<BlockPos, LongSet> held = HELD.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Map<BlockPos, LongSet> wanted = new HashMap<>();
        for (Charter charter : index.activeCharters()) {
            OutpostPlan.Entry entry = plan.entry(charter.id());
            if (entry != null && entry.loaded()) wanted.put(charter.pos(), entry.chunks());
        }
        // Drop what is no longer wanted, then add what is new.
        for (Map.Entry<BlockPos, LongSet> old : java.util.List.copyOf(held.entrySet())) {
            LongSet keep = wanted.get(old.getKey());
            for (long chunk : old.getValue().toLongArray()) {
                if (keep != null && keep.contains(chunk)) continue;
                CONTROLLER.forceChunk(level, old.getKey(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, false);
                old.getValue().remove(chunk);
            }
            if (old.getValue().isEmpty()) held.remove(old.getKey());
        }
        for (Map.Entry<BlockPos, LongSet> want : wanted.entrySet()) {
            LongSet have = held.computeIfAbsent(want.getKey(), k -> new LongOpenHashSet());
            for (long chunk : want.getValue()) {
                if (have.contains(chunk)) continue;
                CONTROLLER.forceChunk(level, want.getKey(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, false);
                have.add(chunk);
            }
        }
    }

    private static void refreshAll(net.minecraft.server.MinecraftServer server, Predicate<ServerPlayer> online) {
        for (ServerLevel level : server.getAllLevels()) {
            RouteIndex.get(level).expire(level);
            refresh(level, online);
        }
    }

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        HELD.clear();
        refreshAll(event.getServer(), p -> true);
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        HELD.clear();
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getServer() != null) refreshAll(player.level().getServer(), p -> true);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getServer() != null) {
            UUID leaving = player.getUUID();
            refreshAll(player.level().getServer(), p -> !p.getUUID().equals(leaving));
        }
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_INTERVAL == 0) refreshAll(event.getServer(), p -> true);
    }
}
