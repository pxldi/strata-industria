package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

final class ModBlockTagsProvider extends BlockTagsProvider {
    ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, StrataIndustria.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        for (RockCategory category : RockCategory.values()) {
            var categoryTag = tag(ModTags.Blocks.rocks(category));
            for (Rock rock : Rock.values()) {
                if (rock.category() == category) categoryTag.add(ModBlocks.RAW_ROCK.get(rock).getKey());
            }
            tag(ModTags.Blocks.ROCKS).addTag(ModTags.Blocks.rocks(category));
            if (category.isIgneous()) tag(ModTags.Blocks.IGNEOUS_ROCKS).addTag(ModTags.Blocks.rocks(category));
        }

        // Raw rock behaves like vanilla stone for every later worldgen feature and for mob spawning.
        for (TagKey<Block> stoneLike : java.util.List.of(
                BlockTags.BASE_STONE_OVERWORLD,
                BlockTags.STONE_ORE_REPLACEABLES,
                BlockTags.DRIPSTONE_REPLACEABLE,
                BlockTags.MOSS_REPLACEABLE,
                BlockTags.LUSH_GROUND_REPLACEABLE,
                BlockTags.AZALEA_ROOT_REPLACEABLE,
                BlockTags.SCULK_REPLACEABLE,
                BlockTags.SCULK_REPLACEABLE_WORLD_GEN,
                BlockTags.MINEABLE_WITH_PICKAXE,
                Tags.Blocks.STONES)) {
            tag(stoneLike).addTag(ModTags.Blocks.ROCKS);
        }

        var looseRocks = tag(ModTags.Blocks.LOOSE_ROCKS);
        var cobbled = tag(Tags.Blocks.COBBLESTONES);
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var ores = tag(Tags.Blocks.ORES);
        var needsCopper = tag(ModTags.Blocks.NEEDS_COPPER_TOOL);
        for (Rock rock : Rock.values()) {
            looseRocks.add(ModBlocks.LOOSE_ROCK.get(rock).getKey());
            cobbled.add(ModBlocks.COBBLED_ROCK.get(rock).getKey());
            pickaxe.add(ModBlocks.COBBLED_ROCK.get(rock).getKey());
            for (OreMineral mineral : OreMineral.values()) {
                var ore = ModBlocks.ORES.get(rock).get(mineral).getKey();
                ores.add(ore);
                pickaxe.add(ore);
                if (mineral == OreMineral.CASSITERITE) needsCopper.add(ore);
            }
        }

        var smallOres = tag(ModTags.Blocks.SMALL_ORES);
        for (OreMineral mineral : OreMineral.values()) {
            smallOres.add(ModBlocks.SMALL_ORES.get(mineral).getKey());
        }

        // Mining tiers: stone < copper < bronze < wrought iron. Vanilla iron and above still apply.
        tag(ModTags.Blocks.NEEDS_BRONZE_TOOL);
        tag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL)
                .addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL)
                .addTag(BlockTags.NEEDS_DIAMOND_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL)
                .addTag(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL)
                .addTag(BlockTags.NEEDS_IRON_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_STONE_TOOL)
                .addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.INCORRECT_FOR_COPPER_TOOL);
        // The vanilla copper pickaxe is the copper tier; vanilla wood and stone tools sit below it.
        tag(BlockTags.INCORRECT_FOR_COPPER_TOOL).addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(BlockTags.INCORRECT_FOR_STONE_TOOL).addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL).addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(BlockTags.INCORRECT_FOR_GOLD_TOOL).addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);

        // Knives cut grass for fibre and straw, and go through leaves quickly.
        var fibrePlants = tag(ModTags.Blocks.FIBRE_PLANTS);
        for (Block plant : java.util.List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
                Blocks.SHORT_DRY_GRASS, Blocks.TALL_DRY_GRASS, Blocks.BUSH)) {
            fibrePlants.add(plant.builtInRegistryHolder().key());
        }
        tag(ModTags.Blocks.MINEABLE_WITH_KNIFE).addTag(ModTags.Blocks.FIBRE_PLANTS).addTag(BlockTags.LEAVES);
        tag(ModTags.Blocks.MINEABLE_WITH_HAMMER);
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.FIRE_PIT.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.LARGE_VESSEL.getKey()).add(ModBlocks.CRUCIBLE.getKey());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.PIT_KILN.getKey()).add(ModBlocks.CHARCOAL_PILE.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.LOG_PILE.getKey());
    }
}
