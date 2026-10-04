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
            for (OreMineral mineral : OreMineral.inRockValues()) {
                var ore = ModBlocks.ORES.get(rock).get(mineral).getKey();
                ores.add(ore);
                pickaxe.add(ore);
                if (mineral.needsBronzeTool()) tag(ModTags.Blocks.NEEDS_BRONZE_TOOL).add(ore);
                else if (mineral.needsCopperTool()) needsCopper.add(ore);
                if (mineral.isIron()) tag(Tags.Blocks.ORES_IRON).add(ore);
                if (mineral == OreMineral.NATIVE_GOLD) tag(Tags.Blocks.ORES_GOLD).add(ore);
            }
        }

        var smallOres = tag(ModTags.Blocks.SMALL_ORES);
        for (OreMineral mineral : OreMineral.values()) {
            smallOres.add(ModBlocks.SMALL_ORES.get(mineral).getKey());
        }

        // Tier 3 spec 3, 4.2 and 4.3: fire clay, fire bricks, bog iron, lignite and river placers.
        ores.add(ModBlocks.BOG_IRON.getKey());
        tag(Tags.Blocks.ORES_IRON).add(ModBlocks.BOG_IRON.getKey());
        tag(ModTags.Blocks.NEEDS_BRONZE_TOOL).add(ModBlocks.BOG_IRON.getKey());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.BOG_IRON.getKey()).add(ModBlocks.FIRE_CLAY.getKey())
                .add(ModBlocks.PLACER_GRAVEL.getKey()).add(ModBlocks.PLACER_SAND.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.LIGNITE_SEAM.getKey()).add(ModBlocks.FIRE_BRICKS.getKey())
                .add(ModBlocks.BLOOMERY.getKey())
                .add(ModBlocks.FIRE_BRICK_SLAB.getKey()).add(ModBlocks.FIRE_BRICK_STAIRS.getKey()).add(ModBlocks.FIRE_BRICK_WALL.getKey());
        // Tier 3 spec 7 and 8: wooden power parts take an axe, the millstone a pickaxe.
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.WOODEN_AXLE.getKey()).add(ModBlocks.WOODEN_GEARBOX.getKey())
                .add(ModBlocks.HAND_CRANK.getKey()).add(ModBlocks.WATER_WHEEL.getKey()).add(ModBlocks.BELLOWS.getKey())
                .add(ModBlocks.SAW_MILL.getKey()).add(ModBlocks.TRIP_HAMMER.getKey())
                .add(ModBlocks.CORE_SAMPLER.getKey()).add(ModBlocks.SLUICE.getKey()).add(ModBlocks.STEP_UP_GEARBOX.getKey())
                .add(ModBlocks.PULLEY.getKey()).add(ModBlocks.WINDMILL_BEARING.getKey()).add(ModBlocks.WINDMILL_SAIL.getKey())
                .add(ModBlocks.SOAKING_BARREL.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.MILLSTONE.getKey());
        tag(BlockTags.SLABS).add(ModBlocks.FIRE_BRICK_SLAB.getKey());
        tag(BlockTags.STAIRS).add(ModBlocks.FIRE_BRICK_STAIRS.getKey());
        tag(BlockTags.WALLS).add(ModBlocks.FIRE_BRICK_WALL.getKey());
        tag(ModTags.Blocks.REFRACTORY).add(ModBlocks.FIRE_BRICKS.getKey());
        tag(ModTags.Blocks.PROSPECTABLE).add(ModBlocks.LIGNITE_SEAM.getKey()).add(ModBlocks.FIRE_CLAY.getKey())
                .add(ModBlocks.BOG_IRON.getKey());

        // Mining tiers: stone < copper < bronze < wrought iron < steel. Vanilla diamond and above still apply.
        tag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(ModTags.Blocks.NEEDS_STEEL_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_WROUGHT_IRON_TOOL)
                .addTag(ModTags.Blocks.NEEDS_STEEL_TOOL)
                .addTag(BlockTags.NEEDS_DIAMOND_TOOL);
        tag(BlockTags.INCORRECT_FOR_IRON_TOOL).addTag(ModTags.Blocks.NEEDS_STEEL_TOOL);
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
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.LARGE_VESSEL.getKey()).add(ModBlocks.CRUCIBLE.getKey())
                .add(ModBlocks.FORGE.getKey()).add(ModBlocks.QUERN.getKey()).add(ModBlocks.BRONZE_ANVIL.getKey())
                .add(ModBlocks.WROUGHT_IRON_ANVIL.getKey());
        for (var anvil : ModBlocks.STONE_ANVILS.values()) tag(BlockTags.MINEABLE_WITH_PICKAXE).add(anvil.getKey());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.PIT_KILN.getKey()).add(ModBlocks.CHARCOAL_PILE.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.LOG_PILE.getKey());
    }
}
