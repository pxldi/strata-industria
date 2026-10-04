package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.ledger.BuilderCrateBlockEntity;
import dev.strataindustria.ledger.LedgerEvents;
import dev.strataindustria.ledger.LedgerRegistry;
import dev.strataindustria.ledger.Ledgers;
import dev.strataindustria.ledger.Plan;
import dev.strataindustria.ledger.Stamping;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Builder's ledger tests (uniqueness 2.5), run by {@link ModGameTests}. */
final class LedgerGameTests {
    private LedgerGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ledger_enter_and_stamp", LedgerGameTests::enterAndStamp);
        tests.put("ledger_stamps_every_plan", LedgerGameTests::everyPlan);
        tests.put("ledger_unknown_and_short", LedgerGameTests::unknownAndShort);
    }

    private static FakePlayer builder(ServerLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), "builder"));
    }

    private static BlockPos place(ServerLevel level, Plan plan, BlockPos at) {
        BlockState state = plan.controller().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        level.setBlock(at, state, Block.UPDATE_ALL);
        return at;
    }

    /** What one standard build of {@code plan} takes, by item. */
    private static Map<Item, Integer> needs(Plan plan, BlockPos controller) {
        Map<Item, Integer> needs = new LinkedHashMap<>();
        for (Plan.Part part : plan.parts(controller, Direction.NORTH)) needs.merge(part.state().getBlock().asItem(), 1, Integer::sum);
        return needs;
    }

    private static BuilderCrateBlockEntity stock(ServerLevel level, BlockPos crate, Map<Item, Integer> items) {
        level.setBlock(crate, LedgerRegistry.BUILDERS_CRATE.get().defaultBlockState(), Block.UPDATE_ALL);
        var entity = (BuilderCrateBlockEntity) level.getBlockEntity(crate);
        int slot = 0;
        for (var entry : items.entrySet()) {
            int left = entry.getValue();
            while (left > 0) {
                int count = Math.min(left, 64);
                entity.setItem(slot++, new ItemStack(entry.getKey(), count));
                left -= count;
            }
        }
        return entity;
    }

    private static int held(BuilderCrateBlockEntity crate) {
        int total = 0;
        for (int slot = 0; slot < crate.getContainerSize(); slot++) total += crate.getItem(slot).getCount();
        return total;
    }

    // A finished oven is written in the ledger once; after that a gutted one is rebuilt from the crate, one block per step.
    private static void enterAndStamp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = builder(level);
        BlockPos door = place(level, Plan.COKE_OVEN, helper.absolutePos(new BlockPos(4, 2, 5)));
        Plan.COKE_OVEN.parts(door, Direction.NORTH).forEach(part -> level.setBlock(part.pos(), part.state(), Block.UPDATE_ALL));
        BlockState state = level.getBlockState(door);
        helper.assertTrue(Plan.COKE_OVEN.complete(level, door, Direction.NORTH), "the hand-built oven is whole");
        helper.assertTrue(!Ledgers.knows(player, Plan.COKE_OVEN), "nothing entered yet");
        LedgerEvents.use(player, Plan.COKE_OVEN, door, state);
        helper.assertTrue(Ledgers.knows(player, Plan.COKE_OVEN), "a whole oven is entered");
        helper.assertTrue(!Ledgers.knows(player, Plan.BLAST_FURNACE), "only that one");

        Map<Item, Integer> needs = needs(Plan.COKE_OVEN, door);
        Plan.COKE_OVEN.parts(door, Direction.NORTH).forEach(part -> level.removeBlock(part.pos(), false));
        helper.assertTrue(!Plan.COKE_OVEN.complete(level, door, Direction.NORTH), "gutted");
        BuilderCrateBlockEntity crate = stock(level, helper.absolutePos(new BlockPos(1, 2, 5)), needs);
        int before = held(crate);

        LedgerEvents.use(player, Plan.COKE_OVEN, door, state);
        helper.assertTrue(Stamping.active(level, door), "stamping has started");
        // Not every tick: it takes a few steps, not one.
        helper.assertTrue(!Plan.COKE_OVEN.complete(level, door, Direction.NORTH), "nothing is placed in the same tick");
        Stamping job = Stamping.start(player, Plan.COKE_OVEN, door, Direction.NORTH);
        helper.assertTrue(job == null, "a second start is refused while one runs");
        helper.runAfterDelay(4L * 40, () -> {
            helper.assertTrue(Plan.COKE_OVEN.complete(level, door, Direction.NORTH), "the oven is whole again");
            helper.assertValueEqual(held(crate), 0, "every brick came out of the crate (was " + before + ")");
            helper.assertTrue(!Stamping.active(level, door), "the job is over");
            helper.succeed();
        });
    }

    // Every plan the ledger knows places a build that its own structure check accepts, and uses what it needs.
    private static void everyPlan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = builder(level);
        int index = 0;
        for (Plan plan : Plan.values()) {
            // One row of the test area per plan would not fit five layers, so run them in turn at the same spot.
            BlockPos controller = place(level, plan, helper.absolutePos(new BlockPos(4, plan == Plan.STEEL_BOILER ? 1 : plan == Plan.COKE_OVEN ? 2 : 0, 5)));
            Ledgers.learn(player, plan);
            Map<Item, Integer> needs = needs(plan, controller);
            // The hearth sits in the test floor, so dig it out first.
            plan.parts(controller, Direction.NORTH).forEach(part -> level.removeBlock(part.pos(), false));
            BuilderCrateBlockEntity crate = stock(level, helper.absolutePos(new BlockPos(0, 0, 0)), needs);
            Stamping job = Stamping.start(player, plan, controller, Direction.NORTH);
            helper.assertTrue(job != null, plan + " starts");
            int placed = job.runToEnd();
            helper.assertTrue(plan.complete(level, controller, Direction.NORTH), plan + " should be whole after stamping, placed " + placed + ", missing " + plan.parts(controller, Direction.NORTH).stream()
                    .filter(part -> !level.getBlockState(part.pos()).is(part.state().getBlock())).map(part -> part.pos().subtract(controller) + "=" + level.getBlockState(part.pos()).getBlock()).toList());
            helper.assertValueEqual(held(crate), 0, plan + " leaves nothing over");
            // Clear it for the next plan.
            plan.parts(controller, Direction.NORTH).forEach(part -> level.removeBlock(part.pos(), false));
            level.removeBlock(controller, false);
            index++;
        }
        helper.assertValueEqual(index, Plan.values().length, "all plans run");
        helper.succeed();
    }

    // Without an entry the ledger builds nothing; with one but too few blocks it builds what it can and stops.
    private static void unknownAndShort(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = builder(level);
        BlockPos door = place(level, Plan.COKE_OVEN, helper.absolutePos(new BlockPos(4, 2, 5)));
        stock(level, helper.absolutePos(new BlockPos(1, 2, 5)), needs(Plan.COKE_OVEN, door));
        LedgerEvents.use(player, Plan.COKE_OVEN, door, level.getBlockState(door));
        helper.assertTrue(!Stamping.active(level, door), "no entry, no stamping");

        Ledgers.learn(player, Plan.COKE_OVEN);
        Map<Item, Integer> few = new LinkedHashMap<>();
        few.put(needs(Plan.COKE_OVEN, door).keySet().iterator().next(), 5);
        BuilderCrateBlockEntity crate = stock(level, helper.absolutePos(new BlockPos(1, 2, 5)), few);
        Stamping job = Stamping.start(player, Plan.COKE_OVEN, door, Direction.NORTH);
        helper.assertTrue(job != null, "starts with an entry");
        helper.assertValueEqual(job.runToEnd(), 5, "places what the crate holds");
        helper.assertValueEqual(held(crate), 0, "and empties it");
        helper.assertTrue(!Plan.COKE_OVEN.complete(level, door, Direction.NORTH), "the oven is not whole");
        helper.succeed();
    }
}
