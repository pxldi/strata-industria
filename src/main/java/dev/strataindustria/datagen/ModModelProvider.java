package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.bloomery.BloomeryBlock;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.ceramics.PitKilnBlock;
import dev.strataindustria.client.HeatGlow;
import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.machine.BellowsBlock;
import dev.strataindustria.machine.SawMillBlock;
import dev.strataindustria.machine.TripHammerBlock;
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
import net.minecraft.data.BlockFamily;
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

            for (OreMineral mineral : OreMineral.inRockValues()) {
                oreBlock(blockModels, rock, mineral);
            }
        }

        for (OreMineral mineral : OreMineral.values()) {
            Block small = ModBlocks.SMALL_ORES.get(mineral).get();
            var model = SMALL_ORE_TEMPLATE.create(small, TextureMapping.singleSlot(ORE, blockTexture("small_" + mineral.id())), blockModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(small,
                    BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(model))));
            if (!mineral.hasPieces()) continue;
            flatItem(itemModels, ModItems.SMALL_ORES.get(mineral).get());
            for (OreGrade grade : OreGrade.values()) {
                flatItem(itemModels, ModItems.orePiece(mineral, grade));
                flatItem(itemModels, ModItems.crushedOre(mineral, grade));
                if (mineral.washable()) flatItem(itemModels, ModItems.washedOre(mineral, grade));
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

        StructureData.models(blockModels, itemModels);
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
        tier4(itemModels);
        ironAge(blockModels, itemModels);
        kinetics(blockModels, itemModels);
        windAndBelts(blockModels, itemModels);

        // Spec 9.1: stone anvils are the raw rock with a dressed face; the bronze anvil turns like a vanilla anvil.
        for (var entry : ModBlocks.STONE_ANVILS.entrySet()) {
            Block anvil = entry.getValue().get();
            var model = STONE_ANVIL_TEMPLATE.create(anvil, new TextureMapping()
                    .put(TextureSlot.SIDE, blockTexture(entry.getKey().id()))
                    .put(TextureSlot.TOP, blockTexture(entry.getKey().id() + "_anvil_top")), blockModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(anvil, BlockModelGenerators.plainVariant(model)));
        }
        metalAnvil(blockModels, itemModels, ModBlocks.BRONZE_ANVIL.get(), ModItems.BRONZE_ANVIL.get(), "bronze_anvil");
        metalAnvil(blockModels, itemModels, ModBlocks.WROUGHT_IRON_ANVIL.get(), ModItems.WROUGHT_IRON_ANVIL.get(), "wrought_iron_anvil");
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
            if (metal.hasPlate()) heatable(itemModels, ModItems.PLATES.get(metal).get());
            if (ModItems.RODS.containsKey(metal)) heatable(itemModels, ModItems.RODS.get(metal).get());
            if (ModItems.GEARS.containsKey(metal)) heatable(itemModels, ModItems.GEARS.get(metal).get());
            if (!metal.isToolMetal()) continue;
            if (!metal.isVanilla()) {
                for (var piece : ModItems.ARMOUR.get(metal).values()) flatItem(itemModels, piece.get());
            }
            if (ModItems.PROSPECTOR_HEADS.containsKey(metal)) {
                heatable(itemModels, ModItems.PROSPECTOR_HEADS.get(metal).get());
                itemModels.generateFlatItem(ModItems.PROSPECTORS_PICKS.get(metal).get(), ModelTemplates.FLAT_HANDHELD_ITEM);
            }
            for (MoldType type : metal.toolTypes()) {
                heatable(itemModels, ModItems.head(metal, type));
                if (ModItems.TOOLS.get(metal).get(type) instanceof net.neoforged.neoforge.registries.DeferredItem<?> tool) {
                    itemModels.generateFlatItem(tool.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
                }
            }
        }
    }

    // Tier 3 spec 3 and 4: fire clay and fire bricks, the single-block deposits, and wrought iron forms.
    private static void ironAge(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(ModBlocks.FIRE_CLAY.get());
        BlockFamily fireBricks = new BlockFamily.Builder(ModBlocks.FIRE_BRICKS.get())
                .slab(ModBlocks.FIRE_BRICK_SLAB.get())
                .stairs(ModBlocks.FIRE_BRICK_STAIRS.get())
                .wall(ModBlocks.FIRE_BRICK_WALL.get())
                .getFamily();
        blockModels.family(ModBlocks.FIRE_BRICKS.get()).generateFor(fireBricks);
        blockModels.createTrivialCube(ModBlocks.LIGNITE_SEAM.get());
        blockModels.createTrivialCube(ModBlocks.PLACER_GRAVEL.get());
        blockModels.createTrivialCube(ModBlocks.PLACER_SAND.get());

        Block bog = ModBlocks.BOG_IRON.get();
        PropertyDispatch.C1<MultiVariant, OreGrade> bogGrades = PropertyDispatch.initial(OreGrade.PROPERTY);
        for (OreGrade grade : OreGrade.values()) {
            TextureMapping texture = TextureMapping.singleSlot(TextureSlot.ALL, blockTexture("bog_iron_" + grade.getSerializedName()));
            var model = grade == OreGrade.NORMAL
                    ? ModelTemplates.CUBE_ALL.create(bog, texture, blockModels.modelOutput)
                    : ModelTemplates.CUBE_ALL.createWithSuffix(bog, "_" + grade.getSerializedName(), texture, blockModels.modelOutput);
            bogGrades.select(grade, BlockModelGenerators.plainVariant(model));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(bog).with(bogGrades));

        for (var item : java.util.List.of(ModItems.FIRE_CLAY_BALL, ModItems.GROG, ModItems.UNFIRED_FIRE_BRICK, ModItems.FIRE_BRICK,
                ModItems.LIGNITE)) {
            flatItem(itemModels, item.get());
        }
        flatItem(itemModels, ModItems.BLOOMERY_SLAG.get());
        heatable(itemModels, ModItems.RAW_BLOOM.get());

        // Spec 5.1: the controller is a fire brick block with the door on its front; the door glows when lit.
        Block bloomery = ModBlocks.BLOOMERY.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.FRONT, blockTexture("bloomery_front"))
                .put(TextureSlot.SIDE, blockTexture("fire_bricks")).put(TextureSlot.TOP, blockTexture("fire_bricks"));
        TextureMapping hot = cold.copyAndUpdate(TextureSlot.FRONT, blockTexture("bloomery_front_lit"));
        var coldModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(bloomery, cold, blockModels.modelOutput));
        var hotModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(bloomery, "_lit", hot,
                blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> bloomeryState =
                PropertyDispatch.initial(BloomeryBlock.FACING, BloomeryBlock.LIT);
        for (boolean lit : new boolean[] {false, true}) {
            var base = lit ? hotModel : coldModel;
            bloomeryState.select(net.minecraft.core.Direction.NORTH, lit, base);
            bloomeryState.select(net.minecraft.core.Direction.EAST, lit, base.with(BlockModelGenerators.Y_ROT_90));
            bloomeryState.select(net.minecraft.core.Direction.SOUTH, lit, base.with(BlockModelGenerators.Y_ROT_180));
            bloomeryState.select(net.minecraft.core.Direction.WEST, lit, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(bloomery).with(bloomeryState));
        itemModels.itemModelOutput.accept(ModItems.BLOOMERY.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/bloomery")));

        heatable(itemModels, ModItems.WROUGHT_IRON_ROD.get());
        heatable(itemModels, ModItems.WROUGHT_IRON_DOUBLE_INGOT.get());

        // Spec 9.4 and 9.5: flux, and a pattern that shows its notes once a sequence is recorded on it.
        flatItem(itemModels, ModItems.FLUX.get());
        Item pattern = ModItems.SMITHING_PATTERN.get();
        var blank = itemModels.createFlatItemModel(pattern, ModelTemplates.FLAT_ITEM);
        var recorded = itemModels.createFlatItemModel(pattern, "_recorded", ModelTemplates.FLAT_ITEM);
        itemModels.itemModelOutput.accept(pattern, ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(ModDataComponents.SMITHING_PATTERN.get()),
                ItemModelUtils.plainModel(recorded), ItemModelUtils.plainModel(blank)));
    }

    /**
     * Tier 3 spec 7 and 8. Turning parts are drawn by the rotor renderer, so the axle, crank and wheel
     * blocks carry particle-only models; the hand-written models live in the main resources.
     */
    private static void kinetics(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (var block : java.util.List.of(ModBlocks.WOODEN_AXLE, ModBlocks.WOODEN_GEARBOX, ModBlocks.HAND_CRANK, ModBlocks.WATER_WHEEL,
                ModBlocks.MILLSTONE)) {
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get(),
                    BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + block.getId().getPath()))));
        }
        itemModels.itemModelOutput.accept(ModItems.WOODEN_AXLE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/rotor/wooden_axle")));
        itemModels.itemModelOutput.accept(ModItems.WOODEN_GEARBOX.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/wooden_gearbox")));
        itemModels.itemModelOutput.accept(ModItems.MILLSTONE.get(), ItemModelUtils.composite(
                ItemModelUtils.plainModel(StrataIndustria.id("block/millstone")),
                ItemModelUtils.plainModel(StrataIndustria.id("block/rotor/millstone_runner"))));
        flatItem(itemModels, ModItems.HAND_CRANK.get());
        flatItem(itemModels, ModItems.WATER_WHEEL.get());
        flatItem(itemModels, ModItems.WOODEN_GEAR.get());

        // The bellows model faces north and squashes while it blows.
        var open = BlockModelGenerators.plainVariant(StrataIndustria.id("block/bellows"));
        var squeezed = BlockModelGenerators.plainVariant(StrataIndustria.id("block/bellows_compressed"));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> bellows =
                PropertyDispatch.initial(BellowsBlock.FACING, BellowsBlock.COMPRESSED);
        for (boolean compressed : new boolean[] {false, true}) {
            var base = compressed ? squeezed : open;
            bellows.select(net.minecraft.core.Direction.NORTH, compressed, base);
            bellows.select(net.minecraft.core.Direction.EAST, compressed, base.with(BlockModelGenerators.Y_ROT_90));
            bellows.select(net.minecraft.core.Direction.SOUTH, compressed, base.with(BlockModelGenerators.Y_ROT_180));
            bellows.select(net.minecraft.core.Direction.WEST, compressed, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.BELLOWS.get()).with(bellows));
        itemModels.itemModelOutput.accept(ModItems.BELLOWS.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/bellows")));

        // Spec 8.2 and 8.4: both face north in their models.
        var sawIdle = BlockModelGenerators.plainVariant(StrataIndustria.id("block/saw_mill"));
        var sawActive = BlockModelGenerators.plainVariant(StrataIndustria.id("block/saw_mill_active"));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> saw =
                PropertyDispatch.initial(SawMillBlock.FACING, SawMillBlock.ACTIVE);
        for (boolean active : new boolean[] {false, true}) {
            var base = active ? sawActive : sawIdle;
            saw.select(net.minecraft.core.Direction.NORTH, active, base);
            saw.select(net.minecraft.core.Direction.EAST, active, base.with(BlockModelGenerators.Y_ROT_90));
            saw.select(net.minecraft.core.Direction.SOUTH, active, base.with(BlockModelGenerators.Y_ROT_180));
            saw.select(net.minecraft.core.Direction.WEST, active, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.SAW_MILL.get()).with(saw));
        itemModels.itemModelOutput.accept(ModItems.SAW_MILL.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/saw_mill")));
        var frame = BlockModelGenerators.plainVariant(StrataIndustria.id("block/trip_hammer"));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> hammer = PropertyDispatch.initial(TripHammerBlock.FACING);
        hammer.select(net.minecraft.core.Direction.NORTH, frame);
        hammer.select(net.minecraft.core.Direction.EAST, frame.with(BlockModelGenerators.Y_ROT_90));
        hammer.select(net.minecraft.core.Direction.SOUTH, frame.with(BlockModelGenerators.Y_ROT_180));
        hammer.select(net.minecraft.core.Direction.WEST, frame.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.TRIP_HAMMER.get()).with(hammer));
        itemModels.itemModelOutput.accept(ModItems.TRIP_HAMMER.get(), ItemModelUtils.composite(
                ItemModelUtils.plainModel(StrataIndustria.id("block/trip_hammer")),
                ItemModelUtils.plainModel(StrataIndustria.id("block/trip_hammer_arm"))));
        flatItem(itemModels, ModItems.BARK.get());
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.CORE_SAMPLER.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/core_sampler"))));
        itemModels.itemModelOutput.accept(ModItems.CORE_SAMPLER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/core_sampler")));
        flatItem(itemModels, ModItems.CORE_SAMPLE.get());

        // Tier 3 spec 11: the sluice faces north in its models; the pan shows its load.
        var dry = BlockModelGenerators.plainVariant(StrataIndustria.id("block/sluice"));
        var wet = BlockModelGenerators.plainVariant(StrataIndustria.id("block/sluice_wet"));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> sluice =
                PropertyDispatch.initial(dev.strataindustria.washing.SluiceBlock.FACING, dev.strataindustria.washing.SluiceBlock.WET);
        for (boolean flowing : new boolean[] {false, true}) {
            var base = flowing ? wet : dry;
            sluice.select(net.minecraft.core.Direction.NORTH, flowing, base);
            sluice.select(net.minecraft.core.Direction.EAST, flowing, base.with(BlockModelGenerators.Y_ROT_90));
            sluice.select(net.minecraft.core.Direction.SOUTH, flowing, base.with(BlockModelGenerators.Y_ROT_180));
            sluice.select(net.minecraft.core.Direction.WEST, flowing, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.SLUICE.get()).with(sluice));
        itemModels.itemModelOutput.accept(ModItems.SLUICE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/sluice")));
        Item washingPan = ModItems.WASHING_PAN.get();
        var emptyPan = itemModels.createFlatItemModel(washingPan, ModelTemplates.FLAT_ITEM);
        var loadedPan = itemModels.createFlatItemModel(washingPan, "_loaded", ModelTemplates.FLAT_ITEM);
        itemModels.itemModelOutput.accept(washingPan, ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(ModDataComponents.PAN_CONTENTS.get()),
                ItemModelUtils.plainModel(loadedPan), ItemModelUtils.plainModel(emptyPan)));
    }

    /** Metal anvils turn like a vanilla anvil; the model JSON is hand-written on the vanilla anvil template. */
    private static void metalAnvil(BlockModelGenerators blockModels, ItemModelGenerators itemModels, Block block, Item item, String name) {
        var model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + name));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> facing = PropertyDispatch.initial(AnvilBlock.FACING);
        facing.select(net.minecraft.core.Direction.SOUTH, model);
        facing.select(net.minecraft.core.Direction.WEST, model.with(BlockModelGenerators.Y_ROT_90));
        facing.select(net.minecraft.core.Direction.NORTH, model.with(BlockModelGenerators.Y_ROT_180));
        facing.select(net.minecraft.core.Direction.EAST, model.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(facing));
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(StrataIndustria.id("block/" + name)));
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

    // Tier 4 spec 4.6 and 21.4: materials.
    private static void tier4(ItemModelGenerators itemModels) {
        heatable(itemModels, ModItems.STEEL_DOUBLE_INGOT.get());
        for (var item : java.util.List.of(dev.strataindustria.registry.Tier4Items.SULFUR, dev.strataindustria.registry.Tier4Items.SULFUR_DUST,
                dev.strataindustria.registry.Tier4Items.CHARCOAL_DUST)) {
            flatItem(itemModels, item.get());
        }
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

    /**
     * Tier 3 spec 7.2 to 7.4. The step-up gearbox and the windmill bearing face north in their models
     * and the sail lies across the z axis; pulleys and their belts are drawn by the pulley renderer.
     */
    private static void windAndBelts(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        var gearbox = BlockModelGenerators.plainVariant(StrataIndustria.id("block/step_up_gearbox"));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> stepUp =
                PropertyDispatch.initial(dev.strataindustria.power.StepUpGearboxBlock.FACING);
        stepUp.select(net.minecraft.core.Direction.NORTH, gearbox);
        stepUp.select(net.minecraft.core.Direction.EAST, gearbox.with(BlockModelGenerators.Y_ROT_90));
        stepUp.select(net.minecraft.core.Direction.SOUTH, gearbox.with(BlockModelGenerators.Y_ROT_180));
        stepUp.select(net.minecraft.core.Direction.WEST, gearbox.with(BlockModelGenerators.Y_ROT_270));
        stepUp.select(net.minecraft.core.Direction.UP, gearbox.with(BlockModelGenerators.X_ROT_270));
        stepUp.select(net.minecraft.core.Direction.DOWN, gearbox.with(BlockModelGenerators.X_ROT_90));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.STEP_UP_GEARBOX.get()).with(stepUp));
        itemModels.itemModelOutput.accept(ModItems.STEP_UP_GEARBOX.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/step_up_gearbox")));

        var bearing = BlockModelGenerators.plainVariant(StrataIndustria.id("block/windmill_bearing"));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> bearings =
                PropertyDispatch.initial(dev.strataindustria.power.WindmillBearingBlock.FACING);
        bearings.select(net.minecraft.core.Direction.NORTH, bearing);
        bearings.select(net.minecraft.core.Direction.EAST, bearing.with(BlockModelGenerators.Y_ROT_90));
        bearings.select(net.minecraft.core.Direction.SOUTH, bearing.with(BlockModelGenerators.Y_ROT_180));
        bearings.select(net.minecraft.core.Direction.WEST, bearing.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.WINDMILL_BEARING.get()).with(bearings));
        itemModels.itemModelOutput.accept(ModItems.WINDMILL_BEARING.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/windmill_bearing")));

        var sail = BlockModelGenerators.plainVariant(StrataIndustria.id("block/windmill_sail"));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction.Axis, Boolean> sails = PropertyDispatch.initial(
                dev.strataindustria.power.WindmillSailBlock.AXIS, dev.strataindustria.power.WindmillSailBlock.ATTACHED);
        for (boolean attached : new boolean[] {false, true}) {
            sails.select(net.minecraft.core.Direction.Axis.Z, attached, sail);
            sails.select(net.minecraft.core.Direction.Axis.X, attached, sail.with(BlockModelGenerators.Y_ROT_90));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.WINDMILL_SAIL.get()).with(sails));
        itemModels.itemModelOutput.accept(ModItems.WINDMILL_SAIL.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/windmill_sail")));

        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.PULLEY.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/pulley"))));
        itemModels.itemModelOutput.accept(ModItems.PULLEY.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/rotor/pulley")));
        flatItem(itemModels, ModItems.LEATHER_BELT.get());

        // Spec 12.1: the soaking barrel shows its lid when sealed; the fluid is drawn by its renderer.
        PropertyDispatch.C1<MultiVariant, Boolean> barrel = PropertyDispatch.initial(dev.strataindustria.tanning.SoakingBarrelBlock.SEALED);
        barrel.select(false, BlockModelGenerators.plainVariant(StrataIndustria.id("block/soaking_barrel")));
        barrel.select(true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/soaking_barrel_sealed")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.SOAKING_BARREL.get()).with(barrel));
        itemModels.itemModelOutput.accept(ModItems.SOAKING_BARREL.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/soaking_barrel_sealed")));
        flatItem(itemModels, ModItems.RAW_HIDE.get());
        flatItem(itemModels, ModItems.LIMED_HIDE.get());
        flatItem(itemModels, ModItems.SCRAPED_HIDE.get());
    }

    private static void flatItem(ItemModelGenerators itemModels, Item item) {
        itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
    }
}
