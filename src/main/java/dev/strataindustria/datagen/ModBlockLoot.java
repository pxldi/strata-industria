package dev.strataindustria.datagen;

import dev.strataindustria.registry.ModBlocks;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/** Block drop tables. */
final class ModBlockLoot {
    private ModBlockLoot() {}

    static void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        dropSelf(output, ModBlocks.FIRE_BRICKS.get());
    }

    private static void dropSelf(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output, Block block) {
        block.getLootTable().ifPresent(key -> output.accept(key, singleItem(block)));
    }

    private static LootTable.Builder singleItem(ItemLike item) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(item))
                .when(ExplosionCondition.survivesExplosion()));
    }
}
