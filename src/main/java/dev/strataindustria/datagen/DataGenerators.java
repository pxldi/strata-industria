package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Province;
import dev.strataindustria.geology.VeinType;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Entry point for {@code ./gradlew runData}. Client and server data are generated in a single
 * client data run, written to {@code src/generated/resources}.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    static void gatherData(GatherDataEvent.Client event) {
        // Worldgen: rock provinces, vein types and the features that place them.
        event.createWorldRegistryObjects(
                new RegistrySetBuilder()
                        .add(Province.REGISTRY, GeologyData::provinces)
                        .add(VeinType.REGISTRY, GeologyData::veins)
                        .add(Registries.FEATURE, GeologyData::features)
                        .add(Registries.PLACED_FEATURE, GeologyData::placedFeatures)
                        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GeologyData::biomeModifiers),
                Set.of(StrataIndustria.MOD_ID));

        // Assets
        event.createProvider(ModModelProvider::new);
        event.createProvider(ModLanguageProvider::new);
        event.createProvider(ModSoundsProvider::new);

        // Data
        event.createProvider(ModBlockTagsProvider::new);
        event.createProvider(ModItemTagsProvider::new);
        event.createReloadableRegistryObjects(
                new RegistrySetBuilder()
                        .add(RecipeProvider.asBootstrap(ModRecipeProvider::new))
                        .add(Registries.LOOT_TABLE, new LootTableProvider(
                                Set.of(),
                                List.of(new LootTableProvider.SubProviderEntry(
                                        ModBlockLoot::new,
                                        LootContextParamSets.BLOCK)))),
                // "minecraft" for the conditional overrides of vanilla recipes (spec section 2).
                Set.of(StrataIndustria.MOD_ID, "minecraft"));
    }
}
