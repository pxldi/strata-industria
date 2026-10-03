package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
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

/**
 * Entry point for {@code ./gradlew runData}. Client and server data are generated in a single
 * client data run, written to {@code src/generated/resources}.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    static void gatherData(GatherDataEvent.Client event) {
        // Assets
        event.createProvider(ModModelProvider::new);
        event.createProvider(ModLanguageProvider::new);

        // Data
        event.createProvider(ModBlockTagsProvider::new);
        event.createReloadableRegistryObjects(
                new RegistrySetBuilder()
                        .add(RecipeProvider.asBootstrap(ModRecipeProvider::new))
                        .add(Registries.LOOT_TABLE, new LootTableProvider(
                                Set.of(),
                                List.of(new LootTableProvider.SubProviderEntry(
                                        output -> () -> ModBlockLoot.generate(output::accept),
                                        LootContextParamSets.BLOCK)))),
                Set.of(StrataIndustria.MOD_ID));
    }
}
