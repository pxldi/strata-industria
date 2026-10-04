package dev.strataindustria.geology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * An ore vein type (worldgen spec 5.3). Datapack registry {@code strataindustria:vein}. Shapes are
 * {@code cluster} (noisy ellipsoid) and {@code layer} (flat lens that follows the strata). A vein
 * places ore of its {@code minerals}, or a plain {@code block} for sediments such as lignite and fire
 * clay. Sediment-pass veins roll in their own attempts so they never crowd out metal ores, and
 * {@code max_depth} keeps them within that many blocks of the surface. An optional {@code biomes} set
 * limits the vein to those biomes (laterite bauxite in hot climates), judged where it enters a chunk.
 */
public record VeinType(ClusterShape shape, List<MineralWeight> minerals, Optional<BlockState> block, List<Rock> hosts,
        List<Rock> preferredHosts, int minY, int maxY, int weight, boolean indicators, Pass pass, int maxDepth,
        Optional<HolderSet<Biome>> biomes) {
    public static final ResourceKey<Registry<VeinType>> REGISTRY = ResourceKey.createRegistryKey(StrataIndustria.id("vein"));

    public static final Codec<VeinType> CODEC = RecordCodecBuilder.create(i -> i.group(
            ClusterShape.CODEC.fieldOf("shape").forGetter(VeinType::shape),
            MineralWeight.CODEC.listOf(0, 16).optionalFieldOf("minerals", List.of()).forGetter(VeinType::minerals),
            BlockState.CODEC.optionalFieldOf("block").forGetter(VeinType::block),
            GeologyCodecs.ROCK.listOf().fieldOf("hosts").forGetter(VeinType::hosts),
            GeologyCodecs.ROCK.listOf().optionalFieldOf("preferred_hosts", List.of()).forGetter(VeinType::preferredHosts),
            Codec.INT.fieldOf("min_y").forGetter(VeinType::minY),
            Codec.INT.fieldOf("max_y").forGetter(VeinType::maxY),
            Codec.intRange(1, 1000).fieldOf("weight").forGetter(VeinType::weight),
            Codec.BOOL.optionalFieldOf("indicators", true).forGetter(VeinType::indicators),
            Pass.CODEC.optionalFieldOf("pass", Pass.METAL).forGetter(VeinType::pass),
            Codec.intRange(0, 512).optionalFieldOf("max_depth", 0).forGetter(VeinType::maxDepth),
            RegistryCodecs.holderSet(Registries.BIOME).optionalFieldOf("biomes").forGetter(VeinType::biomes)
    ).apply(i, VeinType::new));

    /** A metal ore vein in the main pass. */
    public VeinType(ClusterShape shape, List<MineralWeight> minerals, List<Rock> hosts, List<Rock> preferredHosts,
            int minY, int maxY, int weight, boolean indicators) {
        this(shape, minerals, Optional.empty(), hosts, preferredHosts, minY, maxY, weight, indicators, Pass.METAL, 0, Optional.empty());
    }

    public enum Pass implements StringRepresentable {
        METAL("metal"), SEDIMENT("sediment");

        public static final Codec<Pass> CODEC = StringRepresentable.fromEnum(Pass::values);
        private final String name;

        Pass(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** True if the vein places ore blocks (as opposed to a plain sediment block). */
    public boolean placesOre() {
        return block.isEmpty() && !minerals.isEmpty();
    }

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
        return placesOre() && minerals.stream().noneMatch(m -> m.mineral().needsCopperTool());
    }

    public record MineralWeight(OreMineral mineral, int weight) {
        public static final Codec<MineralWeight> CODEC = RecordCodecBuilder.create(i -> i.group(
                GeologyCodecs.MINERAL.fieldOf("mineral").forGetter(MineralWeight::mineral),
                Codec.intRange(1, 1000).fieldOf("weight").forGetter(MineralWeight::weight)
        ).apply(i, MineralWeight::new));
    }

    /**
     * Noisy ellipsoid blob (worldgen spec 5.2), or with type {@code layer} a flat lens: then
     * {@code radius_vertical} is the half thickness and the lens tilts gently with the noise.
     */
    public record ClusterShape(String type, GeologyCodecs.IntRange radiusHorizontal, GeologyCodecs.IntRange radiusVertical,
            float density) {
        public static final String TYPE = StrataIndustria.MOD_ID + ":cluster";
        public static final String LAYER = StrataIndustria.MOD_ID + ":layer";
        public static final Codec<ClusterShape> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.validate(s -> s.equals(TYPE) || s.equals(LAYER)
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

        /** A layer lens with a horizontal radius range and a thickness range (full thickness in blocks). */
        public static ClusterShape layer(int hMin, int hMax, int tMin, int tMax, float density) {
            return new ClusterShape(LAYER, new GeologyCodecs.IntRange(hMin, hMax),
                    new GeologyCodecs.IntRange(Math.max(1, tMin / 2), Math.max(1, (tMax + 1) / 2)), density);
        }

        public boolean isLayer() {
            return type.equals(LAYER);
        }
    }
}
