package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.grid.ElectricLampBlock;
import dev.strataindustria.grid.GridBlocks;
import dev.strataindustria.grid.LeydenJarBlock;
import dev.strataindustria.registry.Tier5Items;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** Data for the tier 5 extras (uniqueness 2.1, 7.1, 7.3): the stethoscope, the electric lamp and the Leyden jar. */
final class GridData {
    private GridData() {}

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(GridBlocks.STETHOSCOPE.get(), ModelTemplates.FLAT_ITEM);

        MultiVariant off = BlockModelGenerators.plainVariant(StrataIndustria.id("block/electric_lamp_off"));
        MultiVariant on = BlockModelGenerators.plainVariant(StrataIndustria.id("block/electric_lamp_on"));
        PropertyDispatch.C1<MultiVariant, Integer> lamp = PropertyDispatch.initial(ElectricLampBlock.LEVEL);
        for (int level = 0; level <= 15; level++) lamp.select(level, level == 0 ? off : on);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(GridBlocks.ELECTRIC_LAMP.get()).with(lamp));
        itemModels.itemModelOutput.accept(GridBlocks.ELECTRIC_LAMP_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/electric_lamp_off")));

        PropertyDispatch.C1<MultiVariant, Integer> jar = PropertyDispatch.initial(LeydenJarBlock.CHARGE);
        for (int charge = 0; charge <= LeydenJarBlock.SEGMENTS; charge++) {
            jar.select(charge, BlockModelGenerators.plainVariant(StrataIndustria.id("block/leyden_jar_" + charge)));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(GridBlocks.LEYDEN_JAR.get()).with(jar));
        itemModels.itemModelOutput.accept(GridBlocks.LEYDEN_JAR_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/leyden_jar_0")));
    }

    static void lang(BiConsumer<String, String> lang) {
        String id = StrataIndustria.MOD_ID;
        lang.accept("block." + id + ".electric_lamp", "Electric Lamp");
        lang.accept("block." + id + ".leyden_jar", "Leyden Jar");
        lang.accept("item." + id + ".stethoscope", "Mechanic's Stethoscope");
        lang.accept(id + ".leyden_jar.charge", "Leyden jar: %s / %s J");
        lang.accept("subtitles." + id + ".grid.hum", "Mains hum");
        lang.accept("subtitles." + id + ".grid.buzz", "Mains buzz");
        lang.accept("subtitles." + id + ".stethoscope.steady", "Steady beat");
        lang.accept("subtitles." + id + ".stethoscope.strained", "Rough beat");
        lang.accept("subtitles." + id + ".stethoscope.silent", "Dead click");
        lang.accept("subtitles." + id + ".leyden_jar.strike", "Lightning hits the jars");
    }

    static final class Recipes extends net.minecraft.data.recipes.RecipeProvider {
        Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
        }

        private static ResourceKey<Recipe<?>> key(String path) {
            return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
        }

        @Override
        protected void buildRecipes() {
            shaped(RecipeCategory.TOOLS, GridBlocks.STETHOSCOPE.get())
                    .pattern("S S")
                    .pattern("RSR")
                    .pattern(" RP")
                    .define('S', Tier5Items.COPPER_ROD.get())
                    .define('R', Tier5Items.RUBBER.get())
                    .define('P', Tier5Items.LEAD_PLATE.get())
                    .unlockedBy("has_rubber", has(Tier5Items.RUBBER.get()))
                    .save(output, key("stethoscope"));
            shaped(RecipeCategory.REDSTONE, GridBlocks.ELECTRIC_LAMP_ITEM.get())
                    .pattern("G")
                    .pattern("W")
                    .pattern("R")
                    .define('G', Items.GLASS)
                    .define('W', Tier5Items.COPPER_WIRE.get())
                    .define('R', Tier5Items.RUBBER.get())
                    .unlockedBy("has_copper_wire", has(Tier5Items.COPPER_WIRE.get()))
                    .save(output, key("electric_lamp"));
            shaped(RecipeCategory.REDSTONE, GridBlocks.LEYDEN_JAR_ITEM.get())
                    .pattern("C")
                    .pattern("B")
                    .pattern("P")
                    .define('C', Tier5Items.COPPER_ROD.get())
                    .define('B', Items.GLASS_BOTTLE)
                    .define('P', Tier5Items.LEAD_PLATE.get())
                    .unlockedBy("has_lead_plate", has(Tier5Items.LEAD_PLATE.get()))
                    .save(output, key("leyden_jar"));
        }
    }
}
