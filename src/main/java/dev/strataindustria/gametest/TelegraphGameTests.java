package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.electric.PoleInsulatorBlockEntity;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.Link;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.telegraph.DispatchBoardBlock;
import dev.strataindustria.transport.telegraph.DispatchBoardBlockEntity;
import dev.strataindustria.transport.telegraph.DispatchView;
import dev.strataindustria.transport.telegraph.TelegraphIndex;
import dev.strataindustria.transport.telegraph.TelegraphKeyBlock;
import dev.strataindustria.transport.telegraph.TelegraphKeyBlockEntity;
import dev.strataindustria.transport.telegraph.TelegraphLine;
import dev.strataindustria.transport.telegraph.TelegraphRegistry;
import dev.strataindustria.transport.telegraph.TelegraphSounderBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Telegraph and dispatch board tests (outposts spec 9.1 and 15, chunk O10), run by {@link ModGameTests}. */
final class TelegraphGameTests {
    private TelegraphGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("telegraph_wire_rules", TelegraphGameTests::wireRules);
        tests.put("telegraph_wire_item", TelegraphGameTests::wireItem);
        tests.put("telegraph_call", TelegraphGameTests::call);
        tests.put("telegraph_key_redstone", TelegraphGameTests::keyRedstone);
        tests.put("telegraph_line_cut", TelegraphGameTests::lineCut);
        tests.put("telegraph_drop_attach", TelegraphGameTests::dropAttach);
        tests.put("dispatch_board_reports", TelegraphGameTests::boardReports);
    }

    // ---------------------------------------------------------------- setup

    /** A pole two blocks tall with an insulator on top, at the test's own (x, z); returns the insulator. */
    private static BlockPos pole(GameTestHelper helper, int x, int z) {
        ServerLevel level = helper.getLevel();
        BlockPos top = helper.absolutePos(new BlockPos(x, 3, z));
        level.setBlock(top.below(2), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(top.below(), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(top, Tier5Blocks.POLE_INSULATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        return top;
    }

    private static void wire(GameTestHelper helper, BlockPos a, BlockPos b) {
        helper.assertTrue(TelegraphLine.problem(helper.getLevel(), a, b) == null, "a clear span is accepted: " + TelegraphLine.problem(helper.getLevel(), a, b));
        TelegraphLine.connect(helper.getLevel(), a, b);
    }

    private static BlockPos key(GameTestHelper helper, int x, int z, Direction facing) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, z));
        helper.getLevel().setBlock(pos, TelegraphRegistry.KEY.get().defaultBlockState().setValue(TelegraphKeyBlock.FACING, facing), Block.UPDATE_ALL);
        return pos;
    }

    private static BlockPos sounder(GameTestHelper helper, int x, int z) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, z));
        helper.getLevel().setBlock(pos, TelegraphRegistry.SOUNDER.get().defaultBlockState(), Block.UPDATE_ALL);
        return pos;
    }

    /** Forgets what an earlier run left in the index around this test. */
    private static void fresh(GameTestHelper helper) {
        TelegraphIndex.get(helper.getLevel()).forget(helper.absolutePos(new BlockPos(6, 2, 6)), 24);
    }

    private static FakePlayer owner(ServerLevel level, String name) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
        OutpostPlan.ALSO_ONLINE.add(player.getUUID());
        return player;
    }

    /** Home at the west end of the row, a far charter at the east end; the telegraph runs between them over three poles. */
    private record Line(BlockPos a, BlockPos b, BlockPos c, Charter home, Charter far, FakePlayer owner) {}

    private static Line line(GameTestHelper helper, String owner) {
        fresh(helper);
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer who = owner(level, owner);
        Charter home = index.post(level, helper.absolutePos(new BlockPos(0, 2, 6)), who, "Home");
        Charter far = index.post(level, helper.absolutePos(new BlockPos(8, 2, 6)), who, "Far");
        BlockPos a = pole(helper, 1, 6), b = pole(helper, 4, 6), c = pole(helper, 7, 6);
        wire(helper, a, b);
        wire(helper, b, c);
        return new Line(a, b, c, home, far, who);
    }

    private static void done(GameTestHelper helper, Line line, BlockPos... clear) {
        RouteIndex.get(helper.getLevel()).clearOwner(helper.getLevel(), line.owner.getUUID());
        OutpostPlan.ALSO_ONLINE.remove(line.owner.getUUID());
        for (BlockPos pos : clear) helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean powered(ServerLevel level, BlockPos sounder) {
        return level.getBlockState(sounder).getValue(TelegraphSounderBlock.POWERED);
    }

    // ---------------------------------------------------------------- wire

    // Wire strings between insulators like a span, but keeps its own books: the power network never sees it, a solid
    // block refuses it, 40 blocks is too far, and a span costs one wire for every 8 blocks.
    private static void wireRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        fresh(helper);
        BlockPos a = pole(helper, 2, 1), b = pole(helper, 2, 7), far = pole(helper, 2, 1).offset(0, 0, 40);
        level.setBlock(far.below(2), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(far.below(), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(far, Tier5Blocks.POLE_INSULATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        try {
            helper.assertValueEqual(TelegraphLine.problem(level, a, a), "same", "not to itself");
            helper.assertValueEqual(TelegraphLine.problem(level, a, far), "too_far", "40 blocks is too far");
            helper.assertValueEqual(TelegraphLine.problem(level, a, a.below(2)), "not_insulator", "insulators only");
            BlockPos middle = a.offset(0, 0, 3);
            level.setBlock(middle, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertValueEqual(TelegraphLine.problem(level, a, b), "blocked", "a solid block refuses the span");
            level.setBlock(middle, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(TelegraphLine.problem(level, a, b) == null, "and a clear line is accepted");
            helper.assertValueEqual(TelegraphIndex.wireFor(a, b), 1, "6 blocks is one wire");
            helper.assertValueEqual(TelegraphIndex.wireFor(a, a.offset(0, 0, 9)), 2, "9 blocks is two");
            TelegraphLine.connect(level, a, b);
            helper.assertValueEqual(TelegraphLine.problem(level, a, b), "already", "not twice");
            helper.assertTrue(((PoleInsulatorBlockEntity) level.getBlockEntity(a)).wires().contains(b), "the near end lists it for the renderer");
            helper.assertTrue(((PoleInsulatorBlockEntity) level.getBlockEntity(b)).wires().contains(a), "and so does the far end");
            helper.assertTrue(((PoleInsulatorBlockEntity) level.getBlockEntity(a)).spans().isEmpty(), "the power spans are untouched");
            helper.assertTrue(TelegraphIndex.get(level).wired(a, b) && TelegraphIndex.get(level).wired(b, a), "the index has it both ways");
        } finally {
            for (BlockPos pos : List.of(far, far.below(), far.below(2))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            fresh(helper);
        }
        helper.succeed();
    }

    // Using the wire item on two insulators strings a span and uses up one wire per 8 blocks.
    private static void wireItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        fresh(helper);
        BlockPos a = pole(helper, 2, 1), b = pole(helper, 2, 7);
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "linesman"));
        ItemStack wire = new ItemStack(TelegraphRegistry.WIRE.get(), 3);
        try {
            use(level, player, wire, a);
            helper.assertTrue(wire.get(Tier5DataComponents.SPAN_LINK.get()) != null, "the first click makes the wire fast");
            helper.assertValueEqual(wire.getCount(), 3, "and costs nothing yet");
            use(level, player, wire, b);
            helper.assertTrue(TelegraphIndex.get(level).wired(a, b), "the second click strings it");
            helper.assertValueEqual(wire.getCount(), 2, "a 6 block span costs one wire");
            helper.assertTrue(wire.get(Tier5DataComponents.SPAN_LINK.get()) == null, "and the wire is let go");
        } finally {
            fresh(helper);
        }
        helper.succeed();
    }

    private static void use(ServerLevel level, FakePlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    // ---------------------------------------------------------------- key and sounder

    // A key at home and a sounder at the far charter, both hung on the line by their drop wires: a press clacks the
    // sounder (redstone for four ticks, then it lifts) and proves a telegraph link between the two charters.
    private static void call(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Line line = line(helper, "operator");
        BlockPos key = key(helper, 1, 4, Direction.EAST), sounder = sounder(helper, 7, 4);
        TelegraphIndex index = TelegraphIndex.get(level);
        helper.assertTrue(line.a.equals(index.dropOf(key)), "the key hangs on the nearest insulator: " + index.dropOf(key));
        helper.assertTrue(line.c.equals(index.dropOf(sounder)), "the sounder on the far one: " + index.dropOf(sounder));
        RouteIndex routes = RouteIndex.get(level);
        TelegraphKeyBlockEntity keyEntity = (TelegraphKeyBlockEntity) level.getBlockEntity(key);
        keyEntity.press(level, level.getBlockState(key), null);
        helper.assertTrue(level.getBlockState(key).getValue(TelegraphKeyBlock.PRESSED), "the lever is down");
        helper.assertTrue(powered(level, sounder), "the sounder clacked");
        helper.assertValueEqual(level.getBlockState(sounder).getSignal(level, sounder, Direction.UP), 15, "and gives redstone");
        List<Link> links = routes.linksOf(line.home.id());
        helper.assertTrue(links.size() == 1 && links.get(0).kind() == LinkKind.TELEGRAPH && links.get(0).open() && links.get(0).joins(line.far.id()),
                "a telegraph link joins the two charters: " + links);
        helper.assertValueEqual(routes.bestTier(line.home.id()), 5, "the telegraph is a tier five link");
        List<BlockPos> route = links.get(0).route();
        helper.assertTrue(route.contains(key) && route.contains(line.a) && route.contains(line.b) && route.contains(line.c) && route.contains(sounder),
                "the route is key, poles and sounder: " + route);
        helper.runAfterDelay(8, () -> {
            try {
                helper.assertTrue(!level.getBlockState(key).getValue(TelegraphKeyBlock.PRESSED), "the lever is back up");
                helper.assertTrue(!powered(level, sounder), "the sounder lifted again");
                helper.assertValueEqual(level.getBlockState(sounder).getSignal(level, sounder, Direction.UP), 0, "and the redstone is gone");
                helper.succeed();
            } finally {
                done(helper, line, key, sounder);
            }
        });
    }

    // Redstone into the key presses it, once per rising edge.
    private static void keyRedstone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Line line = line(helper, "relay");
        BlockPos key = key(helper, 1, 4, Direction.EAST), sounder = sounder(helper, 7, 4);
        BlockPos block = key.north();
        helper.assertTrue(!powered(level, sounder), "quiet to begin with");
        level.setBlock(block, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(powered(level, sounder), "a redstone block beside the key sends");
        helper.runAfterDelay(8, () -> {
            try {
                helper.assertTrue(!powered(level, sounder), "one clack, then it lifts");
                helper.succeed();
            } finally {
                done(helper, line, key, sounder, block);
            }
        });
    }

    // A device with no pole in reach is not on the line; an insulator put up later takes it, and a pole too far away does not.
    private static void dropAttach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        fresh(helper);
        BlockPos far = pole(helper, 2, 12);
        BlockPos key = key(helper, 2, 2, Direction.EAST);
        TelegraphIndex index = TelegraphIndex.get(level);
        helper.assertTrue(index.dropOf(key) == null, "the nearest pole is 10 blocks off, out of reach");
        TelegraphLine.Call none = TelegraphLine.send(level, key, null);
        helper.assertTrue(!none.attached(), "a press with no pole says so");
        BlockPos near = pole(helper, 2, 8);
        helper.assertTrue(near.equals(index.dropOf(key)), "a pole put up within 8 blocks takes the key: " + index.dropOf(key));
        TelegraphLine.Call quiet = TelegraphLine.send(level, key, null);
        helper.assertTrue(quiet.attached() && quiet.sounders() == 0, "and the press goes out to an empty line");
        for (BlockPos pos : List.of(key, far, far.below(), far.below(2), near, near.below(), near.below(2))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        fresh(helper);
        helper.succeed();
    }

    // Breaking an insulator in the middle cuts the telegraph link, frees the wire on both sides and drops it.
    private static void lineCut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Line line = line(helper, "linesman");
        BlockPos key = key(helper, 1, 4, Direction.EAST), sounder = sounder(helper, 7, 4);
        ((TelegraphKeyBlockEntity) level.getBlockEntity(key)).press(level, level.getBlockState(key), null);
        RouteIndex routes = RouteIndex.get(level);
        helper.assertTrue(routes.linksOf(line.home.id()).get(0).open(), "proven first");
        level.destroyBlock(line.b, true);
        Link link = routes.linksOf(line.home.id()).get(0);
        helper.assertTrue(!link.open() && link.cutAt().isPresent() && link.cutAt().get().equals(line.b), "cut at the broken insulator: " + link.cutAt());
        TelegraphIndex index = TelegraphIndex.get(level);
        helper.assertTrue(!index.wired(line.a, line.b) && !index.wired(line.c, line.b), "its wires are gone from the index");
        helper.assertTrue(((PoleInsulatorBlockEntity) level.getBlockEntity(line.a)).wires().isEmpty(), "and from the neighbour's end");
        int wire = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(4))) {
            if (item.getItem().is(TelegraphRegistry.WIRE.get())) wire += item.getItem().getCount();
        }
        helper.assertValueEqual(wire, 2, "the wire of both spans drops");
        helper.runAfterDelay(8, () -> {
            try {
                helper.assertTrue(!powered(level, sounder), "the sounder lifts");
                TelegraphLine.Call call = TelegraphLine.send(level, key, null);
                helper.assertValueEqual(call.sounders(), 0, "a call into a cut line reaches nobody");
                helper.succeed();
            } finally {
                done(helper, line, key, sounder, line.a, line.c);
            }
        });
    }

    // ---------------------------------------------------------------- dispatch board

    // Keys at both charters report to a board at home: the fill of the crate each faces, how the charter stands and its
    // best line. The board's chalk rows follow, and a charter that stops reporting would show as silent.
    private static void boardReports(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Line line = line(helper, "dispatcher");
        RouteIndex routes = RouteIndex.get(level);
        List<BlockPos> run = new ArrayList<>();
        for (int i = 0; i < 10; i++) run.add(line.home.pos().east(i));
        routes.prove(level, line.home.id(), line.far.id(), LinkKind.TRAMWAY, run);
        BlockPos homeKey = key(helper, 1, 4, Direction.NORTH), farKey = key(helper, 7, 4, Direction.EAST);
        BlockPos crate = farKey.east();
        level.setBlock(crate, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(crate);
        for (int slot = 0; slot < 9; slot++) chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        BlockPos board = helper.absolutePos(new BlockPos(4, 2, 3));
        level.setBlock(board.north(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(board, TelegraphRegistry.BOARD.get().defaultBlockState().setValue(DispatchBoardBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(board).is(TelegraphRegistry.BOARD.get()), "the board hangs on the wall");
        helper.assertTrue(line.b.equals(TelegraphIndex.get(level).dropOf(board)), "and on the line, at the nearest pole: " + TelegraphIndex.get(level).dropOf(board));
        helper.assertTrue(DispatchView.of(level, board).lines().isEmpty(), "nobody has reported yet");
        helper.runAfterDelay(150, () -> {
            try {
                DispatchView view = DispatchView.of(level, board);
                helper.assertTrue(view.onLine(), "the board is on a line");
                helper.assertValueEqual(view.lines().size(), 2, "two outposts report: " + view.lines());
                DispatchView.Line far = view.lines().stream().filter(l -> l.name().equals("Far")).findFirst().orElseThrow();
                DispatchView.Line home = view.lines().stream().filter(l -> l.name().equals("Home")).findFirst().orElseThrow();
                helper.assertValueEqual(far.fill(), 33, "nine full slots of 27 read a third");
                helper.assertValueEqual(home.fill(), -1, "a key facing no crate reports no fill");
                helper.assertValueEqual(far.state(), 0, "a charter with fresh traffic is loaded");
                helper.assertValueEqual(far.line(), 0, "its line is open");
                helper.assertValueEqual(far.kind(), LinkKind.TRAMWAY.langKey(), "and it is the tramway");
                helper.assertTrue(far.trafficAgo() >= 0 && far.trafficAgo() < 400, "last traffic is recent: " + far.trafficAgo());
                helper.assertTrue(!far.silent(), "and the report is fresh");
                DispatchBoardBlockEntity chalk = (DispatchBoardBlockEntity) level.getBlockEntity(board);
                helper.assertValueEqual(chalk.rows().size(), 2, "the slate has two rows chalked");
                helper.assertValueEqual(chalk.rows().get(0).name(), "Far", "in name order");
                helper.succeed();
            } finally {
                done(helper, line, homeKey, farKey, crate, board, board.north());
            }
        });
    }
}
