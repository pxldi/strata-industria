package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.hud.FirestarterHud;
import dev.strataindustria.client.screen.FirePitScreen;
import dev.strataindustria.client.screen.KnappingScreen;
import dev.strataindustria.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.KNAPPING.get(), KnappingScreen::new);
        event.register(ModMenus.FIRE_PIT.get(), FirePitScreen::new);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, StrataIndustria.id("firestarter"), FirestarterHud::render);
    }
}
