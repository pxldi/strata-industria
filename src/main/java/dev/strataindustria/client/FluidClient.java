package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModFluids;
import dev.strataindustria.registry.Tier4Fluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;

/**
 * Fluid sprites for lye, tannin, creosote, sulfur dioxide and steam. Without a registered model they draw
 * as the missing texture in recipe viewers and anywhere else a fluid is rendered.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class FluidClient {
    private FluidClient() {}

    @SubscribeEvent
    static void registerFluidModels(RegisterFluidModelsEvent event) {
        liquid(event, "lye", ModFluids.LYE.get(), ModFluids.FLOWING_LYE.get());
        liquid(event, "tannin", ModFluids.TANNIN.get(), ModFluids.FLOWING_TANNIN.get());
        liquid(event, "creosote", Tier4Fluids.CREOSOTE.get(), Tier4Fluids.FLOWING_CREOSOTE.get());
        // Gases have one still frame; it doubles as the flowing sprite.
        gas(event, "sulfur_dioxide", Tier4Fluids.SULFUR_DIOXIDE.get(), Tier4Fluids.FLOWING_SULFUR_DIOXIDE.get());
        gas(event, "steam", Tier4Fluids.STEAM.get(), Tier4Fluids.FLOWING_STEAM.get());
    }

    private static void liquid(RegisterFluidModelsEvent event, String name, Fluid source, Fluid flowing) {
        event.register(new FluidModel.Unbaked(new Material(StrataIndustria.id("block/fluid/" + name + "_still")),
                new Material(StrataIndustria.id("block/fluid/" + name + "_flow")), null, null), source, flowing);
    }

    private static void gas(RegisterFluidModelsEvent event, String name, Fluid source, Fluid flowing) {
        Material sprite = new Material(StrataIndustria.id("block/fluid/" + name));
        event.register(new FluidModel.Unbaked(sprite, sprite, null, null), source, flowing);
    }
}
