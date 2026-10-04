package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.bloomery.BloomeryBlock;
import dev.strataindustria.ceramics.MoldType;
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
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Items;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
        Tier5Models.register(blockModels, itemModels);
        Tier6Models.register(blockModels, itemModels);
        GridData.models(blockModels, itemModels);
        FootData.models(blockModels, itemModels);
        RailData.models(blockModels, itemModels);
        RailwayData.models(blockModels, itemModels);
        RopewayData.models(blockModels, itemModels);
        LogisticsData.models(blockModels, itemModels);
        tier6(blockModels, itemModels);
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

        // Indicator plants: cross models, and the flat item uses the block texture.
        for (var plant : dev.strataindustria.flora.FloraBlocks.PLANTS.values()) {
            Block block = plant.get();
            var model = ModelTemplates.CROSS.create(block, TextureMapping.cross(block), blockModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model)));
            var item = ModelTemplates.FLAT_ITEM.create(block.asItem(), TextureMapping.layer0(block), blockModels.modelOutput);
            itemModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(item));
        }

        // Fire pit: hand-built models in resources (stone ring, sticks, campfire flames when lit).
        var firePit = StrataIndustria.id("block/fire_pit");
        PropertyDispatch.C1<MultiVariant, Boolean> firePitLit = PropertyDispatch.initial(FirePitBlock.LIT);
        firePitLit.select(false, BlockModelGenerators.plainVariant(firePit));
        firePitLit.select(true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/fire_pit_lit")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.FIRE_PIT.get()).with(firePitLit));
        itemModels.itemModelOutput.accept(ModItems.FIRE_PIT.get(), ItemModelUtils.plainModel(firePit));

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
        tier4(blockModels, itemModels);
        prologue(blockModels, itemModels);
        ironAge(blockModels, itemModels);
        kinetics(blockModels, itemModels);
        windAndBelts(blockModels, itemModels);

        // Spec 9.1: stone anvils are the raw rock with a dressed face; the iron anvil turns like a vanilla anvil.
        for (var entry : ModBlocks.STONE_ANVILS.entrySet()) {
            Block anvil = entry.getValue().get();
            var model = STONE_ANVIL_TEMPLATE.create(anvil, new TextureMapping()
                    .put(TextureSlot.SIDE, blockTexture(entry.getKey().id()))
                    .put(TextureSlot.TOP, blockTexture(entry.getKey().id() + "_anvil_top")), blockModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(anvil, BlockModelGenerators.plainVariant(model)));
        }
        metalAnvil(blockModels, itemModels, ModBlocks.IRON_ANVIL.get(), ModItems.IRON_ANVIL.get(), "iron_anvil");
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

    // Spec 4.1 to 4.3: the crucible and the unfired pieces. Pieces on the fire pit hearth are drawn by its block entity renderer.
    private static void clay(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        var crucibleModel = StrataIndustria.id("block/crucible");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.CRUCIBLE.get(), BlockModelGenerators.plainVariant(crucibleModel)));
        // Flat item art, so the unfired and fired pieces share one silhouette (style guide 5).
        flatItem(itemModels, ModBlocks.CRUCIBLE.get().asItem());

        for (var item : java.util.List.of(ModItems.UNFIRED_CRUCIBLE,
                ModItems.UNFIRED_INGOT_MOLD, ModItems.UNFIRED_BRICK)) {
            flatItem(itemModels, item.get());
        }
        for (MoldType type : MoldType.values()) {
            flatItem(itemModels, ModItems.UNFIRED_MOLDS.get(type).get());
            castMold(itemModels, ModItems.MOLDS.get(type).get());
        }
        castMold(itemModels, ModItems.INGOT_MOLD.get());
        flatItem(itemModels, dev.strataindustria.registry.PatternRegistry.PATTERN_BLANK.get());
        flatItem(itemModels, dev.strataindustria.registry.PatternRegistry.SAND_FLASK.get());
        itemModels.generateFlatItem(dev.strataindustria.mark.MarkRegistry.MAKER_PUNCH.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        flatItem(itemModels, dev.strataindustria.ledger.LedgerRegistry.BUILDERS_LEDGER.get());
        for (var pattern : dev.strataindustria.registry.PatternRegistry.PATTERNS.values()) flatItem(itemModels, pattern.get());
        for (var mold : dev.strataindustria.registry.PatternRegistry.SAND_MOLDS.values()) castMold(itemModels, mold.get());
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
        blockModels.createTrivialCube(ModBlocks.BAUXITE_BED.get());
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
                ModItems.LIGNITE, ModItems.BAUXITE)) {
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

        flatItem(itemModels, ModItems.FLUX.get());
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
        // Tier 4 spec 10.5: the steam hammer's frame turns with it; its ram is drawn by the renderer.
        var steamHammer = BlockModelGenerators.plainVariant(StrataIndustria.id("block/steam_hammer"));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> steamHammerState =
                PropertyDispatch.initial(dev.strataindustria.steam.SteamHammerBlock.FACING, dev.strataindustria.steam.SteamHammerBlock.ACTIVE);
        for (boolean active : new boolean[] {false, true}) {
            steamHammerState.select(net.minecraft.core.Direction.NORTH, active, steamHammer);
            steamHammerState.select(net.minecraft.core.Direction.EAST, active, steamHammer.with(BlockModelGenerators.Y_ROT_90));
            steamHammerState.select(net.minecraft.core.Direction.SOUTH, active, steamHammer.with(BlockModelGenerators.Y_ROT_180));
            steamHammerState.select(net.minecraft.core.Direction.WEST, active, steamHammer.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.registry.Tier4Blocks.STEAM_HAMMER.get()).with(steamHammerState));
        itemModels.itemModelOutput.accept(dev.strataindustria.registry.Tier4Items.STEAM_HAMMER.get(), ItemModelUtils.composite(
                ItemModelUtils.plainModel(StrataIndustria.id("block/steam_hammer")),
                ItemModelUtils.plainModel(StrataIndustria.id("block/steam_hammer_ram"))));
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
    /** The brick kiln faces the player and its door glows while it fires; the casting table is a hand-built model. */
    private static void prologue(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block kiln = dev.strataindustria.registry.PrologueRegistry.BRICK_KILN.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.FRONT, blockTexture("brick_kiln_front"))
                .put(TextureSlot.SIDE, blockTexture("brick_kiln_side")).put(TextureSlot.TOP, blockTexture("brick_kiln_top"));
        var idle = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(kiln, cold, blockModels.modelOutput));
        var lit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(kiln, "_active",
                cold.copyAndUpdate(TextureSlot.FRONT, blockTexture("brick_kiln_front_active")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> state = PropertyDispatch.initial(
                dev.strataindustria.ceramics.BrickKilnBlock.FACING, dev.strataindustria.ceramics.BrickKilnBlock.LIT);
        for (boolean on : new boolean[] {false, true}) {
            var base = on ? lit : idle;
            state.select(net.minecraft.core.Direction.NORTH, on, base);
            state.select(net.minecraft.core.Direction.EAST, on, base.with(BlockModelGenerators.Y_ROT_90));
            state.select(net.minecraft.core.Direction.SOUTH, on, base.with(BlockModelGenerators.Y_ROT_180));
            state.select(net.minecraft.core.Direction.WEST, on, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(kiln).with(state));
        itemModels.itemModelOutput.accept(dev.strataindustria.registry.PrologueRegistry.BRICK_KILN_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/brick_kiln")));

        var table = StrataIndustria.id("block/casting_table");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.registry.PrologueRegistry.CASTING_TABLE.get(),
                BlockModelGenerators.plainVariant(table)));
        itemModels.itemModelOutput.accept(dev.strataindustria.registry.PrologueRegistry.CASTING_TABLE_ITEM.get(), ItemModelUtils.plainModel(table));
    }

    private static void tier4(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        heatable(itemModels, ModItems.STEEL_DOUBLE_INGOT.get());
        for (var item : java.util.List.of(Tier4Items.SULFUR, Tier4Items.SULFUR_DUST, Tier4Items.CHARCOAL_DUST, Tier4Items.COKE,
                Tier4Items.COKE_DUST, Tier4Items.UNFIRED_COKE_OVEN_BRICK, Tier4Items.COKE_OVEN_BRICK, Tier4Items.CREOSOTE_BUCKET,
                Tier4Items.TREATED_STICK)) {
            flatItem(itemModels, item.get());
        }
        blockModels.createTrivialCube(Tier4Blocks.COKE_OVEN_BRICKS.get());
        blockModels.createTrivialCube(Tier4Blocks.COKE_BLOCK.get());

        // Spec 5.1: the door on a brick body; the lit door glows through its peephole and seams.
        Block door = Tier4Blocks.COKE_OVEN_DOOR.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.FRONT, blockTexture("coke_oven_door"))
                .put(TextureSlot.SIDE, blockTexture("coke_oven_bricks")).put(TextureSlot.TOP, blockTexture("coke_oven_bricks"));
        TextureMapping hot = cold.copyAndUpdate(TextureSlot.FRONT, blockTexture("coke_oven_door_lit"));
        var coldModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(door, cold, blockModels.modelOutput));
        var hotModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(door, "_lit", hot, blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> doorState =
                PropertyDispatch.initial(dev.strataindustria.coking.CokeOvenBlock.FACING, dev.strataindustria.coking.CokeOvenBlock.LIT);
        for (boolean lit : new boolean[] {false, true}) {
            var base = lit ? hotModel : coldModel;
            doorState.select(net.minecraft.core.Direction.NORTH, lit, base);
            doorState.select(net.minecraft.core.Direction.EAST, lit, base.with(BlockModelGenerators.Y_ROT_90));
            doorState.select(net.minecraft.core.Direction.SOUTH, lit, base.with(BlockModelGenerators.Y_ROT_180));
            doorState.select(net.minecraft.core.Direction.WEST, lit, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(door).with(doorState));

        BlockFamily treated = new BlockFamily.Builder(Tier4Blocks.TREATED_PLANKS.get())
                .slab(Tier4Blocks.TREATED_SLAB.get())
                .stairs(Tier4Blocks.TREATED_STAIRS.get())
                .fence(Tier4Blocks.TREATED_FENCE.get())
                .getFamily();
        blockModels.family(Tier4Blocks.TREATED_PLANKS.get()).generateFor(treated);

        // Spec 6.1: the refractory crucible has the clay crucible's shape; its molds work like the clay ones.
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier4Blocks.REFRACTORY_CRUCIBLE.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/refractory_crucible"))));
        flatItem(itemModels, Tier4Items.REFRACTORY_CRUCIBLE.get());
        for (var item : java.util.List.of(Tier4Items.UNFIRED_REFRACTORY_CRUCIBLE, Tier4Items.UNFIRED_REFRACTORY_INGOT_MOLD,
                Tier4Items.UNFIRED_REFRACTORY_GEAR_MOLD, Tier4Items.UNFIRED_GEAR_MOLD)) {
            flatItem(itemModels, item.get());
        }
        castMold(itemModels, Tier4Items.REFRACTORY_INGOT_MOLD.get());
        castMold(itemModels, Tier4Items.REFRACTORY_GEAR_MOLD.get());
        castMold(itemModels, Tier4Items.GEAR_MOLD.get());
        flatItem(itemModels, dev.strataindustria.bronze.BronzeRegistry.UNFIRED_BELL_MOLD.get());
        castMold(itemModels, dev.strataindustria.bronze.BronzeRegistry.BELL_MOLD.get());

        // Spec 4.4 and 14.4: zinc calcines.
        for (var calcine : Tier4Items.ZINC_CALCINES.values()) flatItem(itemModels, calcine.get());
        flatItem(itemModels, Tier4Items.SMALL_ZINC_CALCINE.get());

        // Spec 11.7: iron transmission, in the wooden parts' models.
        for (var block : java.util.List.of(Tier4Blocks.IRON_AXLE, Tier4Blocks.IRON_GEARBOX)) {
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get(),
                    BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + block.getId().getPath()))));
        }
        itemModels.itemModelOutput.accept(Tier4Items.IRON_AXLE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/rotor/iron_axle")));
        itemModels.itemModelOutput.accept(Tier4Items.IRON_GEARBOX.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/iron_gearbox")));
        var ironStepUp = BlockModelGenerators.plainVariant(StrataIndustria.id("block/iron_step_up_gearbox"));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> ironFacing =
                PropertyDispatch.initial(dev.strataindustria.power.StepUpGearboxBlock.FACING);
        ironFacing.select(net.minecraft.core.Direction.NORTH, ironStepUp);
        ironFacing.select(net.minecraft.core.Direction.EAST, ironStepUp.with(BlockModelGenerators.Y_ROT_90));
        ironFacing.select(net.minecraft.core.Direction.SOUTH, ironStepUp.with(BlockModelGenerators.Y_ROT_180));
        ironFacing.select(net.minecraft.core.Direction.WEST, ironStepUp.with(BlockModelGenerators.Y_ROT_270));
        ironFacing.select(net.minecraft.core.Direction.UP, ironStepUp.with(BlockModelGenerators.X_ROT_270));
        ironFacing.select(net.minecraft.core.Direction.DOWN, ironStepUp.with(BlockModelGenerators.X_ROT_90));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier4Blocks.IRON_STEP_UP_GEARBOX.get()).with(ironFacing));
        itemModels.itemModelOutput.accept(Tier4Items.IRON_STEP_UP_GEARBOX.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/iron_step_up_gearbox")));
        steam(blockModels, itemModels);
    }

    // Spec 8.1, 9.2, 9.3 and 10.2: the firebox and boiler face the player; pipes join on any face.
    private static void steam(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block firebox = Tier4Blocks.FIREBOX.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.FRONT, blockTexture("firebox_front"))
                .put(TextureSlot.SIDE, blockTexture("firebox_side")).put(TextureSlot.TOP, blockTexture("firebox_top"));
        var unlit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(firebox, cold, blockModels.modelOutput));
        var lit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(firebox, "_lit",
                cold.copyAndUpdate(TextureSlot.FRONT, blockTexture("firebox_front_lit")), blockModels.modelOutput));
        var hot = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(firebox, "_lit_hot",
                cold.copyAndUpdate(TextureSlot.FRONT, blockTexture("firebox_front_lit_hot")), blockModels.modelOutput));
        PropertyDispatch.C3<MultiVariant, net.minecraft.core.Direction, Boolean, Boolean> fireboxState = PropertyDispatch.initial(
                dev.strataindustria.steam.FireboxBlock.FACING, dev.strataindustria.steam.FireboxBlock.LIT, dev.strataindustria.steam.FireboxBlock.HOT);
        for (boolean on : new boolean[] {false, true}) {
            for (boolean white : new boolean[] {false, true}) {
                var base = !on ? unlit : white ? hot : lit;
                horizontal(fireboxState, on, white, base);
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(firebox).with(fireboxState));
        itemModels.itemModelOutput.accept(Tier4Items.FIREBOX.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/firebox")));

        for (var boiler : java.util.List.of(Tier4Blocks.BRONZE_BOILER, Tier4Blocks.CRACKED_BRONZE_BOILER)) {
            String name = boiler.getId().getPath();
            TextureMapping faces = new TextureMapping().put(TextureSlot.FRONT, blockTexture(name + "_front"))
                    .put(TextureSlot.SIDE, blockTexture(name + "_side")).put(TextureSlot.TOP, blockTexture(name + "_top"));
            var model = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(boiler.get(), faces, blockModels.modelOutput));
            PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> facing = PropertyDispatch.initial(dev.strataindustria.steam.BoilerBlock.FACING);
            facing.select(net.minecraft.core.Direction.NORTH, model);
            facing.select(net.minecraft.core.Direction.EAST, model.with(BlockModelGenerators.Y_ROT_90));
            facing.select(net.minecraft.core.Direction.SOUTH, model.with(BlockModelGenerators.Y_ROT_180));
            facing.select(net.minecraft.core.Direction.WEST, model.with(BlockModelGenerators.Y_ROT_270));
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(boiler.get()).with(facing));
            itemModels.itemModelOutput.accept(boiler.get().asItem(), ItemModelUtils.plainModel(StrataIndustria.id("block/" + name)));
        }

        steelBoiler(blockModels, itemModels);

        for (var pipe : java.util.List.of(Tier4Blocks.COPPER_FLUID_PIPE, Tier4Blocks.BRONZE_FLUID_PIPE, Tier4Blocks.STEEL_FLUID_PIPE)) {
            String name = pipe.getId().getPath();
            MultiPartGenerator parts = MultiPartGenerator.multiPart(pipe.get())
                    .with(BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + name + "_core")));
            arms(parts, StrataIndustria.id("block/" + name + "_arm"));
            blockModels.blockStateOutput.accept(parts);
            itemModels.itemModelOutput.accept(pipe.get().asItem(), ItemModelUtils.plainModel(StrataIndustria.id("block/" + name)));
        }
        // Spec 8.2: heat pipes, with glowing variants of the hand-built core and arm while they carry heat.
        for (var pipe : java.util.List.of(Tier4Blocks.COPPER_HEAT_PIPE, Tier4Blocks.REFRACTORY_HEAT_DUCT,
                Tier4Blocks.INSULATED_COPPER_HEAT_PIPE, Tier4Blocks.INSULATED_REFRACTORY_HEAT_DUCT)) {
            String name = pipe.getId().getPath();
            boolean insulated = name.startsWith("insulated_");
            MultiPartGenerator parts = MultiPartGenerator.multiPart(pipe.get());
            for (boolean glowing : new boolean[] {false, true}) {
                // The wrap keeps the glow in: insulated pipes look the same hot or cold.
                String suffix = glowing && !insulated ? "_hot" : "";
                parts.with(BlockModelGenerators.condition().term(dev.strataindustria.heat.HeatPipeBlock.HOT, glowing),
                        BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + name + "_core" + suffix)));
                var arm = BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + name + "_arm" + suffix));
                var props = dev.strataindustria.heat.HeatPipeBlock.PROPERTIES;
                java.util.Map<net.minecraft.core.Direction, MultiVariant> turned = java.util.Map.of(
                        net.minecraft.core.Direction.NORTH, arm,
                        net.minecraft.core.Direction.EAST, arm.with(BlockModelGenerators.Y_ROT_90),
                        net.minecraft.core.Direction.SOUTH, arm.with(BlockModelGenerators.Y_ROT_180),
                        net.minecraft.core.Direction.WEST, arm.with(BlockModelGenerators.Y_ROT_270),
                        net.minecraft.core.Direction.UP, arm.with(BlockModelGenerators.X_ROT_270),
                        net.minecraft.core.Direction.DOWN, arm.with(BlockModelGenerators.X_ROT_90));
                for (var side : net.minecraft.core.Direction.values()) {
                    parts.with(BlockModelGenerators.condition().term(props.get(side), true).term(dev.strataindustria.heat.HeatPipeBlock.HOT, glowing),
                            turned.get(side));
                }
            }
            blockModels.blockStateOutput.accept(parts);
            itemModels.itemModelOutput.accept(pipe.get().asItem(), ItemModelUtils.plainModel(StrataIndustria.id("block/" + name)));
        }
        blockModels.createTrivialCube(Tier4Blocks.HEAT_INLET.get());

        // Spec 8.6: the kiln faces the player; its door glows while it fires.
        Block kiln = Tier4Blocks.KILN.get();
        TextureMapping kilnCold = new TextureMapping().put(TextureSlot.FRONT, blockTexture("kiln_front"))
                .put(TextureSlot.SIDE, blockTexture("kiln_side")).put(TextureSlot.TOP, blockTexture("kiln_top"));
        var kilnIdle = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(kiln, kilnCold, blockModels.modelOutput));
        var kilnLit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(kiln, "_active",
                kilnCold.copyAndUpdate(TextureSlot.FRONT, blockTexture("kiln_front_active")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> kilnState = PropertyDispatch.initial(
                dev.strataindustria.ceramics.KilnBlock.FACING, dev.strataindustria.ceramics.KilnBlock.LIT);
        for (boolean on : new boolean[] {false, true}) {
            var base = on ? kilnLit : kilnIdle;
            kilnState.select(net.minecraft.core.Direction.NORTH, on, base);
            kilnState.select(net.minecraft.core.Direction.EAST, on, base.with(BlockModelGenerators.Y_ROT_90));
            kilnState.select(net.minecraft.core.Direction.SOUTH, on, base.with(BlockModelGenerators.Y_ROT_180));
            kilnState.select(net.minecraft.core.Direction.WEST, on, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(kiln).with(kilnState));
        itemModels.itemModelOutput.accept(Tier4Items.KILN.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/kiln")));
        // Spec 8.5: the roaster's hearth faces the player, its gas flange on the back; the ore bed glows while it roasts.
        Block roaster = Tier4Blocks.ROASTER.get();
        TextureMapping roasterCold = new TextureMapping().put(TextureSlot.NORTH, blockTexture("roaster_front"))
                .put(TextureSlot.SOUTH, blockTexture("roaster_back")).put(TextureSlot.EAST, blockTexture("roaster_side"))
                .put(TextureSlot.WEST, blockTexture("roaster_side")).put(TextureSlot.UP, blockTexture("roaster_top"))
                .put(TextureSlot.DOWN, blockTexture("fire_bricks")).put(TextureSlot.PARTICLE, blockTexture("roaster_side"));
        var roasterIdle = BlockModelGenerators.plainVariant(ModelTemplates.CUBE.create(roaster, roasterCold, blockModels.modelOutput));
        var roasterLit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE.createWithSuffix(roaster, "_active",
                roasterCold.copyAndUpdate(TextureSlot.NORTH, blockTexture("roaster_front_active")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> roasterState = PropertyDispatch.initial(
                dev.strataindustria.roasting.RoasterBlock.FACING, dev.strataindustria.roasting.RoasterBlock.LIT);
        for (boolean on : new boolean[] {false, true}) {
            var base = on ? roasterLit : roasterIdle;
            roasterState.select(net.minecraft.core.Direction.NORTH, on, base);
            roasterState.select(net.minecraft.core.Direction.EAST, on, base.with(BlockModelGenerators.Y_ROT_90));
            roasterState.select(net.minecraft.core.Direction.SOUTH, on, base.with(BlockModelGenerators.Y_ROT_180));
            roasterState.select(net.minecraft.core.Direction.WEST, on, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(roaster).with(roasterState));
        itemModels.itemModelOutput.accept(Tier4Items.ROASTER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/roaster")));
        // Spec 8.7: the smelter's spout faces the player; its front shows the molten surface while it works.
        Block smelter = Tier4Blocks.SMELTER.get();
        TextureMapping smelterCold = new TextureMapping().put(TextureSlot.FRONT, blockTexture("smelter_front"))
                .put(TextureSlot.SIDE, blockTexture("smelter_side")).put(TextureSlot.TOP, blockTexture("smelter_top"));
        var smelterIdle = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(smelter, smelterCold, blockModels.modelOutput));
        var smelterLit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(smelter, "_active",
                smelterCold.copyAndUpdate(TextureSlot.FRONT, blockTexture("smelter_front_active"))
                        .copyAndUpdate(TextureSlot.TOP, blockTexture("smelter_top_active")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> smelterState = PropertyDispatch.initial(
                dev.strataindustria.metal.SmelterBlock.FACING, dev.strataindustria.metal.SmelterBlock.LIT);
        for (boolean on : new boolean[] {false, true}) {
            var base = on ? smelterLit : smelterIdle;
            smelterState.select(net.minecraft.core.Direction.NORTH, on, base);
            smelterState.select(net.minecraft.core.Direction.EAST, on, base.with(BlockModelGenerators.Y_ROT_90));
            smelterState.select(net.minecraft.core.Direction.SOUTH, on, base.with(BlockModelGenerators.Y_ROT_180));
            smelterState.select(net.minecraft.core.Direction.WEST, on, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(smelter).with(smelterState));
        itemModels.itemModelOutput.accept(Tier4Items.SMELTER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/smelter")));
        MultiPartGenerator gauge = MultiPartGenerator.multiPart(Tier4Blocks.PRESSURE_GAUGE.get());
        for (int reading = 0; reading <= 4; reading++) {
            gauge.with(BlockModelGenerators.condition().term(dev.strataindustria.fluid.PressureGaugeBlock.READING, reading),
                    BlockModelGenerators.plainVariant(StrataIndustria.id("block/pressure_gauge_" + reading)));
        }
        arms(gauge, StrataIndustria.id("block/bronze_fluid_pipe_arm"));
        blockModels.blockStateOutput.accept(gauge);
        itemModels.itemModelOutput.accept(Tier4Items.PRESSURE_GAUGE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/pressure_gauge")));

        // Spec 9.3: the valve's wheel turns a quarter and shows a red tab while shut.
        MultiPartGenerator valve = MultiPartGenerator.multiPart(Tier4Blocks.VALVE.get());
        var valveOpen = BlockModelGenerators.plainVariant(StrataIndustria.id("block/valve_open"));
        var valveShut = BlockModelGenerators.plainVariant(StrataIndustria.id("block/valve_shut"));
        valve.with(BlockModelGenerators.condition().term(dev.strataindustria.fluid.ValveBlock.OPEN, true)
                .term(dev.strataindustria.fluid.ValveBlock.POWERED, false), valveOpen);
        valve.with(BlockModelGenerators.condition().term(dev.strataindustria.fluid.ValveBlock.OPEN, false), valveShut);
        valve.with(BlockModelGenerators.condition().term(dev.strataindustria.fluid.ValveBlock.OPEN, true)
                .term(dev.strataindustria.fluid.ValveBlock.POWERED, true), valveShut);
        arms(valve, StrataIndustria.id("block/bronze_fluid_pipe_arm"));
        blockModels.blockStateOutput.accept(valve);
        itemModels.itemModelOutput.accept(Tier4Items.VALVE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/valve_open")));
        // Spec 21.4: a column of tanks draws as one, with end caps only at its ends.
        PropertyDispatch.C2<MultiVariant, Boolean, Boolean> tank = PropertyDispatch.initial(dev.strataindustria.fluid.FluidTankBlock.UP,
                dev.strataindustria.fluid.FluidTankBlock.DOWN);
        tank.select(false, false, BlockModelGenerators.plainVariant(StrataIndustria.id("block/fluid_tank")));
        tank.select(true, false, BlockModelGenerators.plainVariant(StrataIndustria.id("block/fluid_tank_bottom")));
        tank.select(true, true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/fluid_tank_middle")));
        tank.select(false, true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/fluid_tank_top")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier4Blocks.FLUID_TANK.get()).with(tank));
        itemModels.itemModelOutput.accept(Tier4Items.FLUID_TANK.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/fluid_tank")));
        // Spec 9.3 and 10.5: hand-built models facing north; the engine's flywheel is drawn by its renderer.
        for (var machine : java.util.List.of(Tier4Blocks.MECHANICAL_PUMP, Tier4Blocks.STEAM_ENGINE)) {
            var model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + machine.getId().getPath()));
            PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> facing = PropertyDispatch.initial(HorizontalDirectionalBlock.FACING);
            facing.select(net.minecraft.core.Direction.NORTH, model);
            facing.select(net.minecraft.core.Direction.EAST, model.with(BlockModelGenerators.Y_ROT_90));
            facing.select(net.minecraft.core.Direction.SOUTH, model.with(BlockModelGenerators.Y_ROT_180));
            facing.select(net.minecraft.core.Direction.WEST, model.with(BlockModelGenerators.Y_ROT_270));
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(machine.get()).with(facing));
        }
        itemModels.itemModelOutput.accept(Tier4Items.MECHANICAL_PUMP.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/mechanical_pump")));
        itemModels.itemModelOutput.accept(Tier4Items.STEAM_ENGINE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/steam_engine_item")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.listening.ListeningBlocks.STEAM_WHISTLE.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/steam_whistle"))));
        itemModels.itemModelOutput.accept(dev.strataindustria.listening.ListeningBlocks.STEAM_WHISTLE_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/steam_whistle")));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Integer> cabinet = PropertyDispatch.initial(
                dev.strataindustria.cabinet.SpecimenCabinetBlock.FACING, dev.strataindustria.cabinet.SpecimenCabinetBlock.FILL);
        for (int fill = 0; fill <= dev.strataindustria.cabinet.SpecimenCabinetBlock.MAX_FILL; fill++) {
            MultiVariant model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/specimen_cabinet_" + fill));
            cabinet.select(net.minecraft.core.Direction.NORTH, fill, model);
            cabinet.select(net.minecraft.core.Direction.EAST, fill, model.with(BlockModelGenerators.Y_ROT_90));
            cabinet.select(net.minecraft.core.Direction.SOUTH, fill, model.with(BlockModelGenerators.Y_ROT_180));
            cabinet.select(net.minecraft.core.Direction.WEST, fill, model.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.cabinet.CabinetRegistry.SPECIMEN_CABINET.get()).with(cabinet));
        itemModels.itemModelOutput.accept(dev.strataindustria.cabinet.CabinetRegistry.SPECIMEN_CABINET_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/specimen_cabinet_0")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.bronze.BronzeRegistry.BELL.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/bell"))));
        itemModels.itemModelOutput.accept(dev.strataindustria.bronze.BronzeRegistry.BELL_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/bell")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.bronze.BronzeRegistry.FUME_HOOD.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/fume_hood"))));
        itemModels.itemModelOutput.accept(dev.strataindustria.bronze.BronzeRegistry.FUME_HOOD_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/fume_hood")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.ledger.LedgerRegistry.BUILDERS_CRATE.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/builders_crate"))));
        itemModels.itemModelOutput.accept(dev.strataindustria.ledger.LedgerRegistry.BUILDERS_CRATE_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/builders_crate")));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> charterFacing = PropertyDispatch.initial(HorizontalDirectionalBlock.FACING);
        var charter = BlockModelGenerators.plainVariant(StrataIndustria.id("block/outpost_charter"));
        charterFacing.select(net.minecraft.core.Direction.NORTH, charter);
        charterFacing.select(net.minecraft.core.Direction.EAST, charter.with(BlockModelGenerators.Y_ROT_90));
        charterFacing.select(net.minecraft.core.Direction.SOUTH, charter.with(BlockModelGenerators.Y_ROT_180));
        charterFacing.select(net.minecraft.core.Direction.WEST, charter.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(dev.strataindustria.registry.TransportBlocks.OUTPOST_CHARTER.get()).with(charterFacing));
        itemModels.itemModelOutput.accept(dev.strataindustria.registry.TransportBlocks.OUTPOST_CHARTER_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/outpost_charter")));
        processing(blockModels, itemModels);
        blastFurnace(blockModels, itemModels);
    }

    // Spec 12.1 and 21.4: the blast furnace's parts are refractory casing with their own front; the
    // controller and tap glow while it runs. The blower is a hand-built housing whose fan the renderer turns.
    private static void blastFurnace(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(Tier4Blocks.REFRACTORY_CASING.get());
        var casing = blockTexture("refractory_casing");
        Block controller = Tier4Blocks.BLAST_FURNACE_CONTROLLER.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.FRONT, blockTexture("blast_furnace_controller_front"))
                .put(TextureSlot.SIDE, casing).put(TextureSlot.TOP, casing);
        var coldModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(controller, cold, blockModels.modelOutput));
        var litModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(controller, "_lit",
                cold.copyAndUpdate(TextureSlot.FRONT, blockTexture("blast_furnace_controller_front_lit")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> controllerState = PropertyDispatch.initial(
                dev.strataindustria.ironworks.BlastFurnaceBlock.FACING, dev.strataindustria.ironworks.BlastFurnaceBlock.LIT);
        for (boolean on : new boolean[] {false, true}) facing(controllerState, on, on ? litModel : coldModel);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(controller).with(controllerState));

        Block converter = Tier4Blocks.CONVERTER_CONTROLLER.get();
        TextureMapping still = new TextureMapping().put(TextureSlot.FRONT, blockTexture("converter_controller_front"))
                .put(TextureSlot.SIDE, casing).put(TextureSlot.TOP, casing);
        var stillModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(converter, still, blockModels.modelOutput));
        var blowingModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(converter, "_blowing",
                still.copyAndUpdate(TextureSlot.FRONT, blockTexture("converter_controller_front_blowing")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> converterState = PropertyDispatch.initial(
                dev.strataindustria.ironworks.ConverterBlock.FACING, dev.strataindustria.ironworks.ConverterBlock.LIT);
        for (boolean on : new boolean[] {false, true}) facing(converterState, on, on ? blowingModel : stillModel);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(converter).with(converterState));

        Block tap = Tier4Blocks.TAP_HATCH.get();
        TextureMapping plugged = new TextureMapping().put(TextureSlot.FRONT, blockTexture("tap_hatch_front"))
                .put(TextureSlot.SIDE, casing).put(TextureSlot.TOP, casing);
        var pluggedModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(tap, plugged, blockModels.modelOutput));
        var hotModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(tap, "_hot",
                plugged.copyAndUpdate(TextureSlot.FRONT, blockTexture("tap_hatch_front_hot")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> tapState = PropertyDispatch.initial(
                dev.strataindustria.ironworks.TapHatchBlock.FACING, dev.strataindustria.ironworks.TapHatchBlock.HOT);
        for (boolean on : new boolean[] {false, true}) facing(tapState, on, on ? hotModel : pluggedModel);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(tap).with(tapState));

        Block tuyere = Tier4Blocks.TUYERE.get();
        var tuyereModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(tuyere, new TextureMapping()
                .put(TextureSlot.FRONT, blockTexture("tuyere_front")).put(TextureSlot.SIDE, blockTexture("tuyere_side"))
                .put(TextureSlot.TOP, casing), blockModels.modelOutput));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> tuyereState =
                PropertyDispatch.initial(dev.strataindustria.ironworks.FurnacePartBlock.FACING);
        tuyereState.select(net.minecraft.core.Direction.NORTH, tuyereModel);
        tuyereState.select(net.minecraft.core.Direction.EAST, tuyereModel.with(BlockModelGenerators.Y_ROT_90));
        tuyereState.select(net.minecraft.core.Direction.SOUTH, tuyereModel.with(BlockModelGenerators.Y_ROT_180));
        tuyereState.select(net.minecraft.core.Direction.WEST, tuyereModel.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(tuyere).with(tuyereState));

        Block hatch = Tier4Blocks.CHARGING_HATCH.get();
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(hatch, BlockModelGenerators.plainVariant(
                ModelTemplates.CUBE_BOTTOM_TOP.create(hatch, new TextureMapping().put(TextureSlot.TOP, blockTexture("charging_hatch_top"))
                        .put(TextureSlot.SIDE, blockTexture("charging_hatch_side")).put(TextureSlot.BOTTOM, casing), blockModels.modelOutput))));

        var blower = BlockModelGenerators.plainVariant(StrataIndustria.id("block/blower"));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> blowerState =
                PropertyDispatch.initial(dev.strataindustria.ironworks.BlowerBlock.FACING);
        blowerState.select(net.minecraft.core.Direction.NORTH, blower);
        blowerState.select(net.minecraft.core.Direction.EAST, blower.with(BlockModelGenerators.Y_ROT_90));
        blowerState.select(net.minecraft.core.Direction.SOUTH, blower.with(BlockModelGenerators.Y_ROT_180));
        blowerState.select(net.minecraft.core.Direction.WEST, blower.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier4Blocks.BLOWER.get()).with(blowerState));
        itemModels.itemModelOutput.accept(Tier4Items.BLOWER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/blower_item")));

        // Spec 10.5: the blowing engine's grille faces the tuyere; its cylinder works behind it while it blows.
        Block blowingEngine = Tier4Blocks.BLOWING_ENGINE.get();
        TextureMapping blowingIdle = new TextureMapping().put(TextureSlot.NORTH, blockTexture("blowing_engine_front"))
                .put(TextureSlot.SOUTH, blockTexture("blowing_engine_back")).put(TextureSlot.EAST, blockTexture("blowing_engine_side"))
                .put(TextureSlot.WEST, blockTexture("blowing_engine_side")).put(TextureSlot.UP, blockTexture("blowing_engine_top"))
                .put(TextureSlot.DOWN, blockTexture("blowing_engine_top")).put(TextureSlot.PARTICLE, blockTexture("blowing_engine_side"));
        var blowingOff = BlockModelGenerators.plainVariant(ModelTemplates.CUBE.create(blowingEngine, blowingIdle, blockModels.modelOutput));
        var blowingOn = BlockModelGenerators.plainVariant(ModelTemplates.CUBE.createWithSuffix(blowingEngine, "_active",
                blowingIdle.copyAndUpdate(TextureSlot.NORTH, blockTexture("blowing_engine_front_active"))
                        .copyAndUpdate(TextureSlot.EAST, blockTexture("blowing_engine_side_active"))
                        .copyAndUpdate(TextureSlot.WEST, blockTexture("blowing_engine_side_active")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> blowingState = PropertyDispatch.initial(
                dev.strataindustria.ironworks.BlowingEngineBlock.FACING, dev.strataindustria.ironworks.BlowingEngineBlock.ACTIVE);
        for (boolean on : new boolean[] {false, true}) {
            var base = on ? blowingOn : blowingOff;
            blowingState.select(net.minecraft.core.Direction.NORTH, on, base);
            blowingState.select(net.minecraft.core.Direction.EAST, on, base.with(BlockModelGenerators.Y_ROT_90));
            blowingState.select(net.minecraft.core.Direction.SOUTH, on, base.with(BlockModelGenerators.Y_ROT_180));
            blowingState.select(net.minecraft.core.Direction.WEST, on, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(blowingEngine).with(blowingState));
        itemModels.itemModelOutput.accept(Tier4Items.BLOWING_ENGINE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/blowing_engine")));

        flatItem(itemModels, Tier4Items.SLAG.get());
        flatItem(itemModels, Tier4Items.SLAG_DUST.get());
        // Spec 13.3 and 13.5: the chute's tube is a hand-built model; the filter is a flat item.
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier4Blocks.CHUTE.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/chute"))));
        itemModels.itemModelOutput.accept(Tier4Items.CHUTE.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/chute")));
        // Spec 13.4: the inserter's base is hand-built, with a paper tag when a filter is fitted; the arm is drawn by its renderer.
        MultiPartGenerator inserter = MultiPartGenerator.multiPart(Tier4Blocks.INSERTER.get())
                .with(BlockModelGenerators.plainVariant(StrataIndustria.id("block/inserter")));
        var tag = BlockModelGenerators.plainVariant(StrataIndustria.id("block/inserter_tag"));
        inserter.with(BlockModelGenerators.condition().term(dev.strataindustria.automation.InserterBlock.FILTERED, true)
                .term(dev.strataindustria.automation.InserterBlock.FACING, net.minecraft.core.Direction.NORTH), tag);
        inserter.with(BlockModelGenerators.condition().term(dev.strataindustria.automation.InserterBlock.FILTERED, true)
                .term(dev.strataindustria.automation.InserterBlock.FACING, net.minecraft.core.Direction.EAST), tag.with(BlockModelGenerators.Y_ROT_90));
        inserter.with(BlockModelGenerators.condition().term(dev.strataindustria.automation.InserterBlock.FILTERED, true)
                .term(dev.strataindustria.automation.InserterBlock.FACING, net.minecraft.core.Direction.SOUTH), tag.with(BlockModelGenerators.Y_ROT_180));
        inserter.with(BlockModelGenerators.condition().term(dev.strataindustria.automation.InserterBlock.FILTERED, true)
                .term(dev.strataindustria.automation.InserterBlock.FACING, net.minecraft.core.Direction.WEST), tag.with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(inserter);
        itemModels.itemModelOutput.accept(Tier4Items.INSERTER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/inserter_item")));
        // Spec 13.1: the belt's frame is hand-built for each slope; the leather top is drawn by its renderer so it can run at network speed.
        PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, dev.strataindustria.automation.ConveyorBlock.Slope> beltState = PropertyDispatch.initial(
                dev.strataindustria.automation.ConveyorBlock.FACING, dev.strataindustria.automation.ConveyorBlock.SLOPE);
        for (var slope : dev.strataindustria.automation.ConveyorBlock.Slope.values()) {
            var base = BlockModelGenerators.plainVariant(StrataIndustria.id("block/conveyor_" + slope.getSerializedName()));
            beltState.select(net.minecraft.core.Direction.NORTH, slope, base);
            beltState.select(net.minecraft.core.Direction.EAST, slope, base.with(BlockModelGenerators.Y_ROT_90));
            beltState.select(net.minecraft.core.Direction.SOUTH, slope, base.with(BlockModelGenerators.Y_ROT_180));
            beltState.select(net.minecraft.core.Direction.WEST, slope, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier4Blocks.CONVEYOR_BELT.get()).with(beltState));
        itemModels.itemModelOutput.accept(Tier4Items.CONVEYOR_BELT.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/conveyor_item")));
        // Spec 13.2: the diverter is a flat belt frame with its push-side rail lowered; the paddle and the leather are drawn by the belt renderer.
        MultiPartGenerator diverter = MultiPartGenerator.multiPart(Tier4Blocks.BELT_DIVERTER.get());
        for (boolean left : new boolean[] {false, true}) {
            for (boolean tagged : new boolean[] {false, true}) {
                var model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/belt_diverter_" + (left ? "left" : "right") + (tagged ? "_tagged" : "")));
                var turns = new MultiVariant[] {model, model.with(BlockModelGenerators.Y_ROT_90), model.with(BlockModelGenerators.Y_ROT_180), model.with(BlockModelGenerators.Y_ROT_270)};
                var facings = new net.minecraft.core.Direction[] {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST,
                        net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST};
                for (int i = 0; i < 4; i++) {
                    diverter.with(BlockModelGenerators.condition().term(dev.strataindustria.automation.ConveyorBlock.FACING, facings[i])
                            .term(dev.strataindustria.automation.BeltDiverterBlock.LEFT, left)
                            .term(dev.strataindustria.automation.BeltDiverterBlock.FILTERED, tagged), turns[i]);
                }
            }
        }
        blockModels.blockStateOutput.accept(diverter);
        itemModels.itemModelOutput.accept(Tier4Items.BELT_DIVERTER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/belt_diverter_item")));
        flatItem(itemModels, Tier4Items.FILTER.get());
        flatItem(itemModels, Tier4Items.SLAG_WOOL.get());
    }

    private static <A extends Comparable<A>> void facing(PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, A> dispatch,
            A a, MultiVariant base) {
        dispatch.select(net.minecraft.core.Direction.NORTH, a, base);
        dispatch.select(net.minecraft.core.Direction.EAST, a, base.with(BlockModelGenerators.Y_ROT_90));
        dispatch.select(net.minecraft.core.Direction.SOUTH, a, base.with(BlockModelGenerators.Y_ROT_180));
        dispatch.select(net.minecraft.core.Direction.WEST, a, base.with(BlockModelGenerators.Y_ROT_270));
    }

    // Spec 11.2 to 11.4: ore processing machines face the player; the front shows the works, moving while active.
    private static void processing(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (var machine : java.util.List.of(Tier4Blocks.CRUSHER, Tier4Blocks.WASHER)) {
            Block block = machine.get();
            String name = machine.getId().getPath();
            TextureMapping idle = new TextureMapping().put(TextureSlot.FRONT, blockTexture(name + "_front"))
                    .put(TextureSlot.SIDE, blockTexture(name + "_side")).put(TextureSlot.TOP, blockTexture(name + "_top"));
            var still = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(block, idle, blockModels.modelOutput));
            var active = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(block, "_active",
                    idle.copyAndUpdate(TextureSlot.FRONT, blockTexture(name + "_front_active")), blockModels.modelOutput));
            PropertyDispatch.C2<MultiVariant, net.minecraft.core.Direction, Boolean> state = PropertyDispatch.initial(
                    dev.strataindustria.processing.ProcessingBlock.FACING, dev.strataindustria.processing.ProcessingBlock.ACTIVE);
            for (boolean on : new boolean[] {false, true}) {
                var model = on ? active : still;
                state.select(net.minecraft.core.Direction.NORTH, on, model);
                state.select(net.minecraft.core.Direction.EAST, on, model.with(BlockModelGenerators.Y_ROT_90));
                state.select(net.minecraft.core.Direction.SOUTH, on, model.with(BlockModelGenerators.Y_ROT_180));
                state.select(net.minecraft.core.Direction.WEST, on, model.with(BlockModelGenerators.Y_ROT_270));
            }
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(state));
            itemModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(StrataIndustria.id("block/" + name)));
        }
    }

    /** A pipe arm, modelled pointing north, on each face the pipe joins. */
    private static void arms(MultiPartGenerator parts, net.minecraft.resources.Identifier arm) {
        var north = BlockModelGenerators.plainVariant(arm);
        var props = dev.strataindustria.fluid.FluidPipeBlock.PROPERTIES;
        parts.with(BlockModelGenerators.condition(props.get(net.minecraft.core.Direction.NORTH), true), north);
        parts.with(BlockModelGenerators.condition(props.get(net.minecraft.core.Direction.EAST), true), north.with(BlockModelGenerators.Y_ROT_90));
        parts.with(BlockModelGenerators.condition(props.get(net.minecraft.core.Direction.SOUTH), true), north.with(BlockModelGenerators.Y_ROT_180));
        parts.with(BlockModelGenerators.condition(props.get(net.minecraft.core.Direction.WEST), true), north.with(BlockModelGenerators.Y_ROT_270));
        parts.with(BlockModelGenerators.condition(props.get(net.minecraft.core.Direction.UP), true), north.with(BlockModelGenerators.X_ROT_270));
        parts.with(BlockModelGenerators.condition(props.get(net.minecraft.core.Direction.DOWN), true), north.with(BlockModelGenerators.X_ROT_90));
    }

    private static <A extends Comparable<A>, B extends Comparable<B>> void horizontal(
            PropertyDispatch.C3<MultiVariant, net.minecraft.core.Direction, A, B> dispatch, A a, B b, MultiVariant base) {
        dispatch.select(net.minecraft.core.Direction.NORTH, a, b, base);
        dispatch.select(net.minecraft.core.Direction.EAST, a, b, base.with(BlockModelGenerators.Y_ROT_90));
        dispatch.select(net.minecraft.core.Direction.SOUTH, a, b, base.with(BlockModelGenerators.Y_ROT_180));
        dispatch.select(net.minecraft.core.Direction.WEST, a, b, base.with(BlockModelGenerators.Y_ROT_270));
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
    }

    /** Tier 6: crude oil in the world, the oil buckets, bitumen, plastics and synthetic rubber. */
    private static void tier6(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createAirLikeBlock(dev.strataindustria.registry.Tier6Blocks.CRUDE_OIL.get(), blockTexture("fluid/crude_oil_still"));
        for (var item : dev.strataindustria.registry.Tier6Items.flatItems()) flatItem(itemModels, item.get());
    }

    private static void flatItem(ItemModelGenerators itemModels, Item item) {
        itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
    }

    /** A model turned to face a horizontal direction, from facing north. */
    private static MultiVariant turned(MultiVariant model, net.minecraft.core.Direction facing) {
        return switch (facing) {
            case EAST -> model.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> model.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> model.with(BlockModelGenerators.Y_ROT_270);
            default -> model;
        };
    }

    /** Spec 10.3: the steel boiler's shell, its two-mode fluid port and the controller with its light and sight glass. */
    private static void steelBoiler(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(Tier4Blocks.STEEL_BOILER_SHELL.get());

        Block port = Tier4Blocks.BOILER_FLUID_PORT.get();
        var water = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.createWithSuffix(port, "_water",
                TextureMapping.singleSlot(TextureSlot.ALL, blockTexture("boiler_fluid_port_water")), blockModels.modelOutput));
        var steam = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.createWithSuffix(port, "_steam",
                TextureMapping.singleSlot(TextureSlot.ALL, blockTexture("boiler_fluid_port_steam")), blockModels.modelOutput));
        PropertyDispatch.C1<MultiVariant, dev.strataindustria.steam.BoilerFluidPortBlock.Mode> portState =
                PropertyDispatch.initial(dev.strataindustria.steam.BoilerFluidPortBlock.MODE);
        portState.select(dev.strataindustria.steam.BoilerFluidPortBlock.Mode.WATER, water);
        portState.select(dev.strataindustria.steam.BoilerFluidPortBlock.Mode.STEAM, steam);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(port).with(portState));
        itemModels.itemModelOutput.accept(Tier4Items.BOILER_FLUID_PORT.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/boiler_fluid_port_water")));

        Block controller = Tier4Blocks.BOILER_CONTROLLER.get();
        TextureMapping faces = new TextureMapping().put(TextureSlot.FRONT, blockTexture("boiler_controller_front"))
                .put(TextureSlot.SIDE, blockTexture("steel_boiler_shell")).put(TextureSlot.TOP, blockTexture("boiler_controller_top"));
        var still = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(controller, faces, blockModels.modelOutput));
        var venting = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(controller, "_venting",
                faces.copyAndUpdate(TextureSlot.FRONT, blockTexture("boiler_controller_front_venting")), blockModels.modelOutput));
        MultiPartGenerator parts = MultiPartGenerator.multiPart(controller);
        for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (boolean vent : new boolean[] {false, true}) {
                parts.with(BlockModelGenerators.condition().term(dev.strataindustria.steam.BoilerBlock.FACING, side)
                        .term(dev.strataindustria.steam.SteelBoilerControllerBlock.VENTING, vent), turned(vent ? venting : still, side));
            }
            for (var light : dev.strataindustria.steam.SteelBoilerControllerBlock.Light.values()) {
                if (light == dev.strataindustria.steam.SteelBoilerControllerBlock.Light.OFF) continue;
                parts.with(BlockModelGenerators.condition().term(dev.strataindustria.steam.BoilerBlock.FACING, side)
                                .term(dev.strataindustria.steam.SteelBoilerControllerBlock.LIGHT, light),
                        turned(BlockModelGenerators.plainVariant(StrataIndustria.id("block/boiler_controller_lamp_" + light.getSerializedName())), side));
            }
            for (int level = 1; level <= 5; level++) {
                parts.with(BlockModelGenerators.condition().term(dev.strataindustria.steam.BoilerBlock.FACING, side)
                                .term(dev.strataindustria.steam.SteelBoilerControllerBlock.GLASS, level),
                        turned(BlockModelGenerators.plainVariant(StrataIndustria.id("block/boiler_controller_glass_" + level)), side));
            }
        }
        blockModels.blockStateOutput.accept(parts);
        itemModels.itemModelOutput.accept(Tier4Items.BOILER_CONTROLLER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/boiler_controller")));

        Block cracked = Tier4Blocks.CRACKED_BOILER_CONTROLLER.get();
        var broken = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(cracked,
                faces.copyAndUpdate(TextureSlot.FRONT, blockTexture("cracked_boiler_controller_front")), blockModels.modelOutput));
        PropertyDispatch.C1<MultiVariant, net.minecraft.core.Direction> crackedState = PropertyDispatch.initial(dev.strataindustria.steam.BoilerBlock.FACING);
        for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) crackedState.select(side, turned(broken, side));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(cracked).with(crackedState));
        itemModels.itemModelOutput.accept(Tier4Items.CRACKED_BOILER_CONTROLLER.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/cracked_boiler_controller")));
    }
}
