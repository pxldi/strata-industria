package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.block.BoulderBlock;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.worldgen.SignPlacement;
import dev.strataindustria.journal.JournalContent;
import dev.strataindustria.journal.JournalState;
import dev.strataindustria.journal.PlantNotes;
import dev.strataindustria.journal.Study;
import dev.strataindustria.knapping.Boulders;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.signs.SignBlocks;
import dev.strataindustria.signs.Stain;
import dev.strataindustria.signs.StainBlock;
import dev.strataindustria.washing.WashingRecipe;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Ore signs (redesign R7): ground stains, coated boulders and black sand, run by {@link ModGameTests}. */
final class SignGameTests {
    private static final int GAP = 20;

    private SignGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("sign_stain_rub", SignGameTests::rub);
        tests.put("sign_coated_boulder", SignGameTests::coatedBoulder);
        tests.put("sign_journal", SignGameTests::journal);
        tests.put("sign_black_sand_pan", SignGameTests::blackSandPan);
        tests.put("sign_placement", SignGameTests::placement);
    }

    private static FakePlayer fake(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "rubber"));
        player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        return player;
    }

    private static int count(GameTestHelper helper, Item... items) {
        int n = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16))) {
            for (Item item : items) if (entity.getItem().is(item)) n += entity.getItem().getCount();
        }
        return n;
    }

    // A stain sits on solid ground, gives way to anything placed on it, and breaks when the ground goes.
    // Rubs climb a step each, a held button repeats too fast to count, and a pause starts the run again.
    private static void rub(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = fake(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockState stain = SignBlocks.STAINS.get(Stain.GOSSAN).get().with(2);
        helper.assertTrue(stain.canSurvive(level, pos), "it rests on solid ground");
        helper.assertTrue(stain.canBeReplaced(), "it gives way to anything placed on it");
        helper.assertTrue(!stain.canSurvive(level, pos.above(5)), "it cannot hang in the air");
        level.setBlock(pos, stain, Block.UPDATE_ALL);
        Vec3 at = Vec3.atCenterOf(pos);
        ServerPlayer asPlayer = player;
        helper.assertValueEqual(StainBlock.rub(level, asPlayer, pos, Stain.GOSSAN, at, GAP), 1, "the first rub is step one");
        helper.assertValueEqual(StainBlock.rub(level, asPlayer, pos, Stain.GOSSAN, at, GAP + 2), 0, "a held button is not another rub");
        helper.assertValueEqual(StainBlock.rub(level, asPlayer, pos, Stain.GOSSAN, at, GAP + 10), 2, "the next one climbs");
        helper.assertValueEqual(StainBlock.rub(level, asPlayer, pos, Stain.GOSSAN, at, GAP + 20), 3, "and the next");
        helper.assertValueEqual(StainBlock.rub(level, asPlayer, pos, Stain.GOSSAN, at, GAP + 200), 1, "a pause starts the run again");
        helper.assertTrue(level.getBlockState(pos).is(SignBlocks.STAINS.get(Stain.GOSSAN).get()), "rubbing leaves the stain: it is the sign");
        helper.succeed();
    }

    // A rust boulder breaks into iron chunks, a green one into copper chunks; stained size stays on the smaller one.
    private static void coatedBoulder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = fake(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(pos, ModBlocks.BOULDER.get(Rock.GRANITE).get().with(1, false, Direction.NORTH, BoulderBlock.Coat.BLOOM), Block.UPDATE_ALL);
        for (int i = 1; i <= 3; i++) Boulders.strike(level, player, pos, level.getBlockState(pos), Vec3.atCenterOf(pos), GAP * i);
        helper.assertTrue(level.getBlockState(pos).isAir(), "it split");
        int copper = count(helper, ModItems.SMALL_ORES.get(OreMineral.MALACHITE).get(), ModItems.SMALL_ORES.get(OreMineral.NATIVE_COPPER).get());
        helper.assertTrue(copper >= 1 && copper <= 2, "one or two copper chunks, not " + copper);
        helper.assertValueEqual(count(helper, ModItems.SMALL_ORES.get(OreMineral.LIMONITE).get()), 0, "and no iron");

        BlockPos rust = helper.absolutePos(new BlockPos(5, 1, 3));
        level.setBlock(rust, ModBlocks.BOULDER.get(Rock.LIMESTONE).get().with(2, false, Direction.NORTH, BoulderBlock.Coat.GOSSAN), Block.UPDATE_ALL);
        for (int i = 1; i <= 3; i++) Boulders.strike(level, player, rust, level.getBlockState(rust), Vec3.atCenterOf(rust), GAP * (10 + i));
        BlockState left = level.getBlockState(rust);
        helper.assertValueEqual(left.getValue(BoulderBlock.SIZE), 1, "a medium boulder leaves a small one");
        helper.assertValueEqual(left.getValue(BoulderBlock.COAT), BoulderBlock.Coat.GOSSAN, "still rust-streaked");
        int ironBefore = count(helper, ModItems.SMALL_ORES.get(OreMineral.HEMATITE).get(), ModItems.SMALL_ORES.get(OreMineral.LIMONITE).get());
        BlockPos clean = helper.absolutePos(new BlockPos(7, 1, 3));
        level.setBlock(clean, ModBlocks.BOULDER.get(Rock.GRANITE).get().with(1, false, Direction.NORTH), Block.UPDATE_ALL);
        for (int i = 1; i <= 3; i++) Boulders.strike(level, player, clean, level.getBlockState(clean), Vec3.atCenterOf(clean), GAP * (20 + i));
        helper.assertValueEqual(count(helper, ModItems.SMALL_ORES.get(OreMineral.HEMATITE).get(), ModItems.SMALL_ORES.get(OreMineral.LIMONITE).get()),
                ironBefore, "a clean boulder gives no ore");
        helper.succeed();
    }

    // Walking past a stain writes its note once and opens the lead for that ore; studying it hints the lead.
    private static void journal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        dev.strataindustria.journal.Leads.refresh(player);
        JournalState state = JournalContent.state(player);
        BlockPos at = player.blockPosition();
        level.setBlock(at.east().below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(at.east(), SignBlocks.STAINS.get(Stain.MALACHITE_BLOOM).get().with(1), 3);
        PlantNotes.scan(player);
        helper.assertTrue(state.seen("sign/malachite_bloom"), "the stain was noticed");
        helper.assertTrue(state.notes().stream().anyMatch(n -> n.key().endsWith("observe.sign.malachite_bloom")), "its note");
        int notes = state.notes().size();
        PlantNotes.scan(player);
        helper.assertValueEqual(state.notes().size(), notes, "once only");
        helper.assertTrue(Study.study(player, at.east()), "studying it did something");
        helper.assertTrue(state.lead("t1/nugget").hinted(), "studying green streaks hints the copper lead");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    // Black sand pans out to tin ore and a little magnetite, and the pan scoops it from the stream bed.
    private static void blackSandPan(GameTestHelper helper) {
        var recipe = WashingRecipe.recipeFor(helper.getLevel(), new ItemStack(ModItems.BLACK_SAND.get()));
        helper.assertTrue(recipe.isPresent(), "black sand can be panned");
        boolean tin = recipe.get().value().chances().stream().anyMatch(c -> c.item().create().is(ModItems.SMALL_ORES.get(OreMineral.CASSITERITE).get()));
        helper.assertTrue(tin, "and holds tin");
        helper.succeed();
    }

    // The placement code puts stains on bare ground, coated boulders beside them, and black sand only on wet sand and gravel.
    private static void placement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        int cx = (origin.getX() + 300) >> 4, cz = (origin.getZ() + 300) >> 4;
        level.getChunk(cx, cz);
        int minX = cx * 16, minZ = cz * 16;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = groundY - 3; y <= groundY + 6; y++) {
                    boolean pond = x >= minX + 10 && z >= minZ + 10;
                    BlockState state = y > groundY ? (pond && y <= groundY + 2 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState())
                            : pond ? (y >= groundY - 1 ? Blocks.SAND.defaultBlockState() : Blocks.DIRT.defaultBlockState())
                            : y == groundY ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
                    level.setBlock(pos.set(x, y, z), state, Block.UPDATE_CLIENTS);
                }
            }
        }
        int placed = SignPlacement.patch(level, minX, minZ, minX + 5, minZ + 5, Stain.GOSSAN, 1234L);
        helper.assertTrue(placed >= 6, "a patch is several blocks, found " + placed);
        int found = 0;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                if (level.getBlockState(pos.set(x, groundY + 1, z)).is(SignBlocks.STAINS.get(Stain.GOSSAN).get())) found++;
            }
        }
        helper.assertValueEqual(found, placed, "each one stands on the ground");

        SignPlacement.coatedGroup(level, net.minecraft.util.RandomSource.create(7), Rock.GRANITE, minX + 3, minZ + 12, BoulderBlock.Coat.GOSSAN);
        int coated = 0;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                BlockState state = level.getBlockState(pos.set(x, groundY + 1, z));
                if (state.getBlock() instanceof BoulderBlock && state.getValue(BoulderBlock.COAT) == BoulderBlock.Coat.GOSSAN) coated++;
            }
        }
        helper.assertTrue(coated >= 1, "a coated boulder stands among the stains' neighbours");

        int sand = SignPlacement.blackSandPatch(level, minX, minZ, minX + 12, minZ + 12, 2);
        helper.assertTrue(sand >= 4, "wet sand turns black, found " + sand);
        helper.assertValueEqual(SignPlacement.blackSandPatch(level, minX, minZ, minX + 3, minZ + 3, 2), 0, "dry grass does not");
        helper.succeed();
    }
}
