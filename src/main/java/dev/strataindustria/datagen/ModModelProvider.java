package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.Optional;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

final class ModModelProvider extends ModelProvider {
    static final TextureSlot ROCK = TextureSlot.create("rock");
    static final TextureSlot ORE = TextureSlot.create("ore");

    static final ModelTemplate ORE_TEMPLATE = template("template_ore", ROCK, ORE);
    static final ModelTemplate LOOSE_ROCK_TEMPLATE = template("template_loose_rock", ROCK);
    static final ModelTemplate SMALL_ORE_TEMPLATE = template("template_small_ore", ORE);
    static final ModelTemplate GROUND_FLAT_TEMPLATE = template("template_ground_flat", TextureSlot.TEXTURE);

    ModModelProvider(PackOutput output) {
        super(output, StrataIndustria.MOD_ID);
    }

    private static ModelTemplate template(String name, TextureSlot... slots) {
        return new ModelTemplate(Optional.of(StrataIndustria.id("block/" + name)), Optional.empty(), slots);
    }

    static Material blockTexture(String path) {
        return new Material(StrataIndustria.id("block/" + path));
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (Rock rock : Rock.values()) {
            blockModels.createTrivialCube(ModBlocks.RAW_ROCK.get(rock).get());
            blockModels.createTrivialCube(ModBlocks.COBBLED_ROCK.get(rock).get());

            Block loose = ModBlocks.LOOSE_ROCK.get(rock).get();
            var looseModel = LOOSE_ROCK_TEMPLATE.create(loose, TextureMapping.singleSlot(ROCK, blockTexture(rock.id())), blockModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(loose,
                    BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(looseModel))));
            flatItem(itemModels, ModItems.LOOSE_ROCK.get(rock).get());

            for (OreMineral mineral : OreMineral.values()) {
                oreBlock(blockModels, rock, mineral);
            }
        }

        for (OreMineral mineral : OreMineral.values()) {
            Block small = ModBlocks.SMALL_ORES.get(mineral).get();
            var model = SMALL_ORE_TEMPLATE.create(small, TextureMapping.singleSlot(ORE, blockTexture("small_" + mineral.id())), blockModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(small,
                    BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(model))));
            flatItem(itemModels, ModItems.SMALL_ORES.get(mineral).get());
            for (OreGrade grade : OreGrade.values()) {
                flatItem(itemModels, ModItems.orePiece(mineral, grade));
                flatItem(itemModels, ModItems.crushedOre(mineral, grade));
            }
        }

        groundFlat(blockModels, ModBlocks.LOOSE_STICK.get(), "loose_stick");
        groundFlat(blockModels, ModBlocks.LOOSE_FLINT.get(), "loose_flint");

        flatItem(itemModels, ModItems.PLANT_FIBRE.get());
    }

    private static void oreBlock(BlockModelGenerators blockModels, Rock rock, OreMineral mineral) {
        Block block = ModBlocks.ORES.get(rock).get(mineral).get();
        PropertyDispatch.C1<MultiVariant, OreGrade> dispatch = PropertyDispatch.initial(OreGrade.PROPERTY);
        for (OreGrade grade : OreGrade.values()) {
            TextureMapping textures = new TextureMapping()
                    .put(ROCK, blockTexture(rock.id()))
                    .put(ORE, blockTexture("ore/" + mineral.id() + "_" + grade.getSerializedName()));
            // The normal grade uses the plain block model location, which the block item also uses.
            var model = grade == OreGrade.NORMAL
                    ? ORE_TEMPLATE.create(block, textures, blockModels.modelOutput)
                    : ORE_TEMPLATE.createWithSuffix(block, "_" + grade.getSerializedName(), textures, blockModels.modelOutput);
            dispatch.select(grade, BlockModelGenerators.plainVariant(model));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
    }

    private static void groundFlat(BlockModelGenerators blockModels, Block block, String texture) {
        var model = GROUND_FLAT_TEMPLATE.create(block, TextureMapping.singleSlot(TextureSlot.TEXTURE, blockTexture(texture)), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block,
                BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(model))));
    }

    private static void flatItem(ItemModelGenerators itemModels, Item item) {
        itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
    }
}
