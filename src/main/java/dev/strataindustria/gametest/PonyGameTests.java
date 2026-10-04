package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.HandCrankBlockEntity;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.transport.outpost.OutpostTickets;
import dev.strataindustria.transport.rail.HayRackBlockEntity;
import dev.strataindustria.transport.rail.InclineWinchBlock;
import dev.strataindustria.transport.rail.InclineWinchBlockEntity;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.PonyEntity;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.StopRule;
import dev.strataindustria.transport.rail.TubStopBlockEntity;
import dev.strataindustria.transport.rail.VehicleTickets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

/** The pony, the hay rack and the incline winch (outposts and transport spec 5.3, 5.4 and 15, chunks O4 and O5). */
final class PonyGameTests {
    private static final int ROW = RailGameTests.ROW;
    private static final int Y = RailGameTests.RAIL_Y;

    private PonyGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("pony_harness_roundtrip", PonyGameTests::harnessRoundtrip);
        tests.put("pony_harness_refusals", PonyGameTests::harnessRefusals);
        tests.put("pony_donkey_and_mule", PonyGameTests::donkeyAndMule);
        tests.put("pony_shuttle", PonyGameTests::shuttle);
        tests.put("pony_eats_at_rack", PonyGameTests::eats);
        tests.put("pony_hungry", PonyGameTests::hungry);
        tests.put("pony_uphill_slower", PonyGameTests::uphillSlower);
        tests.put("pony_balks_at_hill", PonyGameTests::balks);
        tests.put("pony_consist_limit", PonyGameTests::consistLimit);
        tests.put("pony_ticket", PonyGameTests::ticket);
        tests.put("hay_rack_hay_only", PonyGameTests::hayRack);
        tests.put("incline_winch_hauls", PonyGameTests::winchHauls);
        tests.put("incline_winch_holds", PonyGameTests::winchHolds);
        tests.put("incline_winch_pony", PonyGameTests::winchPony);
    }

    // ---------------------------------------------------------------- setup

    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "driver"));
    }

    /** A tame horse of the given kind next to the rail at {@code x}, one block to the south of the line. */
    private static <T extends AbstractHorse> T animal(GameTestHelper helper, EntityType<T> type, int x, boolean tame) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, Y, ROW + 1));
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        T horse = helper.spawn(type, new BlockPos(x, Y, ROW + 1));
        horse.setTamed(tame);
        horse.setYRot(-90);
        return horse;
    }

    /** Uses a harness on {@code target} and returns what is left of the stack. */
    private static ItemStack use(GameTestHelper helper, FakePlayer player, AbstractHorse target) {
        ItemStack harness = new ItemStack(RailRegistry.HARNESS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);
        RailRegistry.HARNESS.get().interactLivingEntity(harness, player, target, InteractionHand.MAIN_HAND);
        return harness;
    }

    private static PonyEntity ponyOn(GameTestHelper helper, int x, boolean east) {
        Horse horse = animal(helper, net.minecraft.world.entity.EntityTypes.HORSE, x, true);
        horse.setYRot(east ? -90 : 90);
        PonyEntity pony = PonyEntity.harness(helper.getLevel(), horse, RailGameTests.at(helper, x));
        helper.assertTrue(pony != null, "the horse goes into harness");
        return pony;
    }

    private static List<PonyEntity> ponies(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(PonyEntity.class, AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(-4, 0, -4)), helper.absolutePos(new BlockPos(30, 30, 12))));
    }

    // ---------------------------------------------------------------- harness

    // A harness on a tame horse beside the track puts a pony on the rail; taking it off gives the same horse back.
    private static void harnessRoundtrip(GameTestHelper helper) {
        RailGameTests.layRun(helper, 0, 6);
        ServerLevel level = helper.getLevel();
        Horse horse = animal(helper, net.minecraft.world.entity.EntityTypes.HORSE, 3, true);
        UUID id = horse.getUUID();
        FakePlayer player = player(helper);
        var held = use(helper, player, horse);
        helper.assertTrue(held.isEmpty(), "the harness is used up");
        helper.assertTrue(horse.isRemoved(), "the horse left the field");
        List<PonyEntity> ponies = ponies(helper);
        helper.assertValueEqual(ponies.size(), 1, "one pony");
        PonyEntity pony = ponies.get(0);
        helper.assertTrue(pony.blockPosition().equals(RailGameTests.at(helper, 3)), "standing on the rail, at " + pony.blockPosition());
        helper.assertTrue(pony.kind() == PonyEntity.KIND_HORSE, "a horse");
        pony.unharness(level);
        helper.assertTrue(pony.isRemoved(), "the pony is gone");
        Horse back = level.getEntitiesOfClass(Horse.class, new AABB(RailGameTests.at(helper, 3)).inflate(3)).stream()
                .filter(h -> h.getUUID().equals(id)).findFirst().orElse(null);
        helper.assertTrue(back != null && back.isTamed(), "the same horse is back, still tame");
        helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, new AABB(RailGameTests.at(helper, 3)).inflate(3), e -> e.getItem().is(RailRegistry.HARNESS.get())).isEmpty(),
                "and the harness lies beside it");
        helper.succeed();
    }

    // Wild, young or far-off animals are not harnessed.
    private static void harnessRefusals(GameTestHelper helper) {
        RailGameTests.layRun(helper, 0, 6);
        FakePlayer player = player(helper);
        Horse wild = animal(helper, net.minecraft.world.entity.EntityTypes.HORSE, 2, false);
        use(helper, player, wild);
        helper.assertTrue(!wild.isRemoved() && ponies(helper).isEmpty(), "a wild horse stays a horse");
        Horse foal = animal(helper, net.minecraft.world.entity.EntityTypes.HORSE, 4, true);
        foal.setBaby(true);
        use(helper, player, foal);
        helper.assertTrue(!foal.isRemoved() && ponies(helper).isEmpty(), "a foal stays a foal");
        Horse far = helper.spawn(net.minecraft.world.entity.EntityTypes.HORSE, new BlockPos(2, Y, ROW + 6));
        helper.getLevel().setBlock(far.blockPosition().below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        far.setTamed(true);
        use(helper, player, far);
        helper.assertTrue(!far.isRemoved() && ponies(helper).isEmpty(), "a horse far from the rail stays where it is");
        helper.succeed();
    }

    private static void donkeyAndMule(GameTestHelper helper) {
        RailGameTests.layRun(helper, 0, 8);
        FakePlayer player = player(helper);
        var donkey = animal(helper, net.minecraft.world.entity.EntityTypes.DONKEY, 2, true);
        use(helper, player, donkey);
        var mule = animal(helper, net.minecraft.world.entity.EntityTypes.MULE, 6, true);
        use(helper, player, mule);
        List<PonyEntity> ponies = ponies(helper);
        helper.assertValueEqual(ponies.size(), 2, "two ponies");
        helper.assertTrue(ponies.stream().anyMatch(p -> p.kind() == PonyEntity.KIND_DONKEY), "one donkey");
        helper.assertTrue(ponies.stream().anyMatch(p -> p.kind() == PonyEntity.KIND_MULE), "one mule");
        helper.succeed();
    }

    // ---------------------------------------------------------------- the line

    /** Two stops, a run of rail between them and a pony with two tubs behind it between the two. */
    private static PonyEntity line(GameTestHelper helper, int a, int b, StopRule rule, int seconds) {
        RailGameTests.layRun(helper, a + 1, b - 1);
        RailGameTests.stopAt(helper, a, rule, seconds, true);
        RailGameTests.stopAt(helper, b, rule, seconds, true);
        int mid = (a + b) / 2;
        PonyEntity pony = ponyOn(helper, mid, true);
        pony.setPos(RailGameTests.at(helper, 0).getX() + mid + 0.5, pony.getY(), pony.getZ());
        MineTubEntity first = RailGameTests.tubAt(helper, mid + 0.5 - 1.25);
        MineTubEntity second = RailGameTests.tubAt(helper, mid + 0.5 - 2.5);
        helper.assertTrue(second.couple() == MineTubEntity.Coupling.OK && first.couple() == MineTubEntity.Coupling.OK, "two tubs coupled behind the pony");
        helper.assertTrue(first.head() == pony, "the pony leads");
        return pony;
    }

    // The pony takes its two tubs from one stop to the next, waits by the rule, and comes back.
    private static void shuttle(GameTestHelper helper) {
        PonyEntity pony = line(helper, 1, 7, StopRule.WAIT, 1);
        int[] seen = {0, 0};
        long[] order = {0, 0};
        for (int t = 1; t <= 190; t++) {
            int tick = t;
            helper.runAfterDelay(t, () -> {
                if (pony.isHeldAt(RailGameTests.at(helper, 7)) && seen[1] == 0) {
                    seen[1] = 1;
                    order[1] = tick;
                }
                if (pony.isHeldAt(RailGameTests.at(helper, 1)) && seen[0] == 0) {
                    seen[0] = 1;
                    order[0] = tick;
                }
            });
        }
        helper.runAfterDelay(191, () -> {
            helper.assertTrue(seen[1] == 1, "it reached the far stop");
            helper.assertTrue(seen[0] == 1 && order[0] > order[1], "and then the near stop, after it, at tick " + order[0] + " from " + order[1]);
            helper.assertValueEqual(pony.consist().size(), 3, "with both tubs still behind it");
            helper.assertTrue(pony.hay() < 0.9f, "it used some hay, has " + pony.hay());
            helper.succeed();
        });
    }

    // A hay rack within three blocks of the stop feeds the pony while it waits there.
    private static void eats(GameTestHelper helper) {
        PonyEntity pony = line(helper, 1, 7, StopRule.WAIT, 6);
        pony.setHay(0.4f);
        BlockPos rackPos = RailGameTests.at(helper, 7).south(2);
        helper.getLevel().setBlock(rackPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(rackPos, RailRegistry.HAY_RACK.get().defaultBlockState(), Block.UPDATE_ALL);
        HayRackBlockEntity rack = (HayRackBlockEntity) helper.getLevel().getBlockEntity(rackPos);
        rack.addBale();
        rack.addBale();
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(pony.isHeldAt(RailGameTests.at(helper, 7)), "the pony is waiting at the stop");
            helper.assertValueEqual(rack.bales(), 1, "it ate one bale");
            helper.assertTrue(pony.hay() > 1.2f, "and has it in it, " + pony.hay());
            helper.succeed();
        });
    }

    // With no hay the pony stands at the stop and does not leave; fed, it goes.
    private static void hungry(GameTestHelper helper) {
        PonyEntity pony = line(helper, 1, 7, StopRule.WAIT, 1);
        pony.setHay(0.0f);
        TubStopBlockEntity stop = (TubStopBlockEntity) helper.getLevel().getBlockEntity(RailGameTests.at(helper, 7));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(stop.isHolding() && pony.isHeldAt(RailGameTests.at(helper, 7)), "held at the far stop");
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(pony.isHeldAt(RailGameTests.at(helper, 7)), "still standing there, hungry");
            pony.setHay(1.0f);
        });
        helper.runAfterDelay(175, () -> {
            helper.assertTrue(!pony.isHeldAt(RailGameTests.at(helper, 7)), "fed, it set off again");
            helper.succeed();
        });
    }

    /** A flat rail at x 0 and two slope rails rising to the east at x 1 and 2, with a pony on the flat. */
    private static PonyEntity hill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RailGameTests.lay(helper, 0, RailRegistry.WOODEN_RAIL.get());
        for (int i = 1; i <= 2; i++) {
            BlockPos pos = helper.absolutePos(new BlockPos(i, Y + i - 1, ROW));
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Incline.flat(RailShape.ASCENDING_EAST), Block.UPDATE_ALL);
        }
        BlockPos top = helper.absolutePos(new BlockPos(3, Y + 2, ROW));
        level.setBlock(top.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(top, Incline.flat(RailShape.EAST_WEST), Block.UPDATE_ALL);
        return ponyOn(helper, 0, true);
    }

    // Uphill the pony walks at half its pace.
    private static void uphillSlower(GameTestHelper helper) {
        PonyEntity pony = hill(helper);
        double[] flatSpeed = {0}, slopeSpeed = {0};
        for (int t = 2; t <= 60; t++) {
            helper.runAfterDelay(t, () -> {
                double x = pony.getX() - RailGameTests.at(helper, 0).getX();
                double speed = pony.getDeltaMovement().horizontalDistance();
                if (x < 0.8) flatSpeed[0] = Math.max(flatSpeed[0], speed);
                else if (x > 1.2 && x < 1.8) slopeSpeed[0] = Math.max(slopeSpeed[0], speed);
            });
        }
        helper.runAfterDelay(61, () -> {
            helper.assertTrue(flatSpeed[0] > 0.17 && flatSpeed[0] < 0.25, "full pace on the flat, " + flatSpeed[0]);
            helper.assertTrue(slopeSpeed[0] > 0.06 && slopeSpeed[0] < 0.14, "half pace on the slope, " + slopeSpeed[0]);
            helper.succeed();
        });
    }

    // After eight slope rails in a row the next one is too much: the pony stops on it, then turns round.
    private static void balks(GameTestHelper helper) {
        PonyEntity pony = hill(helper);
        for (int t = 1; t <= 40; t++) {
            helper.runAfterDelay(t, () -> {
                if (pony.getX() - RailGameTests.at(helper, 0).getX() > 1.1 && pony.slopeRun() < PonyEntity.MAX_SLOPE_RAILS) pony.setSlopeRun(PonyEntity.MAX_SLOPE_RAILS);
            });
        }
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(pony.balking(), "the pony balks");
            helper.assertTrue(pony.getDeltaMovement().horizontalDistance() < 0.02, "and stands still, speed " + pony.getDeltaMovement().horizontalDistance());
            helper.assertTrue(pony.getX() - RailGameTests.at(helper, 0).getX() < 2.9, "short of the top");
        });
        helper.runAfterDelay(170, () -> {
            helper.assertTrue(!pony.balking(), "it has given up waiting");
            helper.assertTrue(pony.getDeltaMovement().x < 0, "and walks back down, " + pony.getDeltaMovement());
            helper.succeed();
        });
    }

    // Four tubs behind a pony, and no fifth.
    private static void consistLimit(GameTestHelper helper) {
        RailGameTests.layRun(helper, 0, 14);
        PonyEntity pony = ponyOn(helper, 8, true);
        pony.setPos(RailGameTests.at(helper, 0).getX() + 8.5, pony.getY(), pony.getZ());
        MineTubEntity last = pony;
        for (int i = 1; i <= 4; i++) {
            MineTubEntity tub = RailGameTests.tubAt(helper, 8.5 - 1.25 * i);
            helper.assertTrue(tub.couple() == MineTubEntity.Coupling.OK, "tub " + i + " couples");
            last = tub;
        }
        helper.assertValueEqual(pony.consist().size(), 5, "pony and four tubs");
        MineTubEntity fifth = RailGameTests.tubAt(helper, 8.5 - 1.25 * 5);
        helper.assertTrue(fifth.couple() == MineTubEntity.Coupling.TOO_LONG, "the fifth is refused");
        helper.assertTrue(pony.couple() == MineTubEntity.Coupling.LEADS, "and a pony never follows");
        helper.succeed();
    }

    // Leaving a stop in a charter's area, the pony holds a moving ticket; it lets go 30 seconds after resting in another station.
    private static void ticket(GameTestHelper helper) {
        RailGameTests.Plan plan = new RailGameTests.Plan(helper);
        try {
            RailGameTests.layRun(helper, 2, 6);
            RailGameTests.stopAt(helper, 1, StopRule.WAIT, 1, true);
            RailGameTests.stopAt(helper, 7, StopRule.REDSTONE, 1, true);
            PonyEntity pony = ponyOn(helper, 1, true);
            pony.setPos(RailGameTests.at(helper, 0).getX() + 1.5, pony.getY(), pony.getZ());
            helper.runAfterDelay(60, () -> {
                try {
                    helper.assertTrue(pony.ticketed() && VehicleTickets.holds(pony.getUUID()), "the pony runs on a moving ticket");
                    helper.assertTrue(VehicleTickets.moving(plan.owner.getUUID()) >= 1, "the owner has a consist moving");
                    pony.discard();
                } finally {
                    plan.close();
                }
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(!VehicleTickets.holds(pony.getUUID()), "a removed pony gives its chunks back");
                    helper.succeed();
                });
            });
        } catch (RuntimeException e) {
            plan.close();
            throw e;
        }
    }

    // ---------------------------------------------------------------- hay rack

    private static void hayRack(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, Y, ROW));
        helper.getLevel().setBlock(pos, RailRegistry.HAY_RACK.get().defaultBlockState(), Block.UPDATE_ALL);
        HayRackBlockEntity rack = (HayRackBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(rack.canPlaceItemThroughFace(0, new ItemStack(Items.HAY_BLOCK), Direction.UP), "a hopper can put hay in");
        helper.assertTrue(!rack.canPlaceItemThroughFace(0, new ItemStack(Items.WHEAT), Direction.UP), "wheat does not go in");
        helper.assertTrue(!rack.canPlaceItemThroughFace(0, new ItemStack(Items.COBBLESTONE), Direction.UP), "nor stone");
        helper.assertTrue(!rack.canTakeItemThroughFace(0, new ItemStack(Items.HAY_BLOCK), Direction.DOWN), "a hopper cannot take it out");
        helper.assertTrue(!helper.getLevel().getBlockState(pos).getValue(dev.strataindustria.transport.rail.HayRackBlock.FILLED), "empty at first");
        rack.addBale();
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(dev.strataindustria.transport.rail.HayRackBlock.FILLED), "it shows hay once there is some");
        helper.assertValueEqual(rack.getContainerSize(), 3, "three slots");
        helper.assertTrue(rack.takeBale() && rack.isEmpty(), "a bale comes out");
        helper.succeed();
    }

    // ---------------------------------------------------------------- winch

    /** A winch at the top of a six-block descent facing east, a hand crank behind it, and a tub on the slope. */
    private static final class Incline {
        final BlockPos winch, crank;
        final HandCrankBlockEntity handle;
        final InclineWinchBlockEntity drum;

        Incline(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            int top = Y + 6;
            winch = helper.absolutePos(new BlockPos(1, top, ROW));
            level.setBlock(winch.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(winch, RailRegistry.INCLINE_WINCH.get().defaultBlockState().setValue(InclineWinchBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
            crank = winch.west();
            level.setBlock(crank, dev.strataindustria.registry.ModBlocks.HAND_CRANK.get().defaultBlockState()
                    .setValue(HandCrankBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
            // line[0] flat beside the winch, then six slope rails running down to the east.
            level.setBlock(winch.east().below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(winch.east(), flat(RailShape.EAST_WEST), Block.UPDATE_ALL);
            for (int i = 1; i <= 6; i++) {
                BlockPos pos = winch.east(1 + i).below(i);
                level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(pos, flat(RailShape.ASCENDING_WEST), Block.UPDATE_ALL);
            }
            BlockPos end = winch.east(8).below(6);
            level.setBlock(end.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(end, flat(RailShape.EAST_WEST), Block.UPDATE_ALL);
            handle = (HandCrankBlockEntity) level.getBlockEntity(crank);
            drum = (InclineWinchBlockEntity) level.getBlockEntity(winch);
        }

        static BlockState flat(RailShape shape) {
            return RailRegistry.WOODEN_RAIL.get().defaultBlockState().setValue(
                    ((BaseRailBlock) RailRegistry.WOODEN_RAIL.get()).getShapeProperty(), shape);
        }

        /** A tub standing on slope rail {@code i} (1 is the top one), with something in it. */
        MineTubEntity tub(GameTestHelper helper, int i) {
            BlockPos pos = winch.east(1 + i).below(i);
            MineTubEntity tub = helper.spawn(RailRegistry.MINE_TUB_ENTITY.get(), helper.relativePos(pos));
            tub.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            tub.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
            return tub;
        }

        void crankFor(GameTestHelper helper, int ticks) {
            FakePlayer player = player(helper);
            for (int t = 1; t <= ticks; t += 4) {
                helper.runAfterDelay(t, () -> {
                    handle.crank(player);
                    KineticNetworks.rebuildNow(helper.getLevel(), winch);
                });
            }
        }
    }

    // A turning winch hauls a loaded tub up the slope toward itself.
    private static void winchHauls(GameTestHelper helper) {
        Incline incline = new Incline(helper);
        MineTubEntity tub = incline.tub(helper, 5);
        double startX = tub.getX(), startY = tub.getY();
        incline.crankFor(helper, 110);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(incline.drum.powered(), "the winch is turning, rpm " + incline.drum.kinetic().rpm());
            helper.assertTrue(tub.getX() < startX - 2.0, "the tub came up the slope, x " + startX + " to " + tub.getX());
            helper.assertTrue(tub.getY() > startY + 1.5, "and rose, y " + startY + " to " + tub.getY());
            helper.assertTrue(incline.drum.ropeTarget() == tub.getId() || incline.drum.ropeTarget() == -1, "the rope is tied to it");
            helper.succeed();
        });
    }

    // With no power the winch holds a tub on the slope: it does not run away.
    private static void winchHolds(GameTestHelper helper) {
        Incline incline = new Incline(helper);
        MineTubEntity tub = incline.tub(helper, 4);
        double startX = tub.getX();
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(!incline.drum.powered(), "nothing turns the winch");
            helper.assertTrue(Math.abs(tub.getX() - startX) < 0.1, "the tub stayed where it was, moved " + (tub.getX() - startX));
            helper.succeed();
        });
    }

    // A pony at the foot of the hill that it will not climb is hauled up by the winch.
    private static void winchPony(GameTestHelper helper) {
        Incline incline = new Incline(helper);
        BlockPos rail = incline.winch.east(5).below(4);
        Horse animal = net.minecraft.world.entity.EntityTypes.HORSE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        animal.setPos(rail.getX() + 0.5, rail.getY(), rail.getZ() + 0.5);
        animal.setTamed(true);
        animal.setYRot(90);
        helper.getLevel().addFreshEntity(animal);
        PonyEntity pony = PonyEntity.harness(helper.getLevel(), animal, rail);
        helper.assertTrue(pony != null, "the pony is in harness on the slope");
        double startY = pony.getY();
        incline.crankFor(helper, 130);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(pony.getY() > startY + 1.5, "the pony was taken up the hill, y " + startY + " to " + pony.getY());
            helper.succeed();
        });
    }
}
