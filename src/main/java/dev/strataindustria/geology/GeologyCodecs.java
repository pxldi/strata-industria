package dev.strataindustria.geology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import net.minecraft.resources.Identifier;

/** Codecs for code-defined geology enums, written as namespaced ids ({@code strataindustria:shale}). */
public final class GeologyCodecs {
    private GeologyCodecs() {}

    public static final Codec<Rock> ROCK = Identifier.CODEC.comapFlatMap(id -> {
        if (id.getNamespace().equals(StrataIndustria.MOD_ID)) {
            for (Rock rock : Rock.values()) {
                if (rock.id().equals(id.getPath())) return DataResult.success(rock);
            }
        }
        return DataResult.error(() -> "Unknown rock: " + id);
    }, rock -> StrataIndustria.id(rock.id()));

    public static final Codec<OreMineral> MINERAL = Identifier.CODEC.comapFlatMap(id -> {
        if (id.getNamespace().equals(StrataIndustria.MOD_ID)) {
            for (OreMineral mineral : OreMineral.values()) {
                if (mineral.id().equals(id.getPath())) return DataResult.success(mineral);
            }
        }
        return DataResult.error(() -> "Unknown ore mineral: " + id);
    }, mineral -> StrataIndustria.id(mineral.id()));

    /** An inclusive integer range written as {@code {"min": a, "max": b}}. */
    public record IntRange(int min, int max) {
        public static final Codec<IntRange> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("min").forGetter(IntRange::min),
                Codec.INT.fieldOf("max").forGetter(IntRange::max)).apply(i, IntRange::new));

        public int sample(double unit) {
            return min + (int) Math.floor(unit * (max - min + 1));
        }
    }
}
