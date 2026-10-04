package dev.strataindustria.transport.outpost;

import dev.strataindustria.Config;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.scores.Team;

/**
 * What every charter of a dimension loads (outposts spec 3.4): districts from the live links, the square each
 * charter's best link allows, and the owner's chunk budget. Pure arithmetic over a {@link RouteIndex}; the
 * tickets themselves are set by {@link OutpostTickets}.
 */
public final class OutpostPlan {
    /** Why a charter loads what it loads. */
    public enum State { LOADED, NO_LINE, OWNER_AWAY, CHUNK_LIMIT }

    /** One charter's share: the side it wanted, the side it got and the chunks of that square. */
    public record Entry(UUID charter, State state, int wanted, int side, LongSet chunks) {
        public boolean loaded() {
            return state == State.LOADED;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();
    private final List<Set<UUID>> districts = new ArrayList<>();

    private OutpostPlan() {}

    public Entry entry(UUID charter) {
        return entries.get(charter);
    }

    /** Districts of two or more charters joined by live links. */
    public List<Set<UUID>> districts() {
        return districts;
    }

    public Map<UUID, Entry> entries() {
        return entries;
    }

    /** Chunks one owner keeps loaded under this plan. */
    public int chunksOf(RouteIndex index, UUID owner) {
        LongSet union = new LongOpenHashSet();
        for (Entry entry : entries.values()) {
            if (index.charter(entry.charter()).map(c -> c.owner().equals(owner)).orElse(false)) union.addAll(entry.chunks());
        }
        return union.size();
    }

    /** Side in chunks of the square a link of this tier loads, from the config. */
    public static int sideForTier(int tier) {
        return Params.fromConfig().sideForTier(tier);
    }

    public static LongSet square(BlockPos centre, int side) {
        LongSet chunks = new LongOpenHashSet();
        int half = side / 2, cx = centre.getX() >> 4, cz = centre.getZ() >> 4;
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) chunks.add(ChunkPos.pack(cx + dx, cz + dz));
        return chunks;
    }

    /** True for the charter's owner and for players on the owner's scoreboard team. */
    public static boolean isOwner(ServerLevel level, ServerPlayer player, Charter charter) {
        if (player.getUUID().equals(charter.owner())) return true;
        Team team = level.getScoreboard().getPlayersTeam(charter.ownerName());
        return team != null && team.equals(player.getTeam());
    }

    /** The tuning a plan is worked out with: the config in play, or other numbers in tests. */
    public record Params(List<Integer> sides, int budget, boolean requireOwnerOnline) {
        public static Params fromConfig() {
            return new Params(List.copyOf(Config.OUTPOSTS_AREA_BY_TIER.get()), Config.OUTPOSTS_MAX_CHUNKS_PER_OWNER.getAsInt(),
                    Config.OUTPOSTS_REQUIRE_OWNER_ONLINE.get());
        }

        int sideForTier(int tier) {
            if (tier < 3 || sides.isEmpty()) return 0;
            int side = sides.get(Math.min(tier - 3, sides.size() - 1));
            return Math.max(1, side % 2 == 0 ? side + 1 : side);
        }
    }

    /**
     * @param online which connected players count; the logout event passes the leaving player out
     */
    public static OutpostPlan compute(ServerLevel level, RouteIndex index, Predicate<ServerPlayer> online) {
        return compute(level, index, online, Params.fromConfig());
    }

    public static OutpostPlan compute(ServerLevel level, RouteIndex index, Predicate<ServerPlayer> online, Params params) {
        OutpostPlan plan = new OutpostPlan();
        List<Charter> active = index.activeCharters();
        // Districts: charters joined by live links, walked from each charter not yet seen.
        Set<UUID> seen = new HashSet<>();
        Map<UUID, Set<UUID>> districtOf = new HashMap<>();
        for (Charter start : active) {
            if (!seen.add(start.id())) continue;
            Set<UUID> district = new HashSet<>();
            List<UUID> open = new ArrayList<>(List.of(start.id()));
            while (!open.isEmpty()) {
                UUID next = open.remove(open.size() - 1);
                if (!district.add(next)) continue;
                seen.add(next);
                for (Link link : index.linksOf(next)) if (index.live(link)) open.add(link.other(next));
            }
            if (district.size() < 2) continue;
            plan.districts.add(district);
            for (UUID id : district) districtOf.put(id, district);
        }
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers().stream().filter(online).toList();
        Map<UUID, LongSet> used = new HashMap<>();
        int budget = params.budget();
        boolean needOwner = params.requireOwnerOnline();
        for (Charter charter : active) {
            Set<UUID> district = districtOf.get(charter.id());
            if (district == null) {
                plan.entries.put(charter.id(), new Entry(charter.id(), State.NO_LINE, 0, 0, new LongOpenHashSet()));
                continue;
            }
            int wanted = params.sideForTier(index.bestTier(charter.id()));
            boolean away = needOwner && !districtOnline(level, index, district, players);
            if (away) {
                plan.entries.put(charter.id(), new Entry(charter.id(), State.OWNER_AWAY, wanted, 0, new LongOpenHashSet()));
                continue;
            }
            LongSet owned = used.computeIfAbsent(charter.owner(), k -> new LongOpenHashSet());
            int side = wanted;
            LongSet chunks = null;
            for (; side >= 3 || (side >= 1 && side == wanted); side -= 2) {
                LongSet square = square(charter.pos(), side);
                LongSet merged = new LongOpenHashSet(owned);
                merged.addAll(square);
                if (merged.size() <= budget) {
                    chunks = square;
                    owned.addAll(square);
                    break;
                }
            }
            if (chunks == null) {
                plan.entries.put(charter.id(), new Entry(charter.id(), State.CHUNK_LIMIT, wanted, 0, new LongOpenHashSet()));
            } else {
                plan.entries.put(charter.id(), new Entry(charter.id(), State.LOADED, wanted, side, chunks));
            }
        }
        return plan;
    }

    /** Owners counted as online without a connected player: for tests and for server tools that stand in for one. */
    public static final Set<UUID> ALSO_ONLINE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static boolean districtOnline(ServerLevel level, RouteIndex index, Set<UUID> district, List<ServerPlayer> players) {
        for (UUID id : district) {
            Charter charter = index.charter(id).orElse(null);
            if (charter == null) continue;
            if (ALSO_ONLINE.contains(charter.owner())) return true;
            for (ServerPlayer player : players) if (isOwner(level, player, charter)) return true;
        }
        return false;
    }
}
