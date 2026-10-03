package dev.strataindustria.geology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * A rock province: one rock per layer, a spawn weight and a biome bias (worldgen spec 4.1).
 * Datapack registry {@code strataindustria:province}.
 */
public record Province(Rock top, Rock middle, Rock bottom, int weight, List<BiomeBias> biomeBias) {
    public static final ResourceKey<Registry<Province>> REGISTRY = ResourceKey.createRegistryKey(StrataIndustria.id("province"));

    public static final Codec<Province> CODEC = RecordCodecBuilder.create(i -> i.group(
            GeologyCodecs.ROCK.fieldOf("top").forGetter(Province::top),
            GeologyCodecs.ROCK.fieldOf("middle").forGetter(Province::middle),
            GeologyCodecs.ROCK.fieldOf("bottom").forGetter(Province::bottom),
            Codec.intRange(1, 1000).fieldOf("weight").forGetter(Province::weight),
            BiomeBias.CODEC.listOf().optionalFieldOf("biome_bias", List.of()).forGetter(Province::biomeBias)
    ).apply(i, Province::new));

    public Rock rock(Layer layer) {
        return switch (layer) {
            case TOP -> top;
            case MIDDLE -> middle;
            case BOTTOM -> bottom;
        };
    }

    /** Spawn weight multiplied by every matching biome bias. */
    public double weightIn(Holder<Biome> biome) {
        double w = weight;
        for (BiomeBias bias : biomeBias) {
            if (bias.biomes().contains(biome)) w *= bias.multiplier();
        }
        return w;
    }

    public enum Layer { TOP, MIDDLE, BOTTOM }

    public record BiomeBias(HolderSet<Biome> biomes, float multiplier) {
        public static final Codec<BiomeBias> CODEC = RecordCodecBuilder.create(i -> i.group(
                RegistryCodecs.holderSet(Registries.BIOME).fieldOf("biomes").forGetter(BiomeBias::biomes),
                Codec.floatRange(0, 100).fieldOf("multiplier").forGetter(BiomeBias::multiplier)
        ).apply(i, BiomeBias::new));
    }
}
