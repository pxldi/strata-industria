package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.ceramics.PitKilnBlock;
import dev.strataindustria.client.HeatGlow;
import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.material.Metal;
import dev.strataindustria.quern.QuernBlock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.AnvilBlock;
import java.util.Optional;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

final class ModModelProvider extends ModelProvider {
    static final TextureSlot ROCK = TextureSlot.create("rock");
    static final TextureSlot ORE = TextureSlot.create("ore");

    static final ModelTemplate ORE_TEMPLATE = template("template_ore", ROCK, ORE);
    static final ModelTemplate LOOSE_ROCK_TEMPLATE = template("template_loose_rock", ROCK);
    static final ModelTemplate SMALL_ORE_TEMPLATE = template("template_small_ore", ORE);
    static final ModelTemplate STONE_ANVIL_TEMPLATE = template("template_stone_anvil", TextureSlot.SIDE, TextureSlot.TOP);
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

        // Fire pit: hand-built models in resources (stone ring, sticks, campfire flames when lit).
        var firePit = StrataIndustria.id("block/fire_pit");
        PropertyDispatch.C1<MultiVariant, Boolean> firePitLit = PropertyDispatch.initial(FirePitBlock.LIT);
        firePitLit.select(false, BlockModelGenerators.plainVariant(firePit));
        firePitLit.select(true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/fire_pit_lit")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.FIRE_PIT.get()).with(firePitLit));
        itemModels.itemModelOutput.accept(ModItems.FIRE_PIT.get(), ItemModelUtils.plainModel(firePit));
        itemModels.generateFlatItem(ModItems.FIRESTARTER.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        clay(blockModels, itemModels);

        // Spec 4.4: one look for the log pile, lit or not, since a lit pile is buried.
        var logPile = ModelTemplates.CUBE_COLUMN.create(ModBlocks.LOG_PILE.get(), TextureMapping.column(ModBlocks.LOG_PILE.get()),
                blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.LOG_PILE.get(), BlockModelGenerators.plainVariant(logPile)));
        blockModels.createTrivialCube(ModBlocks.CHARCOAL_PILE.get());
        flatItem(itemModels, ModItems.ASH.get());

        // Spec 4.5: cold coals, glowing coals, and flames while fuel burns.
        var forge = StrataIndustria.id("block/forge");
        PropertyDispatch.C2<MultiVariant, Boolean, Boolean> forgeState = PropertyDispatch.initial(ForgeBlock.LIT, ForgeBlock.HOT);
        forgeState.select(false, false, BlockModelGenerators.plainVariant(forge));
        forgeState.select(false, true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/forge_hot")));
        forgeState.select(true, false, BlockModelGenerators.plainVariant(StrataIndustria.id("block/forge_lit")));
        forgeState.select(true, true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/forge_lit")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.FORGE.get()).with(forgeState));
        itemModels.itemModelOutput.accept(ModItems.FORGE.get(), ItemModelUtils.plainModel(forge));

        metals(itemModels);

        // Spec 9.1: stone anvils are the raw rock with a dressed face; the bronze anvil turns like a vanilla anvil.
        for (var entry : ModBlocks.STONE_ANVILS.entrySet()) {
            Block anvil = entry.getValue().get();
            var model = STONE_ANVIL_TEMPLATE.create(anvil, new TextureMapping()
                    .put(TextureSlot.SIDE, blockTexture(entry.getKey().id()))
                    .put(TextureSlot.TOP, blockTexture(entry.getKey().id() + "_anvil_top")), blockModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(anvil, BlockModelGenerators.plainVariant(model)));
        }
        var bronzeAnvil = BlockModelGenerators.plainVariant(StrataIndustria.id("block/bronze_anvil"));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> anvilFacing = PropertyDispatch.initial(AnvilBlock.FACING);
        anvilFacing.select(net.minecraft.core.Direction.SOUTH, bronzeAnvil);
        anvilFacing.select(net.minecraft.core.Direction.WEST, bronzeAnvil.with(BlockModelGenerators.Y_ROT_90));
        anvilFacing.select(net.minecraft.core.Direction.NORTH, bronzeAnvil.with(BlockModelGenerators.Y_ROT_180));
        anvilFacing.select(net.minecraft.core.Direction.EAST, bronzeAnvil.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.BRONZE_ANVIL.get()).with(anvilFacing));
        itemModels.itemModelOutput.accept(ModItems.BRONZE_ANVIL.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/bronze_anvil")));
        heatable(itemModels, ModItems.TONGS_JAW.get());
        itemModels.generateFlatItem(ModItems.TONGS.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        // Spec 10.1: the runner stone and its handle turn a quarter at a time as the quern is worked.
        MultiPartGenerator quern = MultiPartGenerator.multiPart(ModBlocks.QUERN.get())
                .with(BlockModelGenerators.plainVariant(StrataIndustria.id("block/quern_base")));
        var runner = BlockModelGenerators.plainVariant(StrataIndustria.id("block/quern_runner"));
        quern.with(BlockModelGenerators.condition().term(QuernBlock.TURN, 0), runner);
        quern.with(BlockModelGenerators.condition().term(QuernBlock.TURN, 1), runner.with(BlockModelGenerators.Y_ROT_90));
        quern.with(BlockModelGenerators.condition().term(QuernBlock.TURN, 2), runner.with(BlockModelGenerators.Y_ROT_180));
        quern.with(BlockModelGenerators.condition().term(QuernBlock.TURN, 3), runner.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(quern);
        itemModels.itemModelOutput.accept(ModItems.QUERN.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/quern")));
        flatItem(itemModels, ModItems.QUERNSTONE.get());
        flatItem(itemModels, ModItems.PLANT_FIBRE.get());
        flatItem(itemModels, ModItems.STRAW.get());
        flatItem(itemModels, ModItems.TWINE.get());
        flatItem(itemModels, ModItems.FIBRE_CLOTH.get());
        flatItem(itemModels, ModItems.FIELD_JOURNAL.get());
        for (var head : java.util.List.of(ModItems.STONE_AXE_HEAD, ModItems.STONE_KNIFE_BLADE, ModItems.STONE_SHOVEL_HEAD,
                ModItems.STONE_HOE_HEAD, ModItems.STONE_HAMMER_HEAD, ModItems.STONE_SPEAR_HEAD, ModItems.STONE_PICKAXE_HEAD)) {
            flatItem(itemModels, head.get());
        }
        for (var tool : java.util.List.of(ModItems.STONE_AXE, ModItems.STONE_KNIFE, ModItems.STONE_SHOVEL,
                ModItems.STONE_HOE, ModItems.STONE_HAMMER, ModItems.STONE_PICKAXE)) {
            itemModels.generateFlatItem(tool.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        }
    }

    // Spec 4.1 to 4.3. The kiln's thatch, logs and fire are hand-built layers; the pieces set out in
    // it are drawn by its block entity renderer.
    private static void clay(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        MultiPartGenerator kiln = MultiPartGenerator.multiPart(ModBlocks.PIT_KILN.get())
                .with(BlockModelGenerators.plainVariant(StrataIndustria.id("block/pit_kiln_base")));
        for (int straw = 1; straw <= PitKilnBlock.MAX_LAYERS; straw++) {
            kiln.with(BlockModelGenerators.condition().term(PitKilnBlock.STRAW, straw),
                    BlockModelGenerators.plainVariant(StrataIndustria.id("block/pit_kiln_thatch_" + straw)));
        }
        for (int log = 1; log <= PitKilnBlock.MAX_LAYERS; log++) {
            Integer[] atLeast = new Integer[PitKilnBlock.MAX_LAYERS - log];
            for (int i = 0; i < atLeast.length; i++) atLeast[i] = log + 1 + i;
            kiln.with(BlockModelGenerators.condition().term(PitKilnBlock.LOGS, log, atLeast),
                    BlockModelGenerators.plainVariant(StrataIndustria.id("block/pit_kiln_log_" + log)));
        }
        kiln.with(BlockModelGenerators.condition(PitKilnBlock.LIT, true),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/pit_kiln_fire")));
        blockModels.blockStateOutput.accept(kiln);

        for (var block : java.util.List.of(ModBlocks.LARGE_VESSEL, ModBlocks.CRUCIBLE)) {
            var model = StrataIndustria.id("block/" + block.getId().getPath());
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get(), BlockModelGenerators.plainVariant(model)));
            // Flat item art, so the unfired and fired pieces share one silhouette (style guide 5).
            flatItem(itemModels, block.get().asItem());
        }

        for (var item : java.util.List.of(ModItems.UNFIRED_SMALL_VESSEL, ModItems.UNFIRED_LARGE_VESSEL, ModItems.UNFIRED_CRUCIBLE,
                ModItems.UNFIRED_INGOT_MOLD, ModItems.UNFIRED_BRICK)) {
            flatItem(itemModels, item.get());
        }
        flatItem(itemModels, ModItems.SMALL_VESSEL.get());
        for (MoldType type : MoldType.values()) {
            flatItem(itemModels, ModItems.UNFIRED_MOLDS.get(type).get());
            castMold(itemModels, ModItems.MOLDS.get(type).get());
        }
        castMold(itemModels, ModItems.INGOT_MOLD.get());
    }

    /** A fired mold shows the cast metal in its cavity once it has been poured. */
    private static void castMold(ItemModelGenerators itemModels, Item mold) {
        var empty = itemModels.createFlatItemModel(mold, ModelTemplates.FLAT_ITEM);
        var filled = itemModels.createFlatItemModel(mold, "_filled", ModelTemplates.FLAT_ITEM);
        itemModels.itemModelOutput.accept(mold, ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(ModDataComponents.CAST_CONTENTS.get()),
                ItemModelUtils.plainModel(filled), ItemModelUtils.plainModel(empty)));
    }

    // Spec 6 to 8: ingots, nuggets, plates and heads are flat; tools are held like vanilla tools.
    private static void metals(ItemModelGenerators itemModels) {
        for (Metal metal : Metal.values()) {
            if (!metal.hasIngot() || metal.isVanilla() && !metal.isToolMetal()) continue;
            if (!metal.isVanilla()) {
                heatable(itemModels, ModItems.ingot(metal));
                if (metal.hasNugget()) heatable(itemModels, ModItems.NUGGETS.get(metal).get());
            }
            if (!metal.isToolMetal()) continue;
            heatable(itemModels, ModItems.PLATES.get(metal).get());
            if (!metal.isVanilla()) {
                for (var piece : ModItems.ARMOUR.get(metal).values()) flatItem(itemModels, piece.get());
            }
            if (ModItems.PROSPECTOR_HEADS.containsKey(metal)) {
                heatable(itemModels, ModItems.PROSPECTOR_HEADS.get(metal).get());
                itemModels.generateFlatItem(ModItems.PROSPECTORS_PICKS.get(metal).get(), ModelTemplates.FLAT_HANDHELD_ITEM);
            }
            for (MoldType type : MoldType.values()) {
                heatable(itemModels, ModItems.head(metal, type));
                if (ModItems.TOOLS.get(metal).get(type) instanceof net.neoforged.neoforge.registries.DeferredItem<?> tool) {
                    itemModels.generateFlatItem(tool.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
                }
            }
        }
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

    /**
     * Metal that can be heated: a flat item, plus a glow layer (a pale copy of the texture under
     * {@code item/glow/}) tinted by temperature once it is hot enough to glow.
     */
    private static void heatable(ItemModelGenerators itemModels, Item item) {
        var cold = itemModels.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
        Identifier name = BuiltInRegistries.ITEM.getKey(item);
        var glowing = ModelTemplates.TWO_LAYERED_ITEM.create(ModelLocationUtils.getModelLocation(item, "_glowing"),
                TextureMapping.layered(TextureMapping.getItemTexture(item),
                        new Material(name.withPrefix("item/glow/"))), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.conditional(HeatGlow.Glowing.INSTANCE,
                ItemModelUtils.tintedModel(glowing, ItemModelUtils.constantTint(-1), HeatGlow.Tint.INSTANCE),
                ItemModelUtils.plainModel(cold)));
    }

    private static void flatItem(ItemModelGenerators itemModels, Item item) {
        itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
    }
}
