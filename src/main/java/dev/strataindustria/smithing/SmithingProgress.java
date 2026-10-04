package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/**
 * An unfinished workpiece (spec 9.2): the chosen recipe, where it stands on the bar, the last three
 * hits (newest first, as {@link HitType} ordinals) and how many hits it has taken so far.
 */
public record SmithingProgress(ResourceKey<Recipe<?>> recipe, int position, List<Integer> recent, int hits) {
    public static final Codec<SmithingProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.RECIPE).fieldOf("recipe").forGetter(SmithingProgress::recipe),
            Codec.intRange(0, Smithing.MAX_POSITION).fieldOf("position").forGetter(SmithingProgress::position),
            Codec.INT.listOf(0, 3).fieldOf("recent").forGetter(SmithingProgress::recent),
            Codec.INT.fieldOf("hits").forGetter(SmithingProgress::hits)
    ).apply(i, SmithingProgress::new));

    public static final StreamCodec<ByteBuf, SmithingProgress> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), SmithingProgress::recipe,
            ByteBufCodecs.VAR_INT, SmithingProgress::position,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(3)), SmithingProgress::recent,
            ByteBufCodecs.VAR_INT, SmithingProgress::hits,
            SmithingProgress::new);

    public SmithingProgress {
        recent = List.copyOf(recent);
    }

    public static SmithingProgress start(ResourceKey<Recipe<?>> recipe) {
        return new SmithingProgress(recipe, 0, List.of(), 0);
    }

    public SmithingProgress withRecipe(ResourceKey<Recipe<?>> other) {
        return new SmithingProgress(other, position, recent, hits);
    }

    public SmithingProgress hit(HitType type) {
        List<Integer> next = new ArrayList<>(3);
        next.add(type.ordinal());
        for (int i = 0; i < Math.min(2, recent.size()); i++) next.add(recent.get(i));
        return new SmithingProgress(recipe, position + type.delta(), next, hits + 1);
    }

    /** The {@code index}-th most recent hit (0 = last), or null. */
    public HitType recent(int index) {
        return index < recent.size() ? HitType.byId(recent.get(index)) : null;
    }
}
