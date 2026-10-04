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
public record SmithingProgress(ResourceKey<Recipe<?>> recipe, int position, List<Integer> recent, int hits, List<Integer> history) {
    /** Hits remembered for a smithing pattern (tier 3 spec 9.5); a longer sequence is not worth recording. */
    public static final int MAX_HISTORY = 64;

    public static final Codec<SmithingProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.RECIPE).fieldOf("recipe").forGetter(SmithingProgress::recipe),
            Codec.intRange(0, Smithing.MAX_POSITION).fieldOf("position").forGetter(SmithingProgress::position),
            Codec.INT.listOf(0, 3).fieldOf("recent").forGetter(SmithingProgress::recent),
            Codec.INT.fieldOf("hits").forGetter(SmithingProgress::hits),
            Codec.INT.listOf(0, MAX_HISTORY).optionalFieldOf("history", List.of()).forGetter(SmithingProgress::history)
    ).apply(i, SmithingProgress::new));

    public static final StreamCodec<ByteBuf, SmithingProgress> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), SmithingProgress::recipe,
            ByteBufCodecs.VAR_INT, SmithingProgress::position,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(3)), SmithingProgress::recent,
            ByteBufCodecs.VAR_INT, SmithingProgress::hits,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_HISTORY)), SmithingProgress::history,
            SmithingProgress::new);

    public SmithingProgress {
        recent = List.copyOf(recent);
        history = List.copyOf(history);
    }

    public static SmithingProgress start(ResourceKey<Recipe<?>> recipe) {
        return new SmithingProgress(recipe, 0, List.of(), 0, List.of());
    }

    public SmithingProgress withRecipe(ResourceKey<Recipe<?>> other) {
        return new SmithingProgress(other, position, recent, hits, history);
    }

    public SmithingProgress hit(HitType type) {
        List<Integer> next = new ArrayList<>(3);
        next.add(type.ordinal());
        for (int i = 0; i < Math.min(2, recent.size()); i++) next.add(recent.get(i));
        List<Integer> all = history;
        if (history.size() < MAX_HISTORY) {
            all = new ArrayList<>(history);
            all.add(type.ordinal());
        }
        return new SmithingProgress(recipe, position + type.delta(), next, hits + 1, all);
    }

    /** The {@code index}-th most recent hit (0 = last), or null. */
    public HitType recent(int index) {
        return index < recent.size() ? HitType.byId(recent.get(index)) : null;
    }
}
