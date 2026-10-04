package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/**
 * An unfinished workpiece: the chosen shape, how many blows it has taken and how many of them landed
 * while the metal was bright. It rides on the piece, so a trip back to the forge keeps it.
 */
public record SmithingProgress(ResourceKey<Recipe<?>> recipe, int blows, int bright) {
    public static final Codec<SmithingProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.RECIPE).fieldOf("recipe").forGetter(SmithingProgress::recipe),
            Codec.INT.optionalFieldOf("blows", 0).forGetter(SmithingProgress::blows),
            Codec.INT.optionalFieldOf("bright", 0).forGetter(SmithingProgress::bright)
    ).apply(i, SmithingProgress::new));

    public static final StreamCodec<ByteBuf, SmithingProgress> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), SmithingProgress::recipe,
            ByteBufCodecs.VAR_INT, SmithingProgress::blows,
            ByteBufCodecs.VAR_INT, SmithingProgress::bright,
            SmithingProgress::new);

    public static SmithingProgress start(ResourceKey<Recipe<?>> recipe) {
        return new SmithingProgress(recipe, 0, 0);
    }

    /** The same work on another shape: progress starts over. */
    public SmithingProgress withRecipe(ResourceKey<Recipe<?>> other) {
        return other.equals(recipe) ? this : start(other);
    }

    public SmithingProgress strike(int weight, boolean wasBright) {
        return new SmithingProgress(recipe, blows + weight, bright + (wasBright ? weight : 0));
    }
}
