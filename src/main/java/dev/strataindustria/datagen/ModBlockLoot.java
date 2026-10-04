package dev.strataindustria.datagen;

import dev.strataindustria.charcoal.CharcoalPileBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.Set;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block drop tables. */
final class ModBlockLoot extends BlockLootSubProvider {
    ModBlockLoot(LootTableSubProvider.Context context) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
    }

    @Override
    protected void generate() {
        Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);

        for (Rock rock : Rock.values()) {
            Block raw = ModBlocks.RAW_ROCK.get(rock).get();
            var loose = ModItems.LOOSE_ROCK.get(rock).get();
            // Raw rock breaks into one or two loose rocks; silk touch keeps the block.
            add(raw, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ContextIntProviders.exactly(1))
                            .add(LootItem.lootTableItem(raw).when(hasSilkTouch())
                                    .otherwise(applyExplosionDecay(raw, LootItem.lootTableItem(loose)))))
                    .withPool(LootPool.lootPool()
                            .setRolls(ContextIntProviders.exactly(1))
                            .when(doesNotHaveSilkTouch())
                            .add(applyExplosionDecay(raw, LootItem.lootTableItem(loose)))
                            .when(LootItemRandomChanceCondition.randomChance(0.5f))));
            dropSelf(ModBlocks.COBBLED_ROCK.get(rock).get());
            dropSelf(ModBlocks.LOOSE_ROCK.get(rock).get());

            for (OreMineral mineral : OreMineral.values()) {
                oreDrops(ModBlocks.ORES.get(rock).get(mineral).get(), mineral, fortune);
            }
        }

        for (OreMineral mineral : OreMineral.values()) {
            dropSelf(ModBlocks.SMALL_ORES.get(mineral).get());
        }
        dropOther(ModBlocks.LOOSE_STICK.get(), Items.STICK);
        dropOther(ModBlocks.LOOSE_FLINT.get(), Items.FLINT);
        dropSelf(ModBlocks.FIRE_PIT.get());
        // The pit kiln drops what it holds itself and has no loot table.
        add(ModBlocks.LARGE_VESSEL.get(), createShulkerBoxDrop(ModBlocks.LARGE_VESSEL.get()));
        dropSelf(ModBlocks.CRUCIBLE.get());
        charcoalPile();
    }

    /** Spec 4.4: the pile's state says how much charcoal and ash the burn left. The log pile drops its own logs. */
    private void charcoalPile() {
        Block block = ModBlocks.CHARCOAL_PILE.get();
        LootTable.Builder table = LootTable.lootTable();
        for (int n = 1; n <= CharcoalPileBlock.MAX_CHARCOAL; n++) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .when(MatchBlock.blockMatches(blocks, block,
                            StatePropertiesPredicate.Builder.properties().hasProperty(CharcoalPileBlock.CHARCOAL, n)))
                    .add(LootItem.lootTableItem(Items.CHARCOAL).apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(n)))));
        }
        for (int n = 1; n <= CharcoalPileBlock.MAX_ASH; n++) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .when(MatchBlock.blockMatches(blocks, block,
                            StatePropertiesPredicate.Builder.properties().hasProperty(CharcoalPileBlock.ASH, n)))
                    .add(LootItem.lootTableItem(ModItems.ASH.get()).apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(n)))));
        }
        add(block, table);
    }

    /** One pool per grade: the ore piece of that grade, with fortune adding up to one extra. */
    private void oreDrops(Block block, OreMineral mineral, Holder<Enchantment> fortune) {
        LootTable.Builder table = LootTable.lootTable();
        for (OreGrade grade : OreGrade.values()) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .when(MatchBlock.blockMatches(blocks, block,
                            StatePropertiesPredicate.Builder.properties().hasProperty(OreGrade.PROPERTY, grade)))
                    .add(applyExplosionDecay(block, LootItem.lootTableItem(ModItems.orePiece(mineral, grade))
                            .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1)))));
        }
        add(block, table);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(Block.class::cast)::iterator;
    }
}
