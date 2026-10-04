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
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Menus;
import dev.strataindustria.registry.Tier4Recipes;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.registry.Tier6Blocks;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Items;
import dev.strataindustria.registry.Tier6Particles;
import dev.strataindustria.registry.Tier6Sounds;
import dev.strataindustria.registry.Tier6Worldgen;
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
        // Tier 4 registers its entries into the shared registers; load those classes first.
        Tier4Blocks.init();
        Tier4Items.init();
        Tier4Fluids.init();
        Tier4BlockEntities.init();
        Tier4Menus.init();
        Tier4Sounds.init();
        dev.strataindustria.listening.ListeningBlocks.init();
        dev.strataindustria.grid.GridBlocks.init();
        dev.strataindustria.flora.FloraBlocks.init();
        Tier4Recipes.init();
        dev.strataindustria.registry.Tier4DataComponents.init();
        dev.strataindustria.registry.Tier5Blocks.init();
        dev.strataindustria.registry.Tier5Items.init();
        dev.strataindustria.registry.Tier5BlockEntities.init();
        dev.strataindustria.registry.Tier5Menus.init();
        dev.strataindustria.registry.Tier5Sounds.init();
        dev.strataindustria.registry.Tier5DataComponents.init();
        dev.strataindustria.registry.Tier5Fluids.init();
        dev.strataindustria.registry.Tier5Recipes.init();
        dev.strataindustria.logistics.Tier5Logistics.init();
        dev.strataindustria.registry.Tier5Particles.PARTICLES.register(modEventBus);
        // Tier 6 likewise.
        Tier6Fluids.init();
        Tier6Blocks.init();
        Tier6Items.init();
        Tier6Sounds.init();
        dev.strataindustria.felling.FellingSounds.init();
        dev.strataindustria.registry.Tier6BlockEntities.init();
        dev.strataindustria.registry.Tier6Menus.init();
        dev.strataindustria.registry.Tier6Recipes.init();
        dev.strataindustria.registry.PrologueRegistry.init();
        dev.strataindustria.registry.PatternRegistry.init();
        dev.strataindustria.mark.MarkRegistry.init();
        dev.strataindustria.registry.TransportBlocks.init();
        dev.strataindustria.ledger.LedgerRegistry.init();
        dev.strataindustria.transport.foot.FootRegistry.init();
        dev.strataindustria.transport.foot.FootRegistry.register(modEventBus);
        dev.strataindustria.transport.rail.RailRegistry.init();
        dev.strataindustria.transport.rail.RailRegistry.register(modEventBus);
        dev.strataindustria.transport.rail.RailwayRegistry.init();
        dev.strataindustria.transport.rail.RailwayRegistry.register(modEventBus);
        dev.strataindustria.bronze.BronzeRegistry.init();
        dev.strataindustria.cabinet.CabinetRegistry.init();
        dev.strataindustria.cabinet.CabinetRegistry.register(modEventBus);
        dev.strataindustria.ledger.Ledgers.register(modEventBus);
        dev.strataindustria.mark.MakerMarks.register(modEventBus);
        Tier6Worldgen.init();
        Tier6Particles.PARTICLES.register(modEventBus);
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
        dev.strataindustria.structure.StructureContent.register(modEventBus);
        ModConditions.CONDITIONS.register(modEventBus);
        Journal.TRIGGERS.register(modEventBus);
        dev.strataindustria.journal.JournalContent.register(modEventBus);
        dev.strataindustria.knapping.HandShaping.register(modEventBus);
        ModGameTests.INSTANCE_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
