package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Tier 3 changes to vanilla (spec 2): iron golems give nuggets, not ingots. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class IronAgeEvents {
    private IronAgeEvents() {}

    /** Spec 2: iron golems drop 2 to 4 nuggets, so iron farms do not skip the bloomery. */
    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getPath().equals("iron_golem") || !Config.GOLEM_DROPS_NUGGETS.getAsBoolean()) return;
        boolean hadIron = event.getDrops().removeIf(drop -> drop.getItem().is(Items.IRON_INGOT));
        if (!hadIron) return;
        var golem = event.getEntity();
        int nuggets = 2 + golem.getRandom().nextInt(3);
        event.getDrops().add(new ItemEntity(golem.level(), golem.getX(), golem.getY(), golem.getZ(), new ItemStack(Items.IRON_NUGGET, nuggets)));
    }
}
