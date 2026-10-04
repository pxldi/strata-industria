package dev.strataindustria.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

/**
 * Tier 5 spec 7.3: a fluid that burns, as the fluid data map {@code strataindustria:liquid_fuel}, so packs can
 * add fuels. The combustion generator turns it into electricity; the liquid-fuel burner (7.5) will read the
 * same entry.
 *
 * @param joulesPerMb J a combustion generator makes from 1 mB
 * @param bucket      the bucket item that carries it, so the generator takes it by hand
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public record LiquidFuel(double joulesPerMb, Optional<Identifier> bucket) {
    public static final Codec<LiquidFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0.01, 100_000).fieldOf("joules_per_mb").forGetter(LiquidFuel::joulesPerMb),
            Identifier.CODEC.optionalFieldOf("bucket").forGetter(LiquidFuel::bucket)
    ).apply(i, LiquidFuel::new));

    public static final DataMapType<Fluid, LiquidFuel> DATA_MAP = DataMapType.builder(StrataIndustria.id("liquid_fuel"), Registries.FLUID, CODEC).build();

    /** The fuel entry of {@code fluid}, or null when it does not burn. */
    public static @Nullable LiquidFuel of(Fluid fluid) {
        return fluid.builtInRegistryHolder().getData(DATA_MAP);
    }

    /** The fuel whose bucket {@code stack} is, or null. */
    public static @Nullable Fluid fluidOfBucket(ItemStack stack) {
        for (var entry : BuiltInRegistries.FLUID.getDataMap(DATA_MAP).entrySet()) {
            if (entry.getValue().bucket().isEmpty()) continue;
            Item bucket = BuiltInRegistries.ITEM.getValue(entry.getValue().bucket().get());
            if (stack.is(bucket)) return BuiltInRegistries.FLUID.getValue(entry.getKey());
        }
        return null;
    }

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }
}
