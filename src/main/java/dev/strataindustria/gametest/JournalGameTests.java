package dev.strataindustria.gametest;

import dev.strataindustria.journal.Journal;
import dev.strataindustria.journal.JournalContent;
import dev.strataindustria.journal.JournalState;
import dev.strataindustria.journal.Leads;
import dev.strataindustria.journal.Observations;
import dev.strataindustria.journal.Study;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Leads notebook tests (journal leads spec, "Tests"), run by {@link ModGameTests}. */
final class JournalGameTests {
    private JournalGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("journal_lead_lifecycle", JournalGameTests::leadLifecycle);
        tests.put("journal_observation", JournalGameTests::observation);
        tests.put("journal_study", JournalGameTests::study);
        tests.put("journal_never_stuck", JournalGameTests::neverStuck);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Leads.refresh(player);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static void award(ServerPlayer player, String path) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Journal.goal(path));
        for (String criterion : holder.value().criteria().keySet()) player.getAdvancements().award(holder, criterion);
    }

    private static JournalState.Lead lead(GameTestHelper helper, ServerPlayer player, String path) {
        JournalState.Lead lead = JournalContent.state(player).lead(path);
        helper.assertTrue(lead != null, "lead " + path + " is in the notebook");
        return lead;
    }

    // A lead opens when its parent is done and closes with its own goal; its children open in turn.
    private static void leadLifecycle(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(lead(helper, player, "t0/loose_rock").open(), "the first lead is open from the start");
        helper.assertTrue(lead(helper, player, "t0/knap").openedSeq() < 0, "a lead under an unfinished goal is not open yet");
        award(player, "t0/loose_rock");
        helper.assertTrue(lead(helper, player, "t0/loose_rock").closed(), "the lead closed with its goal");
        helper.assertTrue(lead(helper, player, "t0/knap").open(), "the next lead opened");
        helper.assertTrue(lead(helper, player, "t1/nugget").open(), "a sibling branch opened too");
        helper.assertTrue(!lead(helper, player, "t0/knap").hinted(), "a fresh lead has no hint yet");
        done(helper, player);
    }

    // An observation writes one note the first time only, and opens the lead it raises.
    private static void observation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(lead(helper, player, "t1/clay_forming").openedSeq() < 0, "clay forming is not open at the start");
        player.getInventory().add(new ItemStack(Items.CLAY_BALL));
        Observations.scan(player);
        JournalState state = JournalContent.state(player);
        int notes = state.notes().size();
        helper.assertTrue(notes == 1, "one note written, found " + notes);
        helper.assertTrue(state.notes().getFirst().key().endsWith("observe.clay"), "the note is the clay observation");
        helper.assertTrue(lead(helper, player, "t1/clay_forming").open(), "the observation opened clay forming early");
        Observations.scan(player);
        helper.assertTrue(state.notes().size() == notes, "the observation fires once");
        done(helper, player);
    }

    // Studying a block that belongs to a lead brings its hint to mind, and studying it again writes nothing new.
    private static void study(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos clay = new BlockPos(1, 1, 1);
        helper.setBlock(clay, Blocks.CLAY);
        helper.assertTrue(!lead(helper, player, "t0/clay").hinted(), "no hint before studying");
        helper.assertTrue(Study.study(player, helper.absolutePos(clay)), "the first study did something");
        JournalState.Lead lead = lead(helper, player, "t0/clay");
        helper.assertTrue(lead.open() && lead.hinted(), "studying clay opened and hinted the clay lead");
        int notes = JournalContent.state(player).notes().size();
        helper.assertTrue(notes == 1, "one study note, found " + notes);
        helper.assertTrue(!Study.study(player, helper.absolutePos(clay)), "a second study adds nothing");
        helper.assertTrue(JournalContent.state(player).notes().size() == notes, "no second note");
        done(helper, player);
    }

    // Working through the whole tree, there is always an open lead until every goal is done: the journal never
    // leaves the player without a next step (anti-tedium rule 6).
    private static void neverStuck(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        JournalState state = JournalContent.state(player);
        int total = state.leads().size();
        helper.assertTrue(total > 60, "the notebook knows every goal, found " + total);
        for (int i = 0; i < total; i++) {
            List<JournalState.Lead> open = Leads.open(state);
            boolean allClosed = state.leads().values().stream().allMatch(JournalState.Lead::closed);
            if (allClosed) break;
            helper.assertTrue(!open.isEmpty(), "an open lead while goals remain, after " + i + " goals");
            award(player, open.getFirst().path());
        }
        helper.assertTrue(state.leads().values().stream().allMatch(JournalState.Lead::closed), "every lead closed");
        done(helper, player);
    }
}
