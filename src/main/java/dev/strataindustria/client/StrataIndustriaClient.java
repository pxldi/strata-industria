package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.hud.FirestarterHud;
import dev.strataindustria.client.render.PitKilnRenderer;
import dev.strataindustria.client.render.QuernRenderer;
import dev.strataindustria.client.screen.CrucibleScreen;
import dev.strataindustria.client.screen.FirePitScreen;
import dev.strataindustria.client.screen.ForgeScreen;
import dev.strataindustria.client.screen.KnappingScreen;
import dev.strataindustria.client.screen.SmallVesselScreen;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = StrataIndustria.MOD_ID, dist = Dist.CLIENT)
public final class StrataIndustriaClient {
    public StrataIndustriaClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(StrataIndustriaClient::registerScreens);
        modBus.addListener(StrataIndustriaClient::registerGuiLayers);
        modBus.addListener(StrataIndustriaClient::registerRenderers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.KNAPPING.get(), KnappingScreen::new);
        event.register(ModMenus.FIRE_PIT.get(), FirePitScreen::new);
        event.register(ModMenus.SMALL_VESSEL.get(), SmallVesselScreen::new);
        event.register(ModMenus.FORGE.get(), ForgeScreen::new);
        event.register(ModMenus.CRUCIBLE.get(), CrucibleScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PIT_KILN.get(), PitKilnRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.QUERN.get(), QuernRenderer::new);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, StrataIndustria.id("firestarter"), FirestarterHud::render);
    }
}
