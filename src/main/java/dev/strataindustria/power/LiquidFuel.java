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
 * add fuels. The combustion generator turns it into electricity; the liquid-fuel burner (7.5) turns it into heat.
 *
 * @param joulesPerMb J a combustion generator makes from 1 mB; 0 for a fuel only burners take (heavy oil, crude oil)
 * @param bucket      the bucket item that carries it, so the generator takes it by hand
 * @param burn        what the liquid-fuel burner makes of it; a fuel without it only feeds generators
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public record LiquidFuel(double joulesPerMb, Optional<Identifier> bucket, Optional<Burn> burn) {
    /**
     * Tier 5 spec 7.5: heat from burning the fluid in a liquid-fuel burner.
     *
     * @param huPerMb        HU one mB gives
     * @param maxTemperature °C the burner can reach on it
     * @param huPerTick      HU/t at full burn
     */
    public record Burn(double huPerMb, int maxTemperature, int huPerTick) {
        public static final Codec<Burn> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.01, 100_000).fieldOf("hu_per_mb").forGetter(Burn::huPerMb),
                Codec.intRange(25, 3000).fieldOf("max_temperature").forGetter(Burn::maxTemperature),
                Codec.intRange(1, 10_000).fieldOf("hu_per_tick").forGetter(Burn::huPerTick)
        ).apply(i, Burn::new));
    }

    public static final Codec<LiquidFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 100_000).optionalFieldOf("joules_per_mb", 0.0).forGetter(LiquidFuel::joulesPerMb),
            Identifier.CODEC.optionalFieldOf("bucket").forGetter(LiquidFuel::bucket),
            Burn.CODEC.optionalFieldOf("burn").forGetter(LiquidFuel::burn)
    ).apply(i, LiquidFuel::new));

    public static final DataMapType<Fluid, LiquidFuel> DATA_MAP = DataMapType.builder(StrataIndustria.id("liquid_fuel"), Registries.FLUID, CODEC).build();

    /** The fuel entry of {@code fluid}, or null when it does not burn. */
    public static @Nullable LiquidFuel of(Fluid fluid) {
        return fluid.builtInRegistryHolder().getData(DATA_MAP);
    }

    /** Whether a combustion generator can burn {@code fluid}. */
    public static boolean generates(Fluid fluid) {
        LiquidFuel fuel = of(fluid);
        return fuel != null && fuel.joulesPerMb() > 0;
    }

    /** The burner's entry for {@code fluid}, or null when the burner cannot burn it. */
    public static @Nullable Burn burnOf(Fluid fluid) {
        LiquidFuel fuel = of(fluid);
        return fuel == null ? null : fuel.burn().orElse(null);
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
