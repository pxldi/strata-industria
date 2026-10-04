package dev.strataindustria.compat;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModRecipes;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Sends the mod's own recipe types to clients. Since 1.21.2 the server keeps recipes to itself, so a
 * recipe viewer on the client sees only the types someone asks to sync.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class RecipeSync {
    private RecipeSync() {}

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.TYPES.getEntries().stream().map(h -> (RecipeType<?>) h.get()).toArray(RecipeType<?>[]::new));
    }
}
