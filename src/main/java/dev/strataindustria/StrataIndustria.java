package dev.strataindustria;

import com.mojang.logging.LogUtils;
import dev.strataindustria.gametest.ModGameTests;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModConditions;
import dev.strataindustria.registry.ModCreativeTabs;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModFluids;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModMenus;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModWorldgen;
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
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModWorldgen.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModFluids.TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModConditions.CONDITIONS.register(modEventBus);
        Journal.TRIGGERS.register(modEventBus);
        ModGameTests.INSTANCE_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
