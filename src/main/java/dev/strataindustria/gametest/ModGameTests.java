package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.PitKilnBlock;
import dev.strataindustria.ceramics.PitKilnBlockEntity;
import dev.strataindustria.charcoal.CharcoalPileBlock;
import dev.strataindustria.charcoal.LogPileBlock;
import dev.strataindustria.charcoal.LogPileBlockEntity;
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.knapping.GridPattern;
import dev.strataindustria.knapping.KnappingInput;
import dev.strataindustria.knapping.KnappingMenu;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.machine.BellowsBlock;
import dev.strataindustria.machine.MillstoneBlockEntity;
import dev.strataindustria.power.AxleBlock;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.HandCrankBlockEntity;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.Config;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.SmithingProgress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the tier 0 to 2 core flows (spec 17): knapping and clay forming patterns, alloy rules,
 * the smithing solver and minigame, item heat, the pit kiln, the charcoal pit, and melting and casting.
 * Each test builds what it needs on a small stone platform and drives the block entities' tickers
 * directly, so hour-long burns finish within one game tick.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class ModGameTests {
    public static final DeferredRegister<MapCodec<? extends GameTestInstance>> INSTANCE_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, StrataIndustria.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends GameTestInstance>, MapCodec<Instance>> INSTANCE =
            INSTANCE_TYPES.register("function", () -> Instance.CODEC);

    private static final Identifier PLATFORM = StrataIndustria.id("gametest/platform");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("knapping_patterns", ModGameTests::knappingPatterns);
        TESTS.put("knapping_clicks", ModGameTests::knappingClicks);
        TESTS.put("alloy_rules", ModGameTests::alloyRules);
        TESTS.put("smithing_shapes", ModGameTests::smithingShapes);
        TESTS.put("item_heat", ModGameTests::itemHeat);
        TESTS.put("pit_kiln", ModGameTests::pitKiln);
        TESTS.put("easy_defaults", ModGameTests::easyDefaults);
        TESTS.put("charcoal_pit", ModGameTests::charcoalPit);
        TESTS.put("charcoal_pit_exposed", ModGameTests::charcoalPitExposed);
        TESTS.put("crucible_casting", ModGameTests::crucibleCasting);
        TESTS.put("anvil_smithing", ModGameTests::anvilSmithing);
        TESTS.put("anvil_true_blow", ModGameTests::anvilTrueBlow);
        TESTS.put("anvil_cold_thud", ModGameTests::anvilColdThud);
        TESTS.put("anvil_reheat_keeps_progress", ModGameTests::anvilReheat);
        TESTS.put("anvil_working_heat", ModGameTests::anvilWorkingHeat);
        TESTS.put("anvil_cycle_shape", ModGameTests::anvilCycleShape);
        TESTS.put("anvil_place_and_take", ModGameTests::anvilPlaceAndTake);
        TESTS.put("anvil_weld", ModGameTests::anvilWeld);
        TESTS.put("anvil_quench", ModGameTests::anvilQuench);
        TESTS.put("kinetic_network", ModGameTests::kineticNetwork);
        TESTS.put("core_sample", ModGameTests::coreSample);
        TESTS.put("sluice_washing", ModGameTests::sluiceWashing);
        TESTS.put("step_up_gearbox", ModGameTests::stepUpGearbox);
        TESTS.put("soaking_barrel", ModGameTests::soakingBarrel);
        Tier3GameTests.register(TESTS);
        Tier4GameTests.register(TESTS);
        Tier5GameTests.register(TESTS);
        LogisticsGameTests.register(TESTS);
        Tier6GameTests.register(TESTS);
        PrologueGameTests.register(TESTS);
        ListeningGameTests.register(TESTS);
        GridGameTests.register(TESTS);
        MarkGameTests.register(TESTS);
        LedgerGameTests.register(TESTS);
        MultiblockGameTests.register(TESTS);
        FootGameTests.register(TESTS);
        RailGameTests.register(TESTS);
        OutpostGameTests.register(TESTS);
        BronzeGameTests.register(TESTS);
        BellGameTests.register(TESTS);
        CabinetGameTests.register(TESTS);
        JournalGameTests.register(TESTS);
        FloraGameTests.register(TESTS);
        StructureGameTests.register(TESTS);
        PreviewExport.register(TESTS);
        CollectibleGameTests.register(TESTS);
        SharedBlockGameTests.register(TESTS);
    }

    private ModGameTests() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(StrataIndustria.id("core"));
        for (String name : TESTS.keySet()) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD, PLATFORM,
                    200, 0, true, Rotation.NONE, false, 1, 1, false, 0);
            event.registerTest(StrataIndustria.id(name), new Instance(data, name));
        }
    }

    /** A test that runs one of the functions above, found by name. */
    static final class Instance extends GameTestInstance {
        static final MapCodec<Instance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                TestData.CODEC.fieldOf("data").forGetter(Instance::info),
                Codec.STRING.fieldOf("test").forGetter(t -> t.name)
        ).apply(i, Instance::new));

        private final String name;

        Instance(TestData<Holder<TestEnvironmentDefinition<?>>> data, String name) {
            super(data);
            this.name = name;
        }

        @Override
        public void run(GameTestHelper helper) {
            Consumer<GameTestHelper> test = TESTS.get(name);
            if (test == null) throw helper.assertionException("No test named %s", name);
            test.accept(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal(name);
        }
    }

    // Knapping and clay forming (spec 3.2, 4.1): every pattern finds exactly one recipe, mirrored too.

    private static void knappingPatterns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack rock = new ItemStack(ModItems.LOOSE_ROCK.get(Rock.GRANITE).get());
        ItemStack flint = new ItemStack(Items.FLINT);
        ItemStack clay = new ItemStack(Items.CLAY_BALL);

        Map<ItemStack, List<String[]>> expected = new LinkedHashMap<>();
        List<String[]> stone = List.of(
                pattern(ModItems.STONE_AXE_HEAD.getId().getPath(), ".#...", "####.", "#####", "####.", ".#..."),
                pattern(ModItems.STONE_KNIFE_BLADE.getId().getPath(), "#....", "##...", ".##..", "..##.", "...##"),
                pattern(ModItems.STONE_SHOVEL_HEAD.getId().getPath(), ".###.", ".###.", ".###.", ".###.", "..#.."),
                pattern(ModItems.STONE_HOE_HEAD.getId().getPath(), "#####", "##...", ".....", ".....", "....."),
                pattern(ModItems.STONE_HAMMER_HEAD.getId().getPath(), "#####", "#####", "..#..", ".....", "....."),
                pattern(ModItems.STONE_SPEAR_HEAD.getId().getPath(), "..#..", ".###.", ".###.", "..#..", "..#.."),
                pattern(ModItems.STONE_PICKAXE_HEAD.getId().getPath(), ".###.", "#...#", ".....", ".....", "....."));
        expected.put(flint, stone);
        List<String[]> rockPatterns = new ArrayList<>(stone);
        rockPatterns.add(pattern(ModItems.QUERNSTONE.getId().getPath(), ".###.", "#####", "##.##", "#####", ".###."));
        expected.put(rock, rockPatterns);
        expected.put(clay, List.of(
                pattern("unfired_small_vessel", ".....", ".###.", "#####", "#####", ".###."),
                pattern("unfired_large_vessel", ".###.", "#####", "#####", "#####", ".###."),
                pattern("unfired_crucible", "##.##", "#...#", "#...#", "#...#", "#####"),
                pattern("unfired_ingot_mold", ".....", "#####", "#...#", "#####", "....."),
                pattern("unfired_brick", "##.##", "##.##", ".....", "##.##", "##.##"),
                pattern("unfired_pickaxe_head_mold", "#...#", ".###.", "#####", "#####", "#####"),
                pattern("unfired_axe_head_mold", "#.###", "....#", ".....", "....#", "#.###"),
                pattern("unfired_shovel_head_mold", "#...#", "#...#", "#...#", "#...#", "##.##"),
                pattern("unfired_hoe_head_mold", ".....", "..###", "#####", "#####", "#####"),
                pattern("unfired_knife_blade_mold", ".####", "..###", "#..##", "##..#", "###.."),
                pattern("unfired_hammer_head_mold", ".....", ".....", "##.##", "#####", "#####"),
                pattern("unfired_saw_blade_mold", "#####", "#####", ".....", ".....", "#####"),
                pattern("unfired_sword_blade_mold", "###..", "##..#", "#..##", "..###", ".####")));

        for (var entry : expected.entrySet()) {
            ItemStack material = entry.getKey();
            for (String[] p : entry.getValue()) {
                int mask = GridPattern.parse(List.of(p).subList(1, 6)).getOrThrow();
                for (int shape : new int[] {mask, GridPattern.mirror(mask)}) {
                    List<RecipeHolder<KnappingRecipe>> found = level.recipeAccess().recipeMap()
                            .getRecipesFor(ModRecipes.KNAPPING.get(), new KnappingInput(material, shape), level).toList();
                    helper.assertValueEqual(found.size(), 1, "recipes for " + p[0] + " from " + material.getItem());
                    ItemStack result = found.get(0).value().assemble(new KnappingInput(material, shape));
                    helper.assertValueEqual(result.getItem().builtInRegistryHolder().key().identifier().getPath(), p[0],
                            "knapping result from " + material.getItem());
                }
            }
        }
        // Clay has no stone patterns and stone has no clay patterns.
        int crucible = GridPattern.parse(List.of("##.##", "#...#", "#...#", "#...#", "#####")).getOrThrow();
        helper.assertTrue(level.recipeAccess().recipeMap()
                .getRecipesFor(ModRecipes.KNAPPING.get(), new KnappingInput(flint, crucible), level).findAny().isEmpty(),
                "flint should not form a crucible");
        helper.succeed();
    }

    private static String[] pattern(String result, String... rows) {
        String[] out = new String[6];
        out[0] = result;
        System.arraycopy(rows, 0, out, 1, 5);
        return out;
    }

    // Alloys (spec 7.2 and 7.4): the three example batches, a near miss, and slag at 90%.

    /** Right-click opens the grid exactly once, and a strike from the open menu removes a cell and spends the material. */
    private static void knappingClicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (String stone : new String[] {"loose_basalt", "flint"}) {
            FakePlayer knapper = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "knapper"));
            var item = stone.equals("flint") ? (net.minecraft.world.item.Item) Items.FLINT : ModItems.LOOSE_ROCK.get(Rock.BASALT).get();
            knapper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 16));
            var result = knapper.gameMode.useItem(knapper, level, knapper.getMainHandItem(), InteractionHand.MAIN_HAND);
            helper.assertTrue(result.consumesAction(), stone + ": use should succeed, got " + result);
            // FakePlayer cannot open screens, so the menu is built the way Knapping.tryOpen builds it.
            KnappingMenu menu = new KnappingMenu(7, knapper.getInventory(), knapper.getMainHandItem().copyWithCount(1), InteractionHand.MAIN_HAND);
            knapper.containerMenu = menu;
            int id = menu.containerId;
            helper.assertTrue(menu.isKept(12), stone + ": every cell starts kept");
            helper.assertTrue(menu.clickMenuButton(knapper, 12), stone + ": striking a cell should be accepted");
            helper.assertTrue(!menu.isKept(12), stone + ": the struck cell should be gone");
            helper.assertTrue(menu.hasStarted(), stone + ": the first strike starts the work");
            helper.assertTrue(knapper.containerMenu == menu && knapper.containerMenu.containerId == id, stone + ": the menu stays open");
        }
        helper.succeed();
    }

    private static void alloyRules(GameTestHelper helper) {
        Melt bronze = melt(ModItems.crushedOre(OreMineral.NATIVE_COPPER, OreGrade.NORMAL), 9)
                .plus(melt(ModItems.crushedOre(OreMineral.CASSITERITE, OreGrade.NORMAL), 1));
        helper.assertValueEqual(bronze.total(), 350, "bronze batch units");
        helper.assertValueEqual(Alloy.resultOf(bronze).orElse(null), Metal.BRONZE, "9 copper + 1 cassiterite");

        Melt arsenical = melt(ModItems.crushedOre(OreMineral.TENNANTITE, OreGrade.NORMAL), 5)
                .plus(melt(ModItems.crushedOre(OreMineral.NATIVE_COPPER, OreGrade.NORMAL), 4));
        helper.assertValueEqual(Alloy.resultOf(arsenical).orElse(null), Metal.ARSENICAL_BRONZE, "5 tennantite + 4 copper");

        Melt bismuth = melt(Items.COPPER_INGOT, 3).plus(melt(ModItems.crushedOre(OreMineral.BISMUTHINITE, OreGrade.RICH), 1));
        helper.assertValueEqual(Alloy.resultOf(bismuth).orElse(null), Metal.BISMUTH_BRONZE, "3 copper ingots + 1 rich bismuthinite");

        Melt off = new Melt(Map.of(Metal.COPPER, 87, Metal.TIN, 13), 0);
        helper.assertTrue(Alloy.resultOf(off).isEmpty(), "87% copper and 13% tin should be no known alloy");

        ItemStack slag = new ItemStack(ModItems.ingot(Metal.SLAG_METAL));
        slag.set(ModDataComponents.SLAG.get(), off);
        Melt back = MetalContent.of(slag).orElseThrow();
        helper.assertValueEqual(back.total(), 90, "slag remelt units");

        // A remelted bronze ingot is bronze again.
        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.ingot(Metal.BRONZE), 1)).orElse(null), Metal.BRONZE, "bronze ingot remelt");
        helper.succeed();
    }

    private static Melt melt(net.minecraft.world.item.Item item, int count) {
        Melt one = MetalContent.of(new ItemStack(item)).orElseThrow();
        Melt total = Melt.EMPTY;
        for (int i = 0; i < count; i++) total = total.plus(one);
        return total;
    }

    // Anvil striking (redesign L2): every shape is a few blows, nothing takes more than a dozen.

    private static void smithingShapes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int recipes = 0;
        for (Metal metal : Metal.values()) {
            if (!metal.isToolMetal()) continue;
            ItemStack ingot = new ItemStack(ModItems.ingot(metal), 2);
            for (RecipeHolder<AnvilRecipe> recipe : level.recipeAccess().recipeMap()
                    .getRecipesFor(ModRecipes.ANVIL.get(), new SingleRecipeInput(ingot), level).toList()) {
                int blows = AnvilBlockEntity.blowsFor(recipe.value(), ingot);
                helper.assertTrue(blows >= 1 && blows <= 8, recipe.id().identifier() + " takes " + blows + " blows");
                recipes++;
            }
        }
        helper.assertTrue(recipes >= 40, "expected at least 40 anvil shapes, found " + recipes);
        helper.assertValueEqual(Smithing.totalBlows(5, Metal.COPPER), 5, "copper blows");
        helper.assertValueEqual(Smithing.totalBlows(5, Metal.WROUGHT_IRON), 6, "iron takes one more");
        helper.assertValueEqual(Smithing.totalBlows(5, Metal.STEEL), 7, "steel takes two more");
        helper.succeed();
    }

    // Heat (spec 5.2): the lazy temperature after a minute out of the forge.

    private static void itemHeat(GameTestHelper helper) {
        ItemStack ingot = new ItemStack(Items.COPPER_INGOT);
        Heat.set(ingot, 1000.0f, 0);
        float after = Heat.get(ingot, 1200);
        float expected = (float) (20 + 980 * Math.exp(-0.010 * 60));
        helper.assertTrue(Math.abs(after - expected) < 1.0f, "temperature after 60 s was " + after + ", expected " + expected);
        Heat.set(ingot, 22.0f, 0);
        helper.assertTrue(!ingot.has(ModDataComponents.TEMPERATURE.get()), "a cold stack should forget its temperature");
        helper.succeed();
    }

    // Pit kiln (spec 4.2): a crucible under straw and logs in a pit comes out fired.

    private static void pitKiln(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        for (Direction side : Direction.Plane.HORIZONTAL) level.setBlock(pos.relative(side), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        ItemStack piece = new ItemStack(ModItems.UNFIRED_CRUCIBLE.get());
        helper.assertTrue(PitKilnBlock.placeNew(level, pos, piece), "the crucible should be set out");
        BlockState state = level.getBlockState(pos)
                .setValue(PitKilnBlock.STRAW, PitKilnBlock.MAX_LAYERS).setValue(PitKilnBlock.LOGS, PitKilnBlock.MAX_LAYERS);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        PitKilnBlock kilnBlock = (PitKilnBlock) state.getBlock();
        helper.assertTrue(kilnBlock.canIgnite(level, pos, state), "a thatched kiln in a pit should light");
        helper.assertTrue(kilnBlock.ignite(level, pos, state), "the kiln should catch");

        PitKilnBlockEntity kiln = (PitKilnBlockEntity) level.getBlockEntity(pos);
        for (int tick = 0; tick < 3000 && level.getBlockState(pos).getValue(PitKilnBlock.LIT); tick++) {
            PitKilnBlockEntity.serverTick(level, pos, level.getBlockState(pos), kiln);
        }
        BlockState done = level.getBlockState(pos);
        helper.assertTrue(!done.getValue(PitKilnBlock.LIT), "the kiln should have burnt out");
        helper.assertValueEqual(done.getValue(PitKilnBlock.STRAW), 0, "straw left");
        helper.assertTrue(kiln.items().get(0).is(ModItems.CRUCIBLE.get()), "the crucible should be fired, got " + kiln.items().get(0));

        // An open side keeps a fresh kiln from lighting.
        BlockPos open = helper.absolutePos(new BlockPos(1, 1, 1));
        PitKilnBlock.placeNew(level, open, new ItemStack(ModItems.UNFIRED_BRICK.get()));
        BlockState exposed = level.getBlockState(open)
                .setValue(PitKilnBlock.STRAW, PitKilnBlock.MAX_LAYERS).setValue(PitKilnBlock.LOGS, PitKilnBlock.MAX_LAYERS);
        level.setBlock(open, exposed, Block.UPDATE_ALL);
        helper.assertTrue(!kilnBlock.canIgnite(level, open, exposed), "a kiln without walls should not light");
        helper.succeed();
    }

    // Prologue defaults: hot items do not burn, fired molds last, the kiln fires in about two minutes.

    private static void easyDefaults(GameTestHelper helper) {
        helper.assertTrue(!Config.HEAT_BURN_PLAYER.getAsBoolean(), "hot items should not burn by default");
        ItemStack mold = new ItemStack(ModItems.INGOT_MOLD.get());
        for (int i = 0; i < 200; i++) {
            helper.assertTrue(!CastMoldItem.breaks(mold, helper.getLevel().getRandom()), "a fired mold should not break");
        }
        helper.assertTrue(Config.KILN_BURN_TICKS.getAsInt() <= 3000, "the pit kiln should be short");
        helper.succeed();
    }

    // Charcoal pit (spec 4.4): a sealed 16-log pile gives 8 charcoal and 2 ash; an open one burns away.

    private static BlockPos logPile(GameTestHelper helper, BlockPos local) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(local);
        ItemStack logs = new ItemStack(Items.OAK_LOG, 16);
        helper.assertTrue(LogPileBlock.placeNew(level, pos, logs), "the log pile should be placed");
        LogPileBlockEntity pile = (LogPileBlockEntity) level.getBlockEntity(pos);
        while (!logs.isEmpty() && pile.add(logs)) {}
        helper.assertValueEqual(pile.count(), 16, "logs in the pile");
        return pos;
    }

    private static void burn(ServerLevel level, BlockPos pos, int ticks) {
        BlockState state = level.getBlockState(pos);
        ((LogPileBlock) state.getBlock()).ignite(level, pos, state);
        for (int tick = 0; tick < ticks && level.getBlockState(pos).is(ModBlocks.LOG_PILE.get()); tick++) {
            LogPileBlockEntity.serverTick(level, pos, level.getBlockState(pos), (LogPileBlockEntity) level.getBlockEntity(pos));
        }
    }

    private static void charcoalPit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = logPile(helper, new BlockPos(4, 1, 4));
        for (Direction side : Direction.values()) {
            if (side != Direction.DOWN) level.setBlock(pos.relative(side), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
        burn(level, pos, 12001);
        BlockState result = level.getBlockState(pos);
        helper.assertTrue(result.is(ModBlocks.CHARCOAL_PILE.get()), "the pile should have charred, got " + result);
        helper.assertValueEqual(result.getValue(CharcoalPileBlock.CHARCOAL), 8, "charcoal");
        helper.assertValueEqual(result.getValue(CharcoalPileBlock.ASH), 2, "ash");
        helper.succeed();
    }

    private static void charcoalPitExposed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = logPile(helper, new BlockPos(4, 1, 4));
        burn(level, pos, LogPileBlockEntity.GRACE_TICKS + 40);
        BlockState result = level.getBlockState(pos);
        helper.assertTrue(result.getBlock() instanceof BaseFireBlock, "an uncovered pile should go up in flames, got " + result);
        helper.succeed();
    }

    // Crucible and casting (spec 7.1 and 7.3): copper melts on a forge and pours into an ingot mold.

    private static void crucibleCasting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos forgePos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos cruciblePos = forgePos.above();
        level.setBlock(forgePos, ModBlocks.FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        ForgeBlockEntity forge = (ForgeBlockEntity) level.getBlockEntity(forgePos);
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(cruciblePos);

        forge.setItem(ForgeBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 16));
        BlockState forgeState = level.getBlockState(forgePos);
        helper.assertTrue(((ForgeBlock) forgeState.getBlock()).ignite(level, forgePos, forgeState), "the forge should light");
        crucible.setItem(0, new ItemStack(ModItems.crushedOre(OreMineral.NATIVE_COPPER, OreGrade.NORMAL), 4));

        for (int tick = 0; tick < 12000; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
            if (crucible.getItem(0).isEmpty() && crucible.isMolten()) break;
        }
        helper.assertTrue(crucible.isMolten(), "the copper should be molten at " + crucible.temperature() + " °C");
        helper.assertValueEqual(crucible.melt().total(), 140, "copper units in the melt");
        helper.assertValueEqual(crucible.result().orElse(null), Metal.COPPER, "melt result");

        crucible.setItem(CrucibleBlockEntity.MOLD_SLOT, new ItemStack(ModItems.INGOT_MOLD.get()));
        helper.assertTrue(crucible.pourProblem().isEmpty(), "pour should start, problem: " + crucible.pourProblem().orElse(""));
        helper.assertTrue(crucible.startPour(), "the pour should start");
        for (int tick = 0; tick < 40; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
        }
        ItemStack mold = crucible.getItem(CrucibleBlockEntity.MOLD_SLOT);
        Melt cast = mold.get(ModDataComponents.CAST_CONTENTS.get());
        helper.assertTrue(cast != null, "the mold should be filled");
        helper.assertValueEqual(cast.total(), 100, "units in the mold");
        helper.assertValueEqual(CastMoldItem.castMetal(cast), Metal.COPPER, "cast metal");
        helper.assertValueEqual(crucible.melt().total(), 40, "copper left in the crucible");

        // Not enough left for a second ingot.
        crucible.setItem(CrucibleBlockEntity.MOLD_SLOT, new ItemStack(ModItems.INGOT_MOLD.get()));
        helper.assertValueEqual(crucible.pourProblem().orElse(""), "not_enough", "second pour problem");
        helper.succeed();
    }

    // ---------------------------------------------------------------- the anvil, struck in the world

    private static final long STEP = 20, BEAT = Smithing.BEAT_TICKS;

    private static AnvilBlockEntity anvilAt(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.STONE_ANVILS.get(Rock.BASALT).get().defaultBlockState(), Block.UPDATE_ALL);
        return (AnvilBlockEntity) level.getBlockEntity(pos);
    }

    private static FakePlayer smithWithHammer(ServerLevel level) {
        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        smith.getInventory().setItem(0, new ItemStack(ModItems.STONE_HAMMER.get()));
        return smith;
    }

    private static ItemStack hotIngot(ServerLevel level, net.minecraft.world.item.Item item, float temperature) {
        ItemStack ingot = new ItemStack(item);
        Heat.set(ingot, temperature, level.getGameTime());
        return ingot;
    }

    /** Puts the copper plate shape on the anvil's piece. */
    private static void selectCopperPlate(GameTestHelper helper, ServerLevel level, AnvilBlockEntity anvil, ItemStack ingot) {
        RecipeHolder<AnvilRecipe> plate = level.recipeAccess().recipeMap()
                .getRecipesFor(ModRecipes.ANVIL.get(), new SingleRecipeInput(ingot), level)
                .filter(r -> r.value().result().create().is(ModItems.PLATES.get(Metal.COPPER).get()))
                .findFirst().orElseThrow(() -> helper.assertionException("no copper plate recipe"));
        helper.assertTrue(anvil.select(plate.id()), "the anvil should offer a copper plate");
    }

    private static void strike(AnvilBlockEntity anvil, FakePlayer smith, long... ticks) {
        for (long tick : ticks) anvil.strikeBy(smith, tick);
    }

    // A hot copper ingot struck three times on bright heat makes a +4 plate; blows too close together are ignored.

    private static void anvilSmithing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity anvil = anvilAt(level, helper.absolutePos(new BlockPos(4, 1, 4)));
        FakePlayer smith = smithWithHammer(level);
        ItemStack ingot = hotIngot(level, Items.COPPER_INGOT, 1000.0f);
        anvil.setItem(AnvilBlockEntity.INPUT, ingot);
        selectCopperPlate(helper, level, anvil, ingot);
        helper.assertValueEqual(anvil.status(smith), AnvilBlockEntity.Status.READY, "anvil status");

        strike(anvil, smith, 100, 101, 102);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty(), "three clicks in a row are one blow, so nothing is finished");
        strike(anvil, smith, 100 + STEP);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty(), "two blows are not enough for a plate");
        strike(anvil, smith, 100 + 2 * STEP);

        ItemStack out = anvil.getItem(AnvilBlockEntity.OUTPUT);
        helper.assertTrue(out.is(ModItems.PLATES.get(Metal.COPPER).get()), "the anvil should hold a copper plate, got " + out);
        Quality quality = out.get(ModDataComponents.QUALITY.get());
        helper.assertTrue(quality != null && quality.craft() == Smithing.MAX_CRAFT, "bright blows should give +" + Smithing.MAX_CRAFT + " craft quality, got " + quality);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.INPUT).isEmpty(), "the ingot should be used up");
        helper.succeed();
    }

    // A strike on the glint is a true blow and counts twice: a plate takes two clicks instead of three.

    private static void anvilTrueBlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity anvil = anvilAt(level, helper.absolutePos(new BlockPos(4, 1, 4)));
        FakePlayer smith = smithWithHammer(level);
        ItemStack ingot = hotIngot(level, Items.COPPER_INGOT, 1000.0f);
        anvil.setItem(AnvilBlockEntity.INPUT, ingot);
        selectCopperPlate(helper, level, anvil, ingot);

        strike(anvil, smith, 100);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty(), "one blow does not finish a plate");
        strike(anvil, smith, 100 + BEAT + 3);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).is(ModItems.PLATES.get(Metal.COPPER).get()),
                "the second click was on the glint, so it counts twice and finishes the plate");

        // Outside the window it is a normal blow.
        anvil.setItem(AnvilBlockEntity.OUTPUT, ItemStack.EMPTY);
        ItemStack second = hotIngot(level, Items.COPPER_INGOT, 1000.0f);
        anvil.setItem(AnvilBlockEntity.INPUT, second);
        selectCopperPlate(helper, level, anvil, second);
        strike(anvil, smith, 1000, 1000 + BEAT + 5);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty(), "a click off the glint counts once");
        helper.succeed();
    }

    // Cold metal only thuds: nothing is struck, nothing is lost, and the hammer is not worn.

    private static void anvilColdThud(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity anvil = anvilAt(level, helper.absolutePos(new BlockPos(4, 1, 4)));
        FakePlayer smith = smithWithHammer(level);
        ItemStack ingot = new ItemStack(Items.COPPER_INGOT);
        anvil.setItem(AnvilBlockEntity.INPUT, ingot);
        selectCopperPlate(helper, level, anvil, ingot);
        helper.assertValueEqual(anvil.status(smith), AnvilBlockEntity.Status.TOO_COLD, "a cold ingot");
        strike(anvil, smith, 100, 100 + STEP, 100 + 2 * STEP, 100 + 3 * STEP);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty(), "cold metal does not take a shape");
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.INPUT).is(Items.COPPER_INGOT), "the ingot stays");
        SmithingProgress progress = anvil.getItem(AnvilBlockEntity.INPUT).get(ModDataComponents.SMITHING_PROGRESS.get());
        helper.assertTrue(progress == null || progress.blows() == 0, "no progress on cold metal");
        helper.assertValueEqual(smith.getInventory().getItem(0).getDamageValue(), 0, "the hammer is not worn by a thud");
        helper.succeed();
    }

    // Back to the forge and back again: the piece keeps the blows it has taken.

    private static void anvilReheat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity anvil = anvilAt(level, helper.absolutePos(new BlockPos(4, 1, 4)));
        FakePlayer smith = smithWithHammer(level);
        ItemStack ingot = hotIngot(level, Items.COPPER_INGOT, 1000.0f);
        anvil.setItem(AnvilBlockEntity.INPUT, ingot);
        selectCopperPlate(helper, level, anvil, ingot);
        strike(anvil, smith, 100);

        helper.assertTrue(anvil.take(smith), "an empty hand takes the piece back");
        ItemStack back = smith.getInventory().getItem(1);
        SmithingProgress progress = back.get(ModDataComponents.SMITHING_PROGRESS.get());
        helper.assertTrue(progress != null && progress.blows() == 1, "the piece carries its blow, got " + progress);
        Heat.set(back, 1000.0f, level.getGameTime());
        helper.assertTrue(anvil.place(smith, back), "the reheated piece goes back on the anvil");
        strike(anvil, smith, 200, 200 + STEP);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).is(ModItems.PLATES.get(Metal.COPPER).get()),
                "two more blows finish the plate: 1 + 2 = 3");
        helper.succeed();
    }

    // Working heat (hot enough, not glowing) still forges, but earns no craft quality.

    private static void anvilWorkingHeat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity anvil = anvilAt(level, helper.absolutePos(new BlockPos(4, 1, 4)));
        FakePlayer smith = smithWithHammer(level);
        int working = Metal.COPPER.workingTemperature();
        float dull = working + 10.0f;
        helper.assertTrue(!Smithing.isBright(dull, working), "just over working heat is not bright");
        ItemStack ingot = hotIngot(level, Items.COPPER_INGOT, dull);
        anvil.setItem(AnvilBlockEntity.INPUT, ingot);
        selectCopperPlate(helper, level, anvil, ingot);
        strike(anvil, smith, 100, 100 + STEP, 100 + 2 * STEP);
        ItemStack out = anvil.getItem(AnvilBlockEntity.OUTPUT);
        helper.assertTrue(out.is(ModItems.PLATES.get(Metal.COPPER).get()), "working heat finishes the plate, got " + out);
        Quality quality = out.get(ModDataComponents.QUALITY.get());
        helper.assertTrue(quality != null && quality.craft() == 0, "no bright blows, no craft part, got " + quality);
        helper.assertValueEqual(Smithing.craftQuality(2, 4), 2, "half bright blows");
        helper.succeed();
    }

    // Sneak + hammer walks through the shapes the metal can take, and the anvil remembers the last one.

    private static void anvilCycleShape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity anvil = anvilAt(level, helper.absolutePos(new BlockPos(4, 1, 4)));
        FakePlayer smith = smithWithHammer(level);
        anvil.setItem(AnvilBlockEntity.INPUT, hotIngot(level, Items.COPPER_INGOT, 1000.0f));
        var shapes = anvil.shapes(level);
        helper.assertTrue(shapes.size() >= 4, "copper takes several shapes, found " + shapes.size());
        var first = anvil.current(level).orElseThrow();
        anvil.cycleShape(smith);
        var second = anvil.current(level).orElseThrow();
        helper.assertTrue(!first.key().equals(second.key()), "cycling changes the shape");
        for (int i = 1; i < shapes.size(); i++) anvil.cycleShape(smith);
        helper.assertValueEqual(anvil.current(level).orElseThrow().key(), first.key(), "a full turn comes back round");
        anvil.cycleShape(smith);
        ItemStack taken = anvil.input().copy();
        anvil.take(smith);
        Heat.set(taken, 1000.0f, level.getGameTime());
        taken.remove(ModDataComponents.SMITHING_PROGRESS.get());
        anvil.place(smith, taken);
        helper.assertValueEqual(anvil.current(level).orElseThrow().key(), second.key(), "the anvil starts on the last shape used");
        helper.succeed();
    }

    // An empty hand takes the finished piece first; pieces go down one at a time.

    private static void anvilPlaceAndTake(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity anvil = anvilAt(level, helper.absolutePos(new BlockPos(4, 1, 4)));
        FakePlayer smith = smithWithHammer(level);
        ItemStack held = hotIngot(level, Items.COPPER_INGOT, 900.0f);
        held.setCount(3);
        smith.getInventory().setItem(1, held);
        helper.assertTrue(!anvil.place(smith, new ItemStack(Items.STICK)), "a stick does not go on the anvil");
        helper.assertTrue(anvil.place(smith, held), "an ingot goes down");
        helper.assertValueEqual(held.getCount(), 2, "one ingot placed");
        helper.assertValueEqual(anvil.input().getCount(), 1, "one ingot on the anvil");
        anvil.setItem(AnvilBlockEntity.OUTPUT, new ItemStack(ModItems.PLATES.get(Metal.COPPER).get()));
        anvil.take(smith);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty(), "the finished piece came back first");
        helper.assertTrue(anvil.input().is(Items.COPPER_INGOT), "the ingot is still on the anvil");
        helper.succeed();
    }

    // Two hot iron ingots on the anvil strike together into a double ingot, with no flux.

    private static void anvilWeld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(pos, ModBlocks.WROUGHT_IRON_ANVIL.get().defaultBlockState(), Block.UPDATE_ALL);
        AnvilBlockEntity anvil = (AnvilBlockEntity) level.getBlockEntity(pos);
        FakePlayer smith = smithWithHammer(level);
        float heat = Metal.WROUGHT_IRON.weldingTemperature() + 80.0f;
        smith.getInventory().setItem(1, hotIngot(level, Items.IRON_INGOT, heat));
        smith.getInventory().setItem(2, hotIngot(level, Items.IRON_INGOT, heat));
        helper.assertTrue(anvil.place(smith, smith.getInventory().getItem(1)), "the first ingot goes down");
        helper.assertTrue(anvil.place(smith, smith.getInventory().getItem(2)), "the second goes beside it");
        helper.assertTrue(!anvil.getItem(AnvilBlockEntity.SECOND).isEmpty(), "the second ingot is the weld partner");
        int blows = Smithing.totalBlows(Smithing.WELD_BLOWS, Metal.WROUGHT_IRON);
        for (int i = 0; i < blows - 1; i++) strike(anvil, smith, 100 + i * STEP);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty(), "the weld takes " + blows + " blows");
        strike(anvil, smith, 100 + (blows - 1) * STEP);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).is(ModItems.WROUGHT_IRON_DOUBLE_INGOT.get()),
                "a double ingot, got " + anvil.getItem(AnvilBlockEntity.OUTPUT));
        helper.assertTrue(anvil.input().isEmpty() && anvil.getItem(AnvilBlockEntity.SECOND).isEmpty(), "both ingots are used up");
        helper.succeed();
    }

    // Quench: a hot piece in a water cauldron goes cold at once.

    private static void anvilQuench(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        ItemStack hot = hotIngot(level, Items.COPPER_INGOT, 900.0f);
        helper.assertTrue(Heat.isHot(hot, level), "the ingot starts hot");
        dev.strataindustria.event.SmithingEvents.quenchAt(level, pos, hot);
        helper.assertTrue(Heat.get(hot, level) < Heat.BURN_FROM, "quenched metal is safe to hold, got " + Heat.get(hot, level));
        helper.assertTrue(!hot.has(ModDataComponents.TEMPERATURE.get()), "the heat component is gone");
        helper.succeed();
    }

    // Mechanical power (tier 3 spec 7 and 8.1): a hand crank turns a millstone through an axle; a
    // bellows added to the same network overstresses it.

    private static void kineticNetwork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos millPos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos axlePos = millPos.north();
        BlockPos crankPos = axlePos.north();
        level.setBlock(millPos, ModBlocks.MILLSTONE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(axlePos, ModBlocks.WOODEN_AXLE.get().defaultBlockState().setValue(AxleBlock.AXIS, Direction.Axis.Z), Block.UPDATE_ALL);
        level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.SOUTH),
                Block.UPDATE_ALL);
        MillstoneBlockEntity mill = (MillstoneBlockEntity) level.getBlockEntity(millPos);
        HandCrankBlockEntity crank = (HandCrankBlockEntity) level.getBlockEntity(crankPos);

        KineticNetworks.rebuildNow(level, millPos);
        helper.assertValueEqual(mill.kinetic().status(), KineticState.Status.IDLE, "network before cranking");

        crank.crank(new FakePlayer(level, new GameProfile(UUID.randomUUID(), "miller")));
        KineticNetworks.rebuildNow(level, millPos);
        helper.assertValueEqual(mill.kinetic().status(), KineticState.Status.RUNNING, "network while cranking");
        helper.assertValueEqual(Math.round(mill.kinetic().rpm()), 16, "millstone RPM");
        helper.assertValueEqual(mill.kinetic().load(), 64, "load of one millstone at 16 RPM");

        mill.setItem(MillstoneBlockEntity.INPUT, new ItemStack(Items.BONE));
        for (int tick = 0; tick < 40; tick++) MillstoneBlockEntity.serverTick(level, millPos, level.getBlockState(millPos), mill);
        ItemStack out = mill.getItem(MillstoneBlockEntity.OUTPUT);
        helper.assertTrue(out.is(Items.BONE_MEAL) && out.getCount() == 4, "40 ticks at 16 RPM should grind a bone, got " + out);

        BlockPos bellowsPos = millPos.east();
        level.setBlock(bellowsPos, ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        KineticNetworks.rebuildNow(level, millPos);
        helper.assertValueEqual(mill.kinetic().status(), KineticState.Status.OVERSTRESSED, "network with a bellows added");
        helper.assertValueEqual(Math.round(mill.kinetic().rpm()), 0, "an overstressed network stands still");
        helper.succeed();
    }

    // Core sampler (tier 3 spec 8.5): the core reads the rock under the sampler and the ore in it.

    private static void coreSample(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos samplerPos = helper.absolutePos(new BlockPos(4, 3, 4));
        Block shale = ModBlocks.RAW_ROCK.get(dev.strataindustria.geology.Rock.SHALE).get();
        dev.strataindustria.block.OreBlock hematite = (dev.strataindustria.block.OreBlock) ModBlocks.ORES
                .get(dev.strataindustria.geology.Rock.SHALE).get(OreMineral.HEMATITE).get();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(samplerPos.offset(dx, -1, dz), shale.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(samplerPos.offset(dx, -2, dz), shale.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.setBlock(samplerPos.offset(0, -2, 0), hematite.withGrade(OreGrade.RICH), Block.UPDATE_ALL);
        level.setBlock(samplerPos.offset(1, -2, 0), hematite.withGrade(OreGrade.NORMAL), Block.UPDATE_ALL);
        level.setBlock(samplerPos, ModBlocks.CORE_SAMPLER.get().defaultBlockState(), Block.UPDATE_ALL);

        dev.strataindustria.prospecting.CoreSample sample = dev.strataindustria.prospecting.CoreSample.take(level, samplerPos);
        helper.assertValueEqual(sample.top(), samplerPos.getY() - 1, "core top");
        helper.assertValueEqual(sample.rows().getFirst().name(), shale.getDescriptionId(), "first row");
        helper.assertValueEqual(sample.rows().get(1).name(), shale.getDescriptionId(), "an ore row is named after its host rock");
        var find = sample.finds().stream().filter(f -> f.deposit().equals(OreMineral.HEMATITE.id())).findFirst();
        helper.assertTrue(find.isPresent(), "hematite should be found, got " + sample.finds());
        helper.assertValueEqual(find.get().top(), samplerPos.getY() - 2, "hematite depth");
        helper.assertValueEqual(find.get().count(), 2, "hematite blocks");
        helper.assertValueEqual(find.get().grade(), OreGrade.RICH.ordinal(), "best grade");
        helper.assertValueEqual(sample.mainDeposits().getFirst(), OreMineral.HEMATITE.id(), "main deposit");
        helper.succeed();
    }

    // Sluice (tier 3 spec 8.6 and 11.2): water at the back washes crushed ore into the chest in front, one block down.

    private static void sluiceWashing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sluicePos = helper.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(sluicePos, ModBlocks.SLUICE.get().defaultBlockState().setValue(dev.strataindustria.washing.SluiceBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        level.setBlock(sluicePos.south(), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        BlockPos chestPos = sluicePos.north().below();
        level.setBlock(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        var sluice = (dev.strataindustria.washing.SluiceBlockEntity) level.getBlockEntity(sluicePos);
        sluice.setItem(0, new ItemStack(ModItems.crushedOre(OreMineral.HEMATITE, OreGrade.NORMAL), 2));
        for (int tick = 0; tick < 2 * dev.strataindustria.washing.WashingRecipe.DEFAULT_TICKS; tick++) {
            dev.strataindustria.washing.SluiceBlockEntity.serverTick(level, sluicePos, level.getBlockState(sluicePos), sluice);
        }
        var chest = (net.minecraft.world.Container) level.getBlockEntity(chestPos);
        int washed = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(ModItems.washedOre(OreMineral.HEMATITE, OreGrade.NORMAL))) washed += stack.getCount();
        }
        helper.assertValueEqual(washed, 2, "washed hematite in the chest");
        helper.assertTrue(sluice.getItem(0).isEmpty(), "the buffer should be empty");
        helper.succeed();
    }

    // Step-up gearbox (tier 3 spec 7.3): a crank at 16 RPM turns the axle behind the gearbox at 32.

    private static void stepUpGearbox(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos crankPos = helper.absolutePos(new BlockPos(4, 1, 2));
        BlockPos gearboxPos = crankPos.south();
        BlockPos axlePos = gearboxPos.south();
        level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.SOUTH),
                Block.UPDATE_ALL);
        level.setBlock(gearboxPos, ModBlocks.STEP_UP_GEARBOX.get().defaultBlockState()
                .setValue(dev.strataindustria.power.StepUpGearboxBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        level.setBlock(axlePos, ModBlocks.WOODEN_AXLE.get().defaultBlockState().setValue(AxleBlock.AXIS, Direction.Axis.Z), Block.UPDATE_ALL);
        var axle = (dev.strataindustria.power.Kinetic) level.getBlockEntity(axlePos);
        HandCrankBlockEntity crank = (HandCrankBlockEntity) level.getBlockEntity(crankPos);
        crank.crank(new FakePlayer(level, new GameProfile(UUID.randomUUID(), "miller")));
        KineticNetworks.rebuildNow(level, axlePos);
        helper.assertValueEqual(Math.round(axle.kinetic().rpm()), 32, "axle RPM behind a step-up gearbox");
        helper.succeed();
    }

    // Soaking barrel (tier 3 spec 12.1): water and ash make lye, and lye limes raw hides.

    private static void soakingBarrel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        var sealed = dev.strataindustria.tanning.SoakingBarrelBlock.SEALED;
        level.setBlock(pos, ModBlocks.SOAKING_BARREL.get().defaultBlockState(), Block.UPDATE_ALL);
        var barrel = (dev.strataindustria.tanning.SoakingBarrelBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(barrel.addWater(1000, false), "a bucket of water fits");
        barrel.setItem(dev.strataindustria.tanning.SoakingBarrelBlockEntity.INPUT, new ItemStack(ModItems.ASH.get(), 2));
        level.setBlock(pos, level.getBlockState(pos).setValue(sealed, true), Block.UPDATE_ALL);
        for (int tick = 0; tick < 600; tick++) {
            dev.strataindustria.tanning.SoakingBarrelBlockEntity.serverTick(level, pos, level.getBlockState(pos), barrel);
        }
        helper.assertTrue(barrel.fluid().isSame(dev.strataindustria.registry.ModFluids.LYE.get()), "water and ash should make lye");
        helper.assertValueEqual(barrel.amount(), 1000, "lye in the tank");
        helper.assertTrue(barrel.getItem(dev.strataindustria.tanning.SoakingBarrelBlockEntity.INPUT).isEmpty(), "the ash is used up");

        barrel.setItem(dev.strataindustria.tanning.SoakingBarrelBlockEntity.INPUT, new ItemStack(ModItems.RAW_HIDE.get(), 4));
        for (int tick = 0; tick < 4000; tick++) {
            dev.strataindustria.tanning.SoakingBarrelBlockEntity.serverTick(level, pos, level.getBlockState(pos), barrel);
        }
        ItemStack out = barrel.getItem(dev.strataindustria.tanning.SoakingBarrelBlockEntity.OUTPUT);
        helper.assertTrue(out.is(ModItems.LIMED_HIDE.get()) && out.getCount() == 4, "four limed hides, got " + out);
        helper.assertValueEqual(barrel.amount(), 0, "the lye is used up");
        helper.succeed();
    }
}
