package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.registry.TransportBlocks;
import dev.strataindustria.registry.TransportDataComponents;
import dev.strataindustria.transport.outpost.BoardView;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.Link;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.OutpostCharterBlock;
import dev.strataindustria.transport.outpost.OutpostPayloads;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.OutpostTickets;
import dev.strataindustria.transport.outpost.RouteIndex;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Outpost charter tests (outposts spec 15, chunk O1), run by {@link ModGameTests}. */
final class OutpostGameTests {
    private OutpostGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("outpost_spacing", OutpostGameTests::spacing);
        tests.put("outpost_route_proven", OutpostGameTests::routeProven);
        tests.put("outpost_route_cut", OutpostGameTests::routeCut);
        tests.put("outpost_prove_refused", OutpostGameTests::proveRefused);
        tests.put("outpost_budget", OutpostGameTests::budget);
        tests.put("outpost_owner_away", OutpostGameTests::ownerAway);
        tests.put("outpost_dormant_revive", OutpostGameTests::dormantRevive);
        tests.put("outpost_charter_block", OutpostGameTests::charterBlock);
        tests.put("outpost_board_view", OutpostGameTests::boardView);
    }

    private static FakePlayer owner(ServerLevel level, String name) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
    }

    /** A straight run of track-like positions from {@code from} east for {@code length} blocks. */
    private static List<BlockPos> run(BlockPos from, int length) {
        List<BlockPos> route = new ArrayList<>();
        for (int i = 0; i < length; i++) route.add(from.east(i));
        return route;
    }

    private static Link link(ServerLevel level, RouteIndex index, Charter a, Charter b, LinkKind kind) {
        RouteIndex.Proof proof = index.prove(level, a.id(), b.id(), kind, run(a.pos(), (int) Math.max(1, Math.abs(b.pos().getX() - a.pos().getX()))));
        return proof.link();
    }

    // A second charter inside the spacing is refused, as is one over the owner's limit; far enough is fine.
    private static void spacing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "surveyor");
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, 4000);
        try {
            index.post(level, base, owner, "Millhouse");
            helper.assertTrue(index.refusal(base.east(100), UUID.randomUUID()) != null, "100 blocks is too close, whoever asks");
            helper.assertTrue(index.refusal(base.east(159), owner.getUUID()) != null, "159 blocks is still too close");
            helper.assertTrue(index.refusal(base.east(161), owner.getUUID()) == null, "161 blocks is far enough");
            for (int i = 1; i < 8; i++) index.post(level, base.east(200 * i), owner, "Post " + i);
            helper.assertTrue(index.refusal(base.east(2000), owner.getUUID()) != null, "a ninth charter is over the limit");
            helper.assertTrue(index.refusal(base.east(2000), UUID.randomUUID()) == null, "another owner is not");
        } finally {
            index.clearOwner(level, owner.getUUID());
        }
        helper.succeed();
    }

    // A tub run between two charters makes an open tramway link; each end loads 3 x 3 chunks, and they are really loaded.
    private static void routeProven(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "foreman");
        OutpostPlan.ALSO_ONLINE.add(owner.getUUID());
        BlockPos a = helper.absolutePos(new BlockPos(4, 2, 4)), b = a.east(300);
        Charter home = index.post(level, a, owner, "Home");
        Charter mine = index.post(level, b, owner, "");
        helper.assertTrue(mine.name().matches("[A-Z][a-z]+ \\d+"), "an unnamed charter is called after its rock and a number: " + mine.name());
        helper.assertTrue(index.plan(level).entry(home.id()).state() == OutpostPlan.State.NO_LINE, "no line, nothing loaded");
        helper.assertTrue(OutpostTickets.held(level, a).isEmpty(), "no tickets without a line");

        Link link = link(level, index, home, mine, LinkKind.TRAMWAY);
        helper.assertTrue(link != null && link.open(), "the trip proves the line");
        helper.assertValueEqual(index.bestTier(home.id()), 3, "tier of the line");
        OutpostPlan.Entry entry = index.plan(level).entry(home.id());
        helper.assertTrue(entry.loaded() && entry.side() == 3, "home loads 3 x 3");
        helper.assertValueEqual(OutpostTickets.held(level, a).size(), 9, "tickets at home");
        helper.assertValueEqual(OutpostTickets.held(level, b).size(), 9, "tickets at the mine");
        helper.assertTrue(OutpostTickets.held(level, b).contains(ChunkPos.pack(b.getX() >> 4, b.getZ() >> 4)), "the mine's own chunk is held");
        helper.assertValueEqual(index.plan(level).districts().size(), 1, "one district");
        // The same trip again only refreshes the link.
        Link again = link(level, index, home, mine, LinkKind.TRAMWAY);
        helper.assertTrue(again.id().equals(link.id()), "the same route is the same link");

        helper.succeedWhen(() -> {
            helper.assertTrue(level.getChunkSource().getChunkNow(b.getX() >> 4, b.getZ() >> 4) != null, "the mine's chunk is loaded");
            index.clearOwner(level, owner.getUUID());
            OutpostPlan.ALSO_ONLINE.remove(owner.getUUID());
            helper.assertTrue(OutpostTickets.held(level, b).isEmpty(), "tickets are given back");
        });
    }

    // Removing one block of the route cuts the line and drops the tickets; the next trip proves it again.
    private static void routeCut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "foreman");
        OutpostPlan.ALSO_ONLINE.add(owner.getUUID());
        BlockPos a = helper.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, 100), b = a.east(300);
        try {
            Charter home = index.post(level, a, owner, "Home");
            Charter mine = index.post(level, b, owner, "Ridge");
            Link link = link(level, index, home, mine, LinkKind.TRAMWAY);
            helper.assertValueEqual(OutpostTickets.held(level, a).size(), 9, "loaded before the cut");

            index.cut(level, a.east(300).above(5));
            helper.assertTrue(index.links().stream().filter(l -> l.id().equals(link.id())).allMatch(Link::open), "a block off the route cuts nothing");
            BlockPos broken = a.east(120);
            index.cut(level, broken);
            Link cut = index.links().stream().filter(l -> l.id().equals(link.id())).findFirst().orElseThrow();
            helper.assertTrue(!cut.open() && cut.cutAt().orElseThrow().equals(broken), "the line is cut where the block went");
            helper.assertTrue(OutpostTickets.held(level, a).isEmpty() && OutpostTickets.held(level, b).isEmpty(), "both ends let go");
            helper.assertTrue(index.plan(level).entry(home.id()).state() == OutpostPlan.State.NO_LINE, "the board says no line");
            BoardView view = BoardView.of(level, index, home, true);
            helper.assertTrue(view.lines().size() == 1 && view.lines().get(0).cutAt().equals(broken), "the board names the cut");

            // Putting the block back does not heal it; the next trip does, as a fresh link.
            Link healed = link(level, index, home, mine, LinkKind.TRAMWAY);
            helper.assertTrue(healed.open() && !healed.id().equals(link.id()), "a new trip proves the line again");
            helper.assertValueEqual(index.linksOf(home.id()).size(), 1, "the cut link is replaced, not kept beside it");
            helper.assertValueEqual(OutpostTickets.held(level, a).size(), 9, "loaded again");
        } finally {
            index.clearOwner(level, owner.getUUID());
            OutpostPlan.ALSO_ONLINE.remove(owner.getUUID());
        }
        helper.succeed();
    }

    // Links need standing charters at both ends, and a district holds at most eight.
    private static void proveRefused(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "clerk");
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, 8000);
        OutpostTickets.paused = true;
        try {
            Charter first = index.post(level, base, owner, "One");
            helper.assertTrue(index.prove(level, first.id(), UUID.randomUUID(), LinkKind.TRAMWAY, run(base, 10)).refusal() != null, "no charter at one end");
            helper.assertTrue(index.prove(level, first.id(), first.id(), LinkKind.TRAMWAY, run(base, 10)).refusal() != null, "not to itself");
            List<Charter> chain = new ArrayList<>(List.of(first));
            for (int i = 1; i < 9; i++) chain.add(index.post(level, base.east(200 * i), owner, "Post " + i));
            for (int i = 0; i < 7; i++) {
                helper.assertTrue(link(level, index, chain.get(i), chain.get(i + 1), LinkKind.TRAMWAY) != null, "link " + i + " fits the district");
            }
            RouteIndex.Proof ninth = index.prove(level, chain.get(7).id(), chain.get(8).id(), LinkKind.TRAMWAY, run(chain.get(7).pos(), 200));
            helper.assertTrue(ninth.link() == null && ninth.refusal() != null, "a ninth charter does not join the district");
        } finally {
            OutpostTickets.paused = false;
            index.clearOwner(level, owner.getUUID());
        }
        helper.succeed();
    }

    // The chunk budget cuts an area to a smaller square, or to nothing.
    private static void budget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "treasurer");
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, 12000);
        OutpostTickets.paused = true;
        try {
            Charter a = index.post(level, base, owner, "A"), b = index.post(level, base.east(200), owner, "B"), c = index.post(level, base.east(400), owner, "C");
            link(level, index, a, b, LinkKind.RAILWAY);
            link(level, index, b, c, LinkKind.RAILWAY);
            // A railway wants 5 x 5 = 25 chunks a charter. With room for 40: 25, then 9 (3 x 3), then none.
            var params = new OutpostPlan.Params(List.of(3, 5, 7, 9), 40, false);
            OutpostPlan plan = OutpostPlan.compute(level, index, p -> true, params);
            helper.assertTrue(plan.entry(a.id()).side() == 5 && plan.entry(a.id()).wanted() == 5, "the first charter gets its 5 x 5");
            helper.assertTrue(plan.entry(b.id()).side() == 3 && plan.entry(b.id()).state() == OutpostPlan.State.LOADED, "the second is cut to 3 x 3");
            helper.assertTrue(plan.entry(c.id()).state() == OutpostPlan.State.CHUNK_LIMIT && plan.entry(c.id()).chunks().isEmpty(), "the third has no room");
            helper.assertValueEqual(plan.chunksOf(index, owner.getUUID()), 34, "chunks the owner holds");
            var roomy = OutpostPlan.compute(level, index, p -> true, new OutpostPlan.Params(List.of(3, 5, 7, 9), 200, false));
            helper.assertTrue(roomy.entry(c.id()).side() == 5, "with room, every area is full size");
            var tight = OutpostPlan.compute(level, index, p -> true, new OutpostPlan.Params(List.of(3, 5, 7, 9), 9, false));
            helper.assertTrue(tight.entry(a.id()).side() == 3 && tight.entry(b.id()).state() == OutpostPlan.State.CHUNK_LIMIT, "9 chunks fits one 3 x 3");
            helper.assertTrue(OutpostPlan.sideForTier(3) == 3 && OutpostPlan.sideForTier(6) == 9, "default sides are 3 to 9");
        } finally {
            OutpostTickets.paused = false;
            index.clearOwner(level, owner.getUUID());
        }
        helper.succeed();
    }

    // Nothing loads while no owner of the district is on, unless the server turns that off.
    private static void ownerAway(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "absentee");
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, 16000);
        OutpostTickets.paused = true;
        try {
            Charter a = index.post(level, base, owner, "A"), b = index.post(level, base.east(200), owner, "B");
            link(level, index, a, b, LinkKind.TRAMWAY);
            var strict = new OutpostPlan.Params(List.of(3, 5, 7, 9), 200, true);
            helper.assertTrue(OutpostPlan.compute(level, index, p -> true, strict).entry(a.id()).state() == OutpostPlan.State.OWNER_AWAY, "nobody on: not loaded");
            OutpostPlan.ALSO_ONLINE.add(owner.getUUID());
            helper.assertTrue(OutpostPlan.compute(level, index, p -> true, strict).entry(a.id()).loaded(), "the owner is on: loaded");
            OutpostPlan.ALSO_ONLINE.remove(owner.getUUID());
            helper.assertTrue(OutpostPlan.compute(level, index, p -> true, new OutpostPlan.Params(List.of(3, 5, 7, 9), 200, false)).entry(b.id()).loaded(),
                    "with the rule off it loads anyway");
        } finally {
            OutpostTickets.paused = false;
            OutpostPlan.ALSO_ONLINE.remove(owner.getUUID());
            index.clearOwner(level, owner.getUUID());
        }
        helper.succeed();
    }

    // A post taken down keeps its links dormant; put back nearby in time it is the same charter, far off it is a new one.
    private static void dormantRevive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "mover");
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, 20000);
        OutpostTickets.paused = true;
        try {
            Charter a = index.post(level, base, owner, "Old Post"), b = index.post(level, base.east(200), owner, "Far End");
            Link link = link(level, index, a, b, LinkKind.TRAMWAY);
            index.take(level, base);
            helper.assertTrue(index.charterAt(base).isEmpty(), "the post is gone");
            helper.assertTrue(!index.live(index.links().stream().filter(l -> l.id().equals(link.id())).findFirst().orElseThrow()), "its line is dormant");
            helper.assertValueEqual(index.bestTier(b.id()), 0, "the other end has no live line");
            Charter far = index.post(level, base.east(40), owner, "");
            helper.assertTrue(!far.id().equals(a.id()), "40 blocks off is a new charter");
            index.take(level, base.east(40));
            Charter back = index.post(level, base.east(3), owner, "");
            helper.assertTrue(back.id().equals(a.id()) && back.name().equals("Old Post"), "3 blocks off is the same charter, same name");
            helper.assertValueEqual(index.bestTier(b.id()), 3, "its line runs again");
            // Left too long, it is forgotten with its links.
            index.take(level, base.east(3));
            index.expire(level, level.getGameTime() + RouteIndex.DORMANT_TICKS + 1);
            helper.assertTrue(index.charter(a.id()).isEmpty() && index.linksOf(b.id()).isEmpty(), "after three days it is forgotten");
        } finally {
            OutpostTickets.paused = false;
            index.clearOwner(level, owner.getUUID());
        }
        helper.succeed();
    }

    // The block: placing posts a charter, renaming is for owners within reach, breaking drops the item with its name.
    private static void charterBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "poster"), stranger = owner(level, "stranger");
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertTrue(level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP), "the platform carries the post");
        OutpostTickets.paused = true;
        try {
            level.setBlock(pos, TransportBlocks.OUTPOST_CHARTER.get().defaultBlockState(), Block.UPDATE_ALL);
            ItemStack named = new ItemStack(TransportBlocks.OUTPOST_CHARTER_ITEM.get());
            named.set(TransportDataComponents.CHARTER_NAME.get(), "Tin Hill");
            ((OutpostCharterBlock) TransportBlocks.OUTPOST_CHARTER.get()).setPlacedBy(level, pos, level.getBlockState(pos), owner, named);
            Charter charter = index.charterAt(pos).orElseThrow(() -> new AssertionError("placing posts a charter"));
            helper.assertTrue(charter.name().equals("Tin Hill") && charter.owner().equals(owner.getUUID()), "name from the item, owner from the placer");

            owner.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            stranger.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            helper.assertTrue(!OutpostPayloads.rename(stranger, pos, "Mine Now"), "a stranger cannot rename it");
            helper.assertTrue(OutpostPayloads.rename(owner, pos, "  Tin   Hill   Works  "), "the owner can");
            helper.assertTrue(index.charterAt(pos).orElseThrow().name().equals("Tin Hill Works"), "spaces are tidied");
            owner.setPos(pos.getX() + 50, pos.getY(), pos.getZ());
            helper.assertTrue(!OutpostPayloads.rename(owner, pos, "Too Far"), "not from across the map");
            index.rename(level, charter.id(), "A name that is far longer than twenty-four characters");
            helper.assertTrue(index.charterAt(pos).orElseThrow().name().length() <= RouteIndex.MAX_NAME, "names stop at 24");

            var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(TransportBlocks.OUTPOST_CHARTER_ITEM.get()), "it drops itself");
            helper.assertTrue(drops.get(0).get(TransportDataComponents.CHARTER_NAME.get()) != null, "with its name on it");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(index.charterAt(pos).isEmpty(), "breaking it takes the charter down");
        } finally {
            OutpostTickets.paused = false;
            index.clearOwner(level, owner.getUUID());
        }
        helper.succeed();
    }

    // What the board shows survives the trip to the client.
    private static void boardView(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = owner(level, "reader");
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4)).offset(0, 0, 24000);
        OutpostTickets.paused = true;
        try {
            Charter a = index.post(level, base, owner, "Millhouse"), b = index.post(level, base.east(300), owner, "Home");
            link(level, index, a, b, LinkKind.TRAMWAY);
            index.cut(level, base.east(77));
            BoardView view = BoardView.of(level, index, a, true);
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            BoardView.CODEC.encode(buf, view);
            BoardView back = BoardView.CODEC.decode(buf);
            helper.assertTrue(back.equals(view), "the board reads back as written");
            helper.assertTrue(back.name().equals("Millhouse") && back.lines().get(0).other().equals("Home") && !back.lines().get(0).open(), "name and line");
            helper.assertValueEqual(back.lastTrafficAgo() >= 0, true, "traffic is noted");
        } finally {
            OutpostTickets.paused = false;
            index.clearOwner(level, owner.getUUID());
        }
        helper.succeed();
    }
}
