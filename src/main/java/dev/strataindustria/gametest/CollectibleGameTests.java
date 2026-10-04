package dev.strataindustria.gametest;

import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.structure.CampLoot;
import dev.strataindustria.structure.CampStructure;
import dev.strataindustria.structure.Ledgers;
import dev.strataindustria.structure.MineralSpecimenItem;
import dev.strataindustria.structure.SpecimenShelfBlockEntity;
import dev.strataindustria.structure.StructureContent;
import dev.strataindustria.survey.SurveyNotesItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

/** Collectibles, ledger pages and the loot tables that hand them out (structures v2 section 4), run by {@link ModGameTests}. */
final class CollectibleGameTests {
    private CollectibleGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("collectible_registries", CollectibleGameTests::registries);
        tests.put("collectible_specimen_shelf", CollectibleGameTests::specimenShelf);
        tests.put("collectible_ledger_pages", CollectibleGameTests::ledgerPages);
        tests.put("collectible_loot_tables", CollectibleGameTests::lootTables);
    }

    /** Pot patterns, trim patterns and the banner pattern load from the data pack, and the items point at them. */
    private static void registries(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (String place : StructureContent.SHERD_PLACES) {
            helper.assertTrue(access.lookupOrThrow(Registries.DECORATED_POT_PATTERN).get(StructureContent.potPattern(place)).isPresent(),
                    "pot pattern " + place + " is missing");
            ItemStack sherd = new ItemStack(StructureContent.SHERDS.get(place).get());
            helper.assertTrue(sherd.get(DataComponents.PROVIDES_POTTERY_PATTERN) != null, place + " sherd gives no pot pattern");
        }
        for (var key : List.of(StructureContent.MINER_TRIM, StructureContent.SMITH_TRIM)) {
            helper.assertTrue(access.lookupOrThrow(Registries.TRIM_PATTERN).get(key).isPresent(), key.identifier() + " trim is missing");
        }
        helper.assertTrue(access.lookupOrThrow(Registries.BANNER_PATTERN).get(StructureContent.PICK_AND_HAMMER).isPresent(),
                "banner pattern is missing");
        ItemStack banner = new ItemStack(StructureContent.PICK_AND_HAMMER_BANNER_PATTERN.get());
        var provided = banner.get(DataComponents.PROVIDES_BANNER_PATTERNS);
        helper.assertTrue(provided != null, "banner pattern item provides nothing");
        helper.succeed();
    }

    /** Four cubbies; only specimens go in; four different minerals make a collection. */
    private static void specimenShelf(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState shelf = StructureContent.SPECIMEN_SHELF.get().defaultBlockState();
        helper.setBlock(pos, shelf);
        helper.assertTrue(helper.getBlockEntity(pos, SpecimenShelfBlockEntity.class) != null, "shelf has no block entity");
        SpecimenShelfBlockEntity entity = helper.getBlockEntity(pos, SpecimenShelfBlockEntity.class);
        helper.assertTrue(!entity.place(new ItemStack(Items.STONE)), "a stone went onto the shelf");
        OreMineral[] minerals = {OreMineral.CASSITERITE, OreMineral.MALACHITE, OreMineral.HEMATITE, OreMineral.NATIVE_GOLD};
        for (int i = 0; i < minerals.length; i++) {
            helper.assertTrue(!entity.isFullCollection(), "collection complete too early");
            helper.assertTrue(entity.place(MineralSpecimenItem.of(minerals[i])), "cubby " + i + " refused a specimen");
        }
        helper.assertTrue(entity.isFullCollection(), "four different minerals do not count as a collection");
        helper.assertTrue(!entity.place(MineralSpecimenItem.of(OreMineral.MALACHITE)), "a fifth specimen fit");
        ItemStack back = entity.takeLast();
        helper.assertTrue(MineralSpecimenItem.mineral(back).equals("native_gold"), "took the wrong specimen");
        helper.assertTrue(!entity.isFullCollection(), "collection still complete after taking one");
        helper.succeed();
    }

    /** Every place's ledger pages are readable notes with a text key, and the survey screen can tell them apart. */
    private static void ledgerPages(GameTestHelper helper) {
        for (CampStructure.Layout layout : CampStructure.Layout.values()) {
            helper.assertTrue(Ledgers.PAGES.getOrDefault(layout.id(), 0) >= 2, layout.id() + " has fewer than two ledger pages");
            for (int page = 1; page <= Ledgers.PAGES.get(layout.id()); page++) {
                ItemStack stack = Ledgers.page(Ledgers.key(layout.id(), page));
                helper.assertTrue(Ledgers.key(layout.id(), page).equals(SurveyNotesItem.ledger(stack)), "page key not kept");
            }
        }
        helper.assertTrue(SurveyNotesItem.ledger(new ItemStack(StructureContent.SURVEY_NOTES.get())) == null, "blank notes read as a ledger");
        helper.succeed();
    }

    /** The best table of each place exists, rolls without error, and its ledger page and collectibles are in it. */
    private static void lootTables(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> problems = new ArrayList<>();
        List<ResourceKey<LootTable>> caches = List.of(CampLoot.key(CampLoot.CLEARING_CACHE), CampLoot.key(CampLoot.MINING_CACHE),
                CampLoot.key(CampLoot.BLOOMERY_CACHE), CampLoot.key(CampLoot.PLACER_CACHE),
                CampLoot.key(CampLoot.PROSPECTOR_CACHE, OreMineral.CASSITERITE), CampLoot.key(CampLoot.ADIT_CACHE, OreMineral.CASSITERITE));
        for (ResourceKey<LootTable> key : caches) {
            LootTable table = server.reloadableRegistries().getLootTable(key);
            if (table == LootTable.EMPTY) {
                problems.add("missing " + key.identifier());
                continue;
            }
            boolean ledger = false;
            for (int roll = 0; roll < 40 && !ledger; roll++) {
                LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(
                        net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                        .create(LootContextParamSets.CHEST);
                for (ItemStack stack : table.getRandomItems(params)) ledger |= SurveyNotesItem.ledger(stack) != null;
            }
            if (!ledger) problems.add(key.identifier() + " never gave a ledger page in 40 rolls");
        }
        helper.assertTrue(problems.isEmpty(), "loot problems: " + problems);
        helper.succeed();
    }
}
