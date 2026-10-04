package dev.strataindustria.journal;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The leads notebook's bookkeeping (journal leads spec). A lead is a journal goal; it opens when its parent goal
 * is done or an observation reveals it, gets its hint once it has stayed open a while or was studied, and closes
 * with its advancement. Everything here runs on the server and syncs the player's {@link JournalState}.
 */
public final class Leads {
    /** Journal goals are "journal/t0/..." to "journal/t9/..."; places and the root are not leads. */
    static final Pattern LEAD = Pattern.compile("journal/t\\d+/.+");
    static final String ROOT_PATH = "journal/root";

    private Leads() {}

    public static boolean isLead(Identifier id) {
        return id.getNamespace().equals(StrataIndustria.MOD_ID) && LEAD.matcher(id.getPath()).matches();
    }

    static String path(Identifier id) {
        return id.getPath().substring("journal/".length());
    }

    /** The in-game day a note is dated with, counting from day 1. */
    static long day(Level level) {
        return level.getOverworldClockTime() / 24000L + 1;
    }

    static long playTime(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
    }

    /**
     * Brings the notebook in line with the player's advancements: closes leads whose goal is done and opens leads
     * whose parent is. Returns true if anything changed (and was synced).
     */
    public static boolean refresh(ServerPlayer player) {
        JournalState state = JournalContent.state(player);
        var advancements = player.level().getServer().getAdvancements();
        long now = playTime(player);
        long day = day(player.level());
        boolean changed = false;
        for (AdvancementNode node : advancements.tree().nodes()) {
            AdvancementHolder holder = node.holder();
            if (!isLead(holder.id()) || node.advancement().display().isEmpty()) continue;
            String path = path(holder.id());
            JournalState.Lead lead = state.lead(path);
            JournalState.Lead fresh = describe(node, lead);
            if (!fresh.equals(lead)) {
                lead = fresh;
                state.put(lead);
                changed = true;
            }
            if (lead.closed()) continue;
            if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
                state.put(lead.closed(state.next(), day));
                changed = true;
            } else if (lead.openedSeq() < 0 && parentDone(player, node)) {
                state.put(lead.opened(now, state.next()));
                changed = true;
            }
        }
        if (changed) player.syncData(JournalContent.STATE);
        return changed;
    }

    /** The lead as the tree describes it now, keeping the player's progress on it. */
    private static JournalState.Lead describe(AdvancementNode node, JournalState.Lead old) {
        DisplayInfo display = node.advancement().display().get();
        AdvancementNode parent = node.parent();
        String parentPath = parent != null && isLead(parent.holder().id()) ? path(parent.holder().id()) : "";
        Identifier icon = BuiltInRegistries.ITEM.getKey(display.icon().item().value());
        int x = Math.round(node.x() * 28), y = Math.round(node.y() * 27);
        if (old == null) return new JournalState.Lead(path(node.holder().id()), parentPath, icon, x, y, 0, -1, -1, -1, 0);
        return new JournalState.Lead(old.path(), parentPath, icon, x, y, old.openedAt(), old.openedSeq(), old.hintedSeq(),
                old.closedSeq(), old.closedDay());
    }

    private static boolean parentDone(ServerPlayer player, AdvancementNode node) {
        AdvancementNode parent = node.parent();
        if (parent == null || parent.holder().id().getPath().equals(ROOT_PATH)) return true;
        return player.getAdvancements().getOrStartProgress(parent.holder()).isDone();
    }

    /** Opens a lead ahead of its place in the tree, because the player stumbled on something that raises it. */
    public static boolean reveal(ServerPlayer player, String path) {
        JournalState state = JournalContent.state(player);
        JournalState.Lead lead = state.lead(path);
        if (lead == null || lead.openedSeq() >= 0) return false;
        state.put(lead.opened(playTime(player), state.next()));
        player.syncData(JournalContent.STATE);
        return true;
    }

    /** Adds the lead's hint: the player studied something that belongs to it. Opens it first if needed. */
    public static boolean hint(ServerPlayer player, String path) {
        JournalState state = JournalContent.state(player);
        JournalState.Lead lead = state.lead(path);
        if (lead == null || lead.closed() || lead.hinted()) return false;
        if (lead.openedSeq() < 0) lead = lead.opened(playTime(player), state.next());
        state.put(lead.hinted(state.next()));
        player.syncData(JournalContent.STATE);
        return true;
    }

    /** Hints every lead that has been open longer than the configured time; the character remembers something. */
    public static void remember(ServerPlayer player) {
        int minutes = Config.JOURNAL_HINT_MINUTES.get();
        if (minutes < 0) return;
        JournalState state = JournalContent.state(player);
        long now = playTime(player);
        boolean changed = false;
        for (JournalState.Lead lead : List.copyOf(state.leads().values())) {
            if (lead.open() && !lead.hinted() && now - lead.openedAt() >= minutes * 1200L) {
                state.put(lead.hinted(state.next()));
                changed = true;
            }
        }
        if (changed) player.syncData(JournalContent.STATE);
    }

    /** Writes a note. Each argument is a lang key shown in place of one {@code %s}. */
    public static void note(ServerPlayer player, String key, List<String> args, Identifier icon, boolean quiet) {
        JournalState state = JournalContent.state(player);
        state.add(new JournalState.Note(key, List.copyOf(args), Optional.ofNullable(icon), day(player.level()), state.next(), quiet));
        player.syncData(JournalContent.STATE);
    }

    /**
     * An observation: a note written the first time something happens, which may also open a lead early.
     * Returns false if this player had already made it.
     */
    public static boolean observe(ServerPlayer player, String id, String key, List<String> args, Identifier icon, String reveals) {
        JournalState state = JournalContent.state(player);
        if (!state.see(id)) return false;
        note(player, key, args, icon, false);
        if (reveals != null) {
            JournalState.Lead lead = state.lead(reveals);
            if (lead != null && !lead.closed()) reveal(player, reveals);
        }
        return true;
    }

    /** An observation for a block event with nobody's hand on it, made by everyone close enough to see it. */
    public static void observeNear(Level level, BlockPos pos, String id, String key, List<String> args, Identifier icon,
            String reveals) {
        if (!(level instanceof ServerLevel server)) return;
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(Journal.NEARBY))) {
            observe(player, id, key, args, icon, reveals);
        }
    }

    /** Open leads, in the order they were opened. */
    public static List<JournalState.Lead> open(JournalState state) {
        List<JournalState.Lead> open = new ArrayList<>();
        for (JournalState.Lead lead : state.leads().values()) {
            if (lead.open()) open.add(lead);
        }
        open.sort((a, b) -> Integer.compare(a.openedSeq(), b.openedSeq()));
        return open;
    }
}
