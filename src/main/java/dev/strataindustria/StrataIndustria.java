package dev.strataindustria;

import com.mojang.logging.LogUtils;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModCreativeTabs;
import dev.strataindustria.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(StrataIndustria.MOD_ID)
public final class StrataIndustria {
    public static final String MOD_ID = "strataindustria";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StrataIndustria(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
