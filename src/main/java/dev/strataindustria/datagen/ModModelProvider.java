package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

final class ModModelProvider extends ModelProvider {
    ModModelProvider(PackOutput output) {
        super(output, StrataIndustria.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(ModBlocks.FIRE_BRICKS.get());

        itemModels.generateFlatItem(ModItems.PLANT_FIBRE.get(), ModelTemplates.FLAT_ITEM);
    }
}
