package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    /** Holding hot metal bare-handed (spec 5.4). */
    public static final ResourceKey<DamageType> HOT_ITEM =
            ResourceKey.create(Registries.DAMAGE_TYPE, StrataIndustria.id("hot_item"));

    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(HOT_ITEM, new DamageType(StrataIndustria.MOD_ID + ".hot_item", 0.1f, DamageEffects.BURNING));
    }

    private ModDamageTypes() {}
}
