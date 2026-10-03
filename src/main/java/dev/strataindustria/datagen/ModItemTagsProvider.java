package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

final class ModItemTagsProvider extends ItemTagsProvider {
    ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, StrataIndustria.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        for (RockCategory category : RockCategory.values()) {
            var categoryTag = tag(ModTags.Items.rocks(category));
            for (Rock rock : Rock.values()) {
                if (rock.category() == category) categoryTag.add(ModItems.RAW_ROCK.get(rock).getKey());
            }
            tag(ModTags.Items.ROCKS).addTag(ModTags.Items.rocks(category));
            if (category.isIgneous()) tag(ModTags.Items.IGNEOUS_ROCKS).addTag(ModTags.Items.rocks(category));
        }
        tag(Tags.Items.STONES).addTag(ModTags.Items.ROCKS);

        var looseRocks = tag(ModTags.Items.LOOSE_ROCKS);
        var cobbled = tag(Tags.Items.COBBLESTONES);
        var ores = tag(Tags.Items.ORES);
        for (Rock rock : Rock.values()) {
            looseRocks.add(ModItems.LOOSE_ROCK.get(rock).getKey());
            cobbled.add(ModItems.COBBLED_ROCK.get(rock).getKey());
            for (OreMineral mineral : OreMineral.values()) {
                ores.add(ModItems.ORE_BLOCKS.get(rock).get(mineral).getKey());
            }
        }

        var smallOres = tag(ModTags.Items.SMALL_ORES);
        for (OreMineral mineral : OreMineral.values()) {
            smallOres.add(ModItems.SMALL_ORES.get(mineral).getKey());
            var mineralTag = tag(ModTags.Items.ores(mineral.id()));
            for (OreGrade grade : OreGrade.values()) {
                mineralTag.add(ModItems.ORE_PIECES.get(mineral).get(grade).getKey());
            }
        }
    }
}
