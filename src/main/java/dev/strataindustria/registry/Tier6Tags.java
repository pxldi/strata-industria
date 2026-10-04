package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/** Tier 6 tags (spec 17.6). */
public final class Tier6Tags {
    public static final class Items {
        /** Every plastic pellet and sheet. */
        public static final TagKey<Item> PLASTICS = TagKey.create(Registries.ITEM, StrataIndustria.id("plastics"));
        public static final TagKey<Item> C_POLYETHYLENE = common(Registries.ITEM, "plastics/polyethylene");
        public static final TagKey<Item> C_PVC = common(Registries.ITEM, "plastics/pvc");
        public static final TagKey<Item> C_RUBBERS = common(Registries.ITEM, "rubbers");

        private Items() {}
    }

    public static final class Fluids {
        /** Spec 12.2: fluids that only stainless and PVC may carry. */
        public static final TagKey<Fluid> CORROSIVE = TagKey.create(Registries.FLUID, StrataIndustria.id("corrosive"));

        /** Common tag for one tier 6 fluid, for example {@code c:crude_oil}. */
        public static TagKey<Fluid> common(String name) {
            return Tier6Tags.common(Registries.FLUID, name);
        }

        private Fluids() {}
    }

    private static <T> TagKey<T> common(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registry, String path) {
        return TagKey.create(registry, Identifier.fromNamespaceAndPath("c", path));
    }

    private Tier6Tags() {}
}
