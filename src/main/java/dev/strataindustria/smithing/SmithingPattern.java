package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.metal.Quality;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/**
 * A recorded smithing pattern (tier 3 spec 9.5): which recipe, the target it was made against, every
 * hit in order, and the craft part that sequence earns. A trip hammer replays it.
 *
 * @param result the finished item's id, for the tooltip
 */
public record SmithingPattern(ResourceKey<Recipe<?>> recipe, Identifier result, int target, List<Integer> hits, int craft) {
    public static final Codec<SmithingPattern> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.RECIPE).fieldOf("recipe").forGetter(SmithingPattern::recipe),
            Identifier.CODEC.fieldOf("result").forGetter(SmithingPattern::result),
            Codec.INT.fieldOf("target").forGetter(SmithingPattern::target),
            Codec.INT.listOf(1, SmithingProgress.MAX_HISTORY).fieldOf("hits").forGetter(SmithingPattern::hits),
            Codec.INT.fieldOf("craft").forGetter(SmithingPattern::craft)
    ).apply(i, SmithingPattern::new));

    public static final StreamCodec<ByteBuf, SmithingPattern> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), SmithingPattern::recipe,
            Identifier.STREAM_CODEC, SmithingPattern::result,
            ByteBufCodecs.VAR_INT, SmithingPattern::target,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(SmithingProgress.MAX_HISTORY)), SmithingPattern::hits,
            ByteBufCodecs.VAR_INT, SmithingPattern::craft,
            SmithingPattern::new);

    public SmithingPattern {
        hits = List.copyOf(hits);
    }

    /** "Pickaxe head, 7 hits, Fine". */
    public Component tooltip() {
        Component name = BuiltInRegistries.ITEM.getOptional(result).map(item -> item.getDefaultInstance().getHoverName()).orElse(Component.literal(result.toString()));
        return Component.translatable(StrataIndustria.MOD_ID + ".pattern.recorded", name, hits.size(),
                Component.translatable(StrataIndustria.MOD_ID + ".quality." + new Quality(0, craft).grade()));
    }
}
