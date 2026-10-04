package dev.strataindustria.transport.outpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.RockLookup;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.TransportSounds;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Charters, the links between them and the routes that prove those links (outposts spec 3.3), per dimension.
 * Track, tower, span and pipeline blocks call {@link #cut} when one is removed; whatever carries something
 * between two stations calls {@link #prove}. Districts and the chunks they load are worked out from this by
 * {@link OutpostPlan} and held by {@link OutpostTickets}.
 */
public final class RouteIndex extends SavedData {
    /** Three in-game days, in ticks. */
    public static final long DORMANT_TICKS = 72000;
    /** How far a taken-down post can move and still take its old name and links back. */
    public static final int REVIVE_RADIUS = 16;
    public static final int MAX_NAME = 24;

    private record Data(List<Charter> charters, List<Link> links) {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Charter.CODEC.listOf().fieldOf("charters").forGetter(Data::charters),
                Link.CODEC.listOf().fieldOf("links").forGetter(Data::links)
        ).apply(i, Data::new));
    }

    public static final SavedDataType<RouteIndex> TYPE = new SavedDataType<>(StrataIndustria.id("route_index"),
            RouteIndex::new, Data.CODEC.xmap(RouteIndex::new, RouteIndex::data));

    private final Map<UUID, Charter> charters = new LinkedHashMap<>();
    private final Map<UUID, Link> links = new LinkedHashMap<>();
    /** Chunk key to the links with route blocks in that chunk, and each link's route blocks by position key. */
    private final Map<Long, Set<UUID>> linksByChunk = new HashMap<>();
    private final Map<UUID, LongSet> routeSets = new HashMap<>();
    private @Nullable OutpostPlan plan;

    public RouteIndex() {}

    private RouteIndex(Data data) {
        for (Charter charter : data.charters()) charters.put(charter.id(), charter);
        for (Link link : data.links()) addLink(link);
    }

    private Data data() {
        return new Data(List.copyOf(charters.values()), List.copyOf(links.values()));
    }

    public static RouteIndex get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ---------------------------------------------------------------- lookups

    public List<Charter> charters() {
        return List.copyOf(charters.values());
    }

    public List<Charter> activeCharters() {
        return charters.values().stream().filter(Charter::active).toList();
    }

    public List<Link> links() {
        return List.copyOf(links.values());
    }

    public Optional<Charter> charter(UUID id) {
        return Optional.ofNullable(charters.get(id));
    }

    /** The standing charter whose post is at {@code pos}. */
    public Optional<Charter> charterAt(BlockPos pos) {
        for (Charter charter : charters.values()) if (charter.active() && charter.pos().equals(pos)) return Optional.of(charter);
        return Optional.empty();
    }

    /** The charter at {@code pos}, standing or just taken down (a drop is worked out after the post is gone). */
    public Optional<Charter> charterEver(BlockPos pos) {
        Optional<Charter> standing = charterAt(pos);
        if (standing.isPresent()) return standing;
        for (Charter charter : charters.values()) if (charter.pos().equals(pos)) return Optional.of(charter);
        return Optional.empty();
    }

    public List<Link> linksOf(UUID charter) {
        return links.values().stream().filter(l -> l.joins(charter)).toList();
    }

    /** A link is open when nothing on its route is cut and both its charters stand. */
    public boolean live(Link link) {
        if (!link.open()) return false;
        Charter a = charters.get(link.a()), b = charters.get(link.b());
        return a != null && b != null && a.active() && b.active();
    }

    /** Tier of the best live link of a charter, or 0 with none. */
    public int bestTier(UUID charter) {
        int best = 0;
        for (Link link : linksOf(charter)) if (live(link)) best = Math.max(best, link.kind().tier());
        return best;
    }

    // ---------------------------------------------------------------- placing and taking down

    /** Why a charter cannot be posted at {@code pos}, or null when it can (spacing and the per-owner limit). */
    public @Nullable Component refusal(BlockPos pos, UUID owner) {
        int count = 0;
        double nearest = Double.MAX_VALUE;
        for (Charter charter : charters.values()) {
            if (!charter.active()) continue;
            if (charter.owner().equals(owner)) count++;
            nearest = Math.min(nearest, horizontal(pos, charter.pos()));
        }
        if (count >= Config.OUTPOSTS_MAX_CHARTERS.getAsInt()) {
            return Component.translatable(StrataIndustria.MOD_ID + ".outposts.too_many", Config.OUTPOSTS_MAX_CHARTERS.getAsInt());
        }
        int spacing = Config.OUTPOSTS_MIN_SPACING.getAsInt();
        if (nearest < spacing) {
            return Component.translatable(StrataIndustria.MOD_ID + ".outposts.too_close", (int) Math.ceil(nearest), spacing);
        }
        return null;
    }

    private static double horizontal(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Posts a charter. A post put back within {@link #REVIVE_RADIUS} blocks of the owner's taken-down one in time
     * is that charter again, with its name and links.
     */
    public Charter post(ServerLevel level, BlockPos pos, ServerPlayer owner, String requestedName) {
        String name = clean(requestedName);
        long now = level.getGameTime();
        for (Charter old : charters.values()) {
            if (old.active() || !old.owner().equals(owner.getUUID()) || now - old.brokenAt() > DORMANT_TICKS) continue;
            if (old.pos().distSqr(pos) > REVIVE_RADIUS * REVIVE_RADIUS) continue;
            Charter back = old.movedTo(pos, name.isEmpty() ? old.name() : name);
            charters.put(back.id(), back);
            changed(level);
            return back;
        }
        if (name.isEmpty()) name = autoName(level, pos);
        Charter charter = new Charter(UUID.randomUUID(), pos.immutable(), owner.getUUID(), owner.getGameProfile().name(), name, -1);
        charters.put(charter.id(), charter);
        changed(level);
        return charter;
    }

    /** "Granite 2": the rock under the post and the next free number. */
    private String autoName(ServerLevel level, BlockPos pos) {
        String base = "Outpost";
        for (int dy = 1; dy <= 24; dy++) {
            var rock = RockLookup.rawRock(level.getBlockState(pos.below(dy)));
            if (rock != null) {
                base = rock.id().substring(0, 1).toUpperCase(Locale.ROOT) + rock.id().substring(1);
                break;
            }
        }
        for (int n = 1; ; n++) {
            String candidate = base + " " + n;
            if (charters.values().stream().noneMatch(c -> c.name().equals(candidate))) return candidate;
        }
    }

    private static String clean(String name) {
        String trimmed = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        return trimmed.length() > MAX_NAME ? trimmed.substring(0, MAX_NAME) : trimmed;
    }

    public void rename(ServerLevel level, UUID id, String name) {
        Charter charter = charters.get(id);
        String clean = clean(name);
        if (charter == null || clean.isEmpty() || clean.equals(charter.name())) return;
        charters.put(id, charter.withName(clean));
        changed(level);
    }

    /** The post at {@code pos} is gone. Its links go dormant until it is posted again nearby or the time runs out. */
    public void take(ServerLevel level, BlockPos pos) {
        charterAt(pos).ifPresent(charter -> {
            charters.put(charter.id(), charter.brokenAt(level.getGameTime()));
            changed(level);
        });
    }

    /** Forgets taken-down charters that nobody put back in time, with their links. */
    public void expire(ServerLevel level) {
        expire(level, level.getGameTime());
    }

    public void expire(ServerLevel level, long now) {
        List<UUID> gone = new ArrayList<>();
        for (Charter charter : charters.values()) if (!charter.active() && now - charter.brokenAt() > DORMANT_TICKS) gone.add(charter.id());
        if (gone.isEmpty()) return;
        for (UUID id : gone) {
            charters.remove(id);
            for (Link link : linksOf(id)) removeLink(link.id());
        }
        changed(level);
    }

    /** Removes every charter of an owner and the links they hold. For tests and for server tools. */
    public void clearOwner(ServerLevel level, UUID owner) {
        for (Charter charter : List.copyOf(charters.values())) {
            if (!charter.owner().equals(owner)) continue;
            for (Link link : linksOf(charter.id())) removeLink(link.id());
            charters.remove(charter.id());
        }
        changed(level);
    }

    // ---------------------------------------------------------------- links

    /** The result of {@link #prove}: the link, or the reason there is none. */
    public record Proof(@Nullable Link link, @Nullable Component refusal) {}

    /**
     * Records the first completed trip between two stations as a link. A trip over the same pair and kind
     * replaces a cut link and refreshes an open one. A link that would join a district past the limit is refused.
     */
    public Proof prove(ServerLevel level, UUID a, UUID b, LinkKind kind, List<BlockPos> route) {
        Charter ca = charters.get(a), cb = charters.get(b);
        if (ca == null || cb == null || !ca.active() || !cb.active() || a.equals(b) || route.isEmpty()) {
            return new Proof(null, Component.translatable(StrataIndustria.MOD_ID + ".outposts.no_charter"));
        }
        List<BlockPos> blocks = route.stream().map(BlockPos::immutable).toList();
        LongSet wanted = keys(blocks);
        long now = level.getGameTime();
        for (Link old : linksOf(a)) {
            if (!old.joins(b) || old.kind() != kind) continue;
            if (old.open() && routeSets.get(old.id()).equals(wanted)) {
                Link fresh = old.withTraffic(now);
                links.put(fresh.id(), fresh);
                setDirty();
                return new Proof(fresh, null);
            }
            if (!old.open()) removeLink(old.id());
        }
        if (district(a, b).size() > Config.OUTPOSTS_MAX_DISTRICT.getAsInt()) {
            return new Proof(null, Component.translatable(StrataIndustria.MOD_ID + ".outposts.district_full", Config.OUTPOSTS_MAX_DISTRICT.getAsInt()));
        }
        boolean first = bestTier(a) == 0 || bestTier(b) == 0;
        Link link = new Link(UUID.randomUUID(), a, b, kind, blocks, Optional.empty(), now);
        addLink(link);
        changed(level);
        if (first) {
            Journal.awardOwners(level, ca, Journal.CHARTER_LINKED);
            Journal.awardOwners(level, cb, Journal.CHARTER_LINKED);
        }
        tell(level, Component.translatable(StrataIndustria.MOD_ID + ".outposts.opened", Component.translatable(kind.langKey()),
                ca.name(), cb.name()), null, ca, cb);
        return new Proof(link, null);
    }

    /** Notes that something travelled the link, for "last traffic" on the charter board. */
    public void traffic(ServerLevel level, UUID linkId) {
        Link link = links.get(linkId);
        if (link == null) return;
        links.put(linkId, link.withTraffic(level.getGameTime()));
        setDirty();
    }

    /** Charters that one more link between {@code a} and {@code b} would put in one district. */
    private Set<UUID> district(UUID a, UUID b) {
        Set<UUID> seen = new HashSet<>();
        List<UUID> open = new ArrayList<>(List.of(a, b));
        while (!open.isEmpty()) {
            UUID next = open.remove(open.size() - 1);
            if (!seen.add(next)) continue;
            for (Link link : linksOf(next)) if (live(link)) open.add(link.other(next));
        }
        return seen;
    }

    /**
     * A block of some route was removed or replaced: every open link running over {@code pos} is cut there.
     * Called from the removal hook of every track, tower, span and pipeline block this spec adds.
     */
    public void cut(ServerLevel level, BlockPos pos) {
        Set<UUID> here = linksByChunk.get(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        if (here == null) return;
        boolean any = false;
        for (UUID id : List.copyOf(here)) {
            Link link = links.get(id);
            if (link == null || !link.open() || !routeSets.get(id).contains(pos.asLong())) continue;
            links.put(id, link.cut(pos.immutable()));
            any = true;
            Component line = Component.translatable(StrataIndustria.MOD_ID + ".outposts.cut", Component.translatable(link.kind().langKey()),
                    name(link.a()), name(link.b()), pos.getX() + " " + pos.getY() + " " + pos.getZ());
            tell(level, line, TransportSounds.CHARTER_LINE_CUT.get(), charters.get(link.a()), charters.get(link.b()));
        }
        if (any) changed(level);
    }

    public String name(UUID charter) {
        Charter c = charters.get(charter);
        return c == null ? "?" : c.name();
    }

    /** Tells every online owner (and team mate) of the given charters once. */
    private void tell(ServerLevel level, Component line, net.minecraft.sounds.@Nullable SoundEvent sound, @Nullable Charter... ofCharters) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            boolean owner = false;
            for (Charter charter : ofCharters) owner |= charter != null && OutpostPlan.isOwner(level, player, charter);
            if (!owner) continue;
            player.sendSystemMessage(line);
            if (sound != null) player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.7f, 1f);
        }
    }

    private void addLink(Link link) {
        links.put(link.id(), link);
        LongSet keys = keys(link.route());
        routeSets.put(link.id(), keys);
        for (BlockPos pos : link.route()) {
            linksByChunk.computeIfAbsent(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4), k -> new HashSet<>()).add(link.id());
        }
    }

    private void removeLink(UUID id) {
        Link link = links.remove(id);
        routeSets.remove(id);
        if (link == null) return;
        for (BlockPos pos : link.route()) {
            Set<UUID> set = linksByChunk.get(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
            if (set != null) set.remove(id);
        }
    }

    private static LongSet keys(List<BlockPos> blocks) {
        LongSet keys = new LongOpenHashSet();
        for (BlockPos pos : blocks) keys.add(pos.asLong());
        return keys;
    }

    // ---------------------------------------------------------------- plan

    private void changed(ServerLevel level) {
        setDirty();
        plan = null;
        OutpostTickets.refresh(level);
    }

    /** What every charter loads right now, as last worked out by {@link OutpostTickets#refresh}. */
    public OutpostPlan plan(ServerLevel level) {
        if (plan == null) plan = OutpostPlan.compute(level, this, p -> true);
        return plan;
    }

    void setPlan(OutpostPlan plan) {
        this.plan = plan;
    }
}
