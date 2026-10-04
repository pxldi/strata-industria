package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.oil.OilStillBlock;
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

    private static Material texture(String path) {
        return new Material(StrataIndustria.id("block/" + path));
    }
}
