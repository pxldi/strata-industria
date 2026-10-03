package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = StrataIndustria.MOD_ID, dist = Dist.CLIENT)
public final class StrataIndustriaClient {
    public StrataIndustriaClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
