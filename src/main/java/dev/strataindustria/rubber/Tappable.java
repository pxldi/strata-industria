package dev.strataindustria.rubber;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Spec 5.1: what a tree tap draws from a log, as the block data map {@code strataindustria:tappable}.
 * The default entry is jungle log to latex, 1 mB per tap interval; packs add other mods' rubber trees.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public record Tappable(Fluid fluid, int amount) {
    public static final Codec<Tappable> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(Tappable::fluid),
            Codec.intRange(1, 1000).optionalFieldOf("amount", 1).forGetter(Tappable::amount)
    ).apply(i, Tappable::new));

    /** Spec 16: the block tag mirror of the data map; the living-tree check counts logs in it. */
    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, StrataIndustria.id("tappable"));

    public static final DataMapType<Block, Tappable> DATA_MAP = DataMapType.builder(StrataIndustria.id("tappable"), Registries.BLOCK, CODEC)
            .synced(CODEC, false)
            .build();

    /** What {@code state} gives a tap, or null when it is not tappable. */
    public static Tappable of(BlockState state) {
        return state.getBlock().builtInRegistryHolder().getData(DATA_MAP);
    }

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }
}
