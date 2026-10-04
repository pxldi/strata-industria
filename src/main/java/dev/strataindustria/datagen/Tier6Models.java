package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.oil.OilStillBlock;
import dev.strataindustria.oil.PumpJackBlock;
import dev.strataindustria.oil.SeismicChargeBlock;
import dev.strataindustria.oil.WellheadBlock;
import dev.strataindustria.registry.Tier6Blocks;
import dev.strataindustria.registry.Tier6Items;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/** Block states and models for tier 6 (industrial). {@link ModModelProvider} calls it. */
final class Tier6Models {
    private Tier6Models() {}

    static void register(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        oilStill(blockModels, itemModels);
        oilField(blockModels, itemModels);
    }

    // Spec 24.3: the gauge glass faces the player, the pot is copper, the base is fire bricks; the glass bubbles while it works.
    private static void oilStill(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block block = Tier6Blocks.OIL_STILL.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.NORTH, texture("oil_still_front"))
                .put(TextureSlot.SOUTH, texture("oil_still_side")).put(TextureSlot.EAST, texture("oil_still_side"))
                .put(TextureSlot.WEST, texture("oil_still_side")).put(TextureSlot.UP, texture("oil_still_top"))
                .put(TextureSlot.DOWN, texture("fire_bricks")).put(TextureSlot.PARTICLE, texture("oil_still_side"));
        MultiVariant idle = BlockModelGenerators.plainVariant(ModelTemplates.CUBE.create(block, cold, blockModels.modelOutput));
        MultiVariant lit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE.createWithSuffix(block, "_active",
                cold.copyAndUpdate(TextureSlot.NORTH, texture("oil_still_front_active")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, Direction, Boolean> dispatch = PropertyDispatch.initial(OilStillBlock.FACING, OilStillBlock.LIT);
        for (boolean on : new boolean[] {false, true}) {
            MultiVariant base = on ? lit : idle;
            dispatch.select(Direction.NORTH, on, base);
            dispatch.select(Direction.EAST, on, base.with(BlockModelGenerators.Y_ROT_90));
            dispatch.select(Direction.SOUTH, on, base.with(BlockModelGenerators.Y_ROT_180));
            dispatch.select(Direction.WEST, on, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        itemModels.itemModelOutput.accept(Tier6Items.OIL_STILL.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/oil_still")));
    }

    // Spec 24.3: hand-written models (the wellhead and charge are small, the jack's moving parts are drawn by its renderer).
    private static void oilField(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block charge = Tier6Blocks.SEISMIC_CHARGE.get();
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(charge).with(PropertyDispatch.initial(SeismicChargeBlock.LIT)
                .select(false, BlockModelGenerators.plainVariant(StrataIndustria.id("block/seismic_charge")))
                .select(true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/seismic_charge_lit")))));
        itemModels.generateFlatItem(Tier6Items.SEISMIC_CHARGE.get(), ModelTemplates.FLAT_ITEM);

        Block wellhead = Tier6Blocks.WELLHEAD.get();
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(wellhead).with(PropertyDispatch.initial(WellheadBlock.DRILLED)
                .select(false, BlockModelGenerators.plainVariant(StrataIndustria.id("block/wellhead")))
                .select(true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/wellhead_drilled")))));
        itemModels.itemModelOutput.accept(Tier6Items.WELLHEAD.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/wellhead")));

        Block jack = Tier6Blocks.PUMP_JACK.get();
        PropertyDispatch.C2<MultiVariant, Direction, Boolean> dispatch = PropertyDispatch.initial(PumpJackBlock.FACING, PumpJackBlock.RUNNING);
        MultiVariant base = BlockModelGenerators.plainVariant(StrataIndustria.id("block/pump_jack"));
        for (boolean running : new boolean[] {false, true}) {
            dispatch.select(Direction.NORTH, running, base);
            dispatch.select(Direction.EAST, running, base.with(BlockModelGenerators.Y_ROT_90));
            dispatch.select(Direction.SOUTH, running, base.with(BlockModelGenerators.Y_ROT_180));
            dispatch.select(Direction.WEST, running, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(jack).with(dispatch));
        itemModels.itemModelOutput.accept(Tier6Items.PUMP_JACK.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/pump_jack_item")));
    }

    private static Material texture(String path) {
        return new Material(StrataIndustria.id("block/" + path));
    }
}
