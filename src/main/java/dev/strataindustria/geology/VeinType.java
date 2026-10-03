package dev.strataindustria.geology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * An ore vein type (worldgen spec 5.3). Datapack registry {@code strataindustria:vein}. Only the
 * {@code cluster} shape exists in M1; the shape is a typed object so that layers, pipes and dikes can
 * be added without changing the format.
 */
public record VeinType(ClusterShape shape, List<MineralWeight> minerals, List<Rock> hosts, List<Rock> preferredHosts,
        int minY, int maxY, int weight, boolean indicators) {
    public static final ResourceKey<Registry<VeinType>> REGISTRY = ResourceKey.createRegistryKey(StrataIndustria.id("vein"));

    public static final Codec<VeinType> CODEC = RecordCodecBuilder.create(i -> i.group(
            ClusterShape.CODEC.fieldOf("shape").forGetter(VeinType::shape),
            MineralWeight.CODEC.listOf(1, 16).fieldOf("minerals").forGetter(VeinType::minerals),
            GeologyCodecs.ROCK.listOf().fieldOf("hosts").forGetter(VeinType::hosts),
            GeologyCodecs.ROCK.listOf().optionalFieldOf("preferred_hosts", List.of()).forGetter(VeinType::preferredHosts),
            Codec.INT.fieldOf("min_y").forGetter(VeinType::minY),
            Codec.INT.fieldOf("max_y").forGetter(VeinType::maxY),
            Codec.intRange(1, 1000).fieldOf("weight").forGetter(VeinType::weight),
            Codec.BOOL.optionalFieldOf("indicators", true).forGetter(VeinType::indicators)
    ).apply(i, VeinType::new));

    /** Picks a mineral by weight from a unit random value. */
    public OreMineral pickMineral(double unit) {
        int total = 0;
        for (MineralWeight m : minerals) total += m.weight();
        double roll = unit * total;
        for (MineralWeight m : minerals) {
            roll -= m.weight();
            if (roll < 0) return m.mineral();
        }
        return minerals.getLast().mineral();
    }

    /** True if every mineral of this vein can be mined with stone tools. */
    public boolean isStoneTier() {
        return minerals.stream().noneMatch(m -> m.mineral().needsCopperTool());
    }

    public record MineralWeight(OreMineral mineral, int weight) {
        public static final Codec<MineralWeight> CODEC = RecordCodecBuilder.create(i -> i.group(
                GeologyCodecs.MINERAL.fieldOf("mineral").forGetter(MineralWeight::mineral),
                Codec.intRange(1, 1000).fieldOf("weight").forGetter(MineralWeight::weight)
        ).apply(i, MineralWeight::new));
    }

    /** Noisy ellipsoid blob (worldgen spec 5.2). */
    public record ClusterShape(String type, GeologyCodecs.IntRange radiusHorizontal, GeologyCodecs.IntRange radiusVertical,
            float density) {
        public static final String TYPE = StrataIndustria.MOD_ID + ":cluster";
        public static final Codec<ClusterShape> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.validate(s -> s.equals(TYPE)
                        ? com.mojang.serialization.DataResult.success(s)
                        : com.mojang.serialization.DataResult.error(() -> "Unknown vein shape: " + s))
                        .fieldOf("type").forGetter(ClusterShape::type),
                GeologyCodecs.IntRange.CODEC.fieldOf("radius_horizontal").forGetter(ClusterShape::radiusHorizontal),
                GeologyCodecs.IntRange.CODEC.fieldOf("radius_vertical").forGetter(ClusterShape::radiusVertical),
                Codec.floatRange(0, 1).fieldOf("density").forGetter(ClusterShape::density)
        ).apply(i, ClusterShape::new));

        public static ClusterShape of(int hMin, int hMax, int vMin, int vMax, float density) {
            return new ClusterShape(TYPE, new GeologyCodecs.IntRange(hMin, hMax), new GeologyCodecs.IntRange(vMin, vMax), density);
        }
    }
}
