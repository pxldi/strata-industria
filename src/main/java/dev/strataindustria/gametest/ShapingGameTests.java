package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.knapping.KnappedFrom;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.knapping.Shaping;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.PatternRegistry;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Hand shaping in the world (redesign L5), run by {@link ModGameTests}. */
final class ShapingGameTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;
    /** Far enough apart that no blow lands on the glint or counts as held. */
    private static final int GAP = 20;

    private ShapingGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("shaping_shapes", ShapingGameTests::shapes);
        tests.put("shaping_strikes", ShapingGameTests::strikes);
        tests.put("shaping_true_blow", ShapingGameTests::trueBlow);
        tests.put("shaping_hold", ShapingGameTests::hold);
        tests.put("shaping_cost", ShapingGameTests::cost);
        tests.put("shaping_clay_and_carving", ShapingGameTests::clayAndCarving);
        tests.put("shaping_remembers", ShapingGameTests::remembers);
    }

    private static FakePlayer player(GameTestHelper helper, ItemStack held) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "shaper"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
        player.setItemInHand(HAND, held);
        return player;
    }

    private static List<ItemEntity> dropped(GameTestHelper helper, FakePlayer player, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(8), e -> e.getItem().is(item));
    }

    private static int pieces(GameTestHelper helper, FakePlayer player) {
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class, new AABB(player.blockPosition()).inflate(8)).size();
    }

    private static Set<Item> results(ServerLevel level, ItemStack material) {
        Set<Item> out = new HashSet<>();
        for (RecipeHolder<KnappingRecipe> shape : Shaping.shapes(level, material)) out.add(shape.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(material)).getItem());
        return out;
    }

    // Every material offers its own shapes, and none takes more than a handful of blows.
    private static void shapes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack flint = new ItemStack(Items.FLINT);
        ItemStack rock = new ItemStack(ModItems.LOOSE_ROCK.get(Rock.GRANITE).get());
        Set<Item> stone = Set.of(ModItems.STONE_AXE_HEAD.get(), ModItems.STONE_KNIFE_BLADE.get(), ModItems.STONE_SHOVEL_HEAD.get(),
                ModItems.STONE_HOE_HEAD.get(), ModItems.STONE_HAMMER_HEAD.get(), ModItems.STONE_SPEAR_HEAD.get(), ModItems.STONE_PICKAXE_HEAD.get());
        helper.assertTrue(results(level, flint).containsAll(stone), "flint should offer every stone head");
        helper.assertTrue(!results(level, flint).contains(ModItems.QUERNSTONE.get()), "flint is too small for a quernstone");
        helper.assertTrue(results(level, rock).containsAll(stone), "rock should offer every stone head");
        helper.assertTrue(results(level, rock).contains(ModItems.QUERNSTONE.get()), "rock should offer a quernstone");
        helper.assertTrue(results(level, new ItemStack(Items.CLAY_BALL)).contains(ModItems.UNFIRED_CRUCIBLE.get()), "clay should offer a crucible");
        helper.assertTrue(!results(level, new ItemStack(Items.CLAY_BALL)).contains(ModItems.STONE_AXE_HEAD.get()), "clay offers no stone heads");
        helper.assertValueEqual(Shaping.shapes(level, new ItemStack(PatternRegistry.PATTERN_BLANK.get())).size(), PatternRegistry.SHAPES.size(), "carvable shapes");
        for (RecipeHolder<KnappingRecipe> shape : level.recipeAccess().recipeMap().byType(dev.strataindustria.registry.ModRecipes.KNAPPING.get())) {
            int blows = shape.value().blows();
            helper.assertTrue(blows >= 3 && blows <= 6, shape.id().identifier() + " takes " + blows + " blows");
        }
        helper.succeed();
    }

    // Four spaced blows make an axe head from flint: nothing is spent until the last one, then the head hops free.
    private static void strikes(GameTestHelper helper) {
        FakePlayer player = player(helper, new ItemStack(Items.FLINT, 16));
        Shaping.cycle(player, HAND, 0);
        helper.assertValueEqual(pieces(helper, player), 2, "picking a shape shows the piece and a ghost of the result");
        long t = 0;
        for (int blow = 1; blow <= 3; blow++, t += GAP) {
            Shaping.strike(player, HAND, t);
            helper.assertValueEqual(Shaping.blows(player), blow, "blows after click " + blow);
            helper.assertValueEqual(player.getMainHandItem().getCount(), 16, "a half-shaped piece costs nothing");
            helper.assertTrue(dropped(helper, player, ModItems.STONE_AXE_HEAD.get()).isEmpty(), "no head before the last blow");
            helper.assertValueEqual(pieces(helper, player), 2, "the piece and the growing result show on the surface");
        }
        Shaping.strike(player, HAND, t);
        List<ItemEntity> heads = dropped(helper, player, ModItems.STONE_AXE_HEAD.get());
        helper.assertValueEqual(heads.size(), 1, "the head drops on the last blow");
        helper.assertValueEqual(pieces(helper, player), 0, "the pieces go when the head is done");
        helper.assertTrue(heads.get(0).getItem().get(ModDataComponents.KNAPPED_FROM.get()).equals(new KnappedFrom(KnappedFrom.FLINT)), "the head remembers its flint");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 15, "one flint spent");
        helper.assertValueEqual(Shaping.blows(player), 0, "the next piece starts fresh");
        helper.succeed();
    }

    // A blow on the rebound glint counts twice, so a keen player finishes in fewer clicks.
    private static void trueBlow(GameTestHelper helper) {
        FakePlayer player = player(helper, new ItemStack(Items.FLINT, 16));
        Shaping.strike(player, HAND, 0);
        helper.assertValueEqual(Shaping.blows(player), 1, "first blow");
        Shaping.strike(player, HAND, 10);
        helper.assertValueEqual(Shaping.blows(player), 3, "a blow on the glint counts twice");
        Shaping.strike(player, HAND, 20);
        helper.assertTrue(!dropped(helper, player, ModItems.STONE_AXE_HEAD.get()).isEmpty(), "three clicks finish a four-blow head");
        helper.succeed();
    }

    // A held button strikes at a steady pace and never lands on the glint; clicks too close together are ignored.
    private static void hold(GameTestHelper helper) {
        FakePlayer player = player(helper, new ItemStack(Items.FLINT, 16));
        for (long t = 0; t <= 44; t += 4) {
            Shaping.strike(player, HAND, t);
            if (t < 36) helper.assertTrue(dropped(helper, player, ModItems.STONE_AXE_HEAD.get()).isEmpty(), "still shaping at tick " + t);
        }
        helper.assertValueEqual(dropped(helper, player, ModItems.STONE_AXE_HEAD.get()).size(), 1, "holding the button finishes the head");
        FakePlayer quick = player(helper, new ItemStack(Items.FLINT, 16));
        Shaping.strike(quick, HAND, 100);
        Shaping.strike(quick, HAND, 102);
        helper.assertValueEqual(Shaping.blows(quick), 1, "a second click two ticks later is ignored");
        helper.succeed();
    }

    // Too little material gives a line and no progress; rock costs two, a quernstone four.
    private static void cost(GameTestHelper helper) {
        FakePlayer player = player(helper, new ItemStack(ModItems.LOOSE_ROCK.get(Rock.BASALT).get(), 1));
        Shaping.strike(player, HAND, 0);
        helper.assertValueEqual(Shaping.blows(player), 0, "one rock is not enough");
        player.setItemInHand(HAND, new ItemStack(ModItems.LOOSE_ROCK.get(Rock.BASALT).get(), 5));
        long t = 100;
        for (int i = 0; i < 4; i++, t += GAP) Shaping.strike(player, HAND, t);
        helper.assertValueEqual(dropped(helper, player, ModItems.STONE_AXE_HEAD.get()).size(), 1, "an axe head from rock");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "an axe head costs two rocks");
        // The quernstone: four rocks, six blows.
        int guard = 0;
        while (!Shaping.current(player, player.getMainHandItem()).map(h -> h.value().result().item().value().equals(ModItems.QUERNSTONE.get())).orElse(false) && guard++ < 30) {
            Shaping.cycle(player, HAND, 1);
        }
        player.setItemInHand(HAND, new ItemStack(ModItems.LOOSE_ROCK.get(Rock.BASALT).get(), 4));
        for (int i = 0; i < 6; i++, t += GAP) Shaping.strike(player, HAND, t);
        helper.assertValueEqual(dropped(helper, player, ModItems.QUERNSTONE.get()).size(), 1, "a quernstone");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 0, "a quernstone costs four rocks");
        helper.succeed();
    }

    // Clay presses into a crucible, a plank blank carves into a pattern, with the same strikes.
    private static void clayAndCarving(GameTestHelper helper) {
        FakePlayer player = player(helper, new ItemStack(Items.CLAY_BALL, 10));
        pick(player, ModItems.UNFIRED_CRUCIBLE.get());
        long t = 0;
        for (int i = 0; i < 3; i++, t += GAP) Shaping.strike(player, HAND, t);
        helper.assertValueEqual(dropped(helper, player, ModItems.UNFIRED_CRUCIBLE.get()).size(), 1, "a crucible from clay");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 5, "a crucible costs five clay");

        player.setItemInHand(HAND, new ItemStack(PatternRegistry.PATTERN_BLANK.get(), 2));
        pick(player, PatternRegistry.PATTERNS.get("gear").get());
        for (int i = 0; i < 3; i++, t += GAP) Shaping.strike(player, HAND, t);
        helper.assertValueEqual(dropped(helper, player, PatternRegistry.PATTERNS.get("gear").get()).size(), 1, "a gear pattern from a blank");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "one blank used");
        helper.succeed();
    }

    // The next piece starts on the shape last worked, and switching shape drops the blows on the old one.
    private static void remembers(GameTestHelper helper) {
        FakePlayer player = player(helper, new ItemStack(Items.FLINT, 16));
        pick(player, ModItems.STONE_HAMMER_HEAD.get());
        long t = 0;
        for (int i = 0; i < 4; i++, t += GAP) Shaping.strike(player, HAND, t);
        helper.assertValueEqual(dropped(helper, player, ModItems.STONE_HAMMER_HEAD.get()).size(), 1, "a hammer head");
        helper.assertValueEqual(Shaping.current(player, player.getMainHandItem()).map(h -> h.value().result().item().value()).orElse(null),
                ModItems.STONE_HAMMER_HEAD.get(), "the picker stays on the last shape");
        Shaping.strike(player, HAND, t);
        helper.assertValueEqual(Shaping.blows(player), 1, "one blow on the hammer head");
        Shaping.cycle(player, HAND, 1);
        helper.assertValueEqual(Shaping.blows(player), 0, "a new shape starts from nothing");
        helper.succeed();
    }

    /** Cycles until the held material is set to the shape that gives {@code result}. */
    private static void pick(FakePlayer player, Item result) {
        int guard = 0;
        while (!Shaping.current(player, player.getMainHandItem()).map(h -> h.value().result().item().value().equals(result)).orElse(false)) {
            if (guard++ > 40) throw new IllegalStateException("no shape gives " + result);
            Shaping.cycle(player, HAND, 1);
        }
    }
}
