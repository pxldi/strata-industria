package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/**
 * A shape card for a hammer machine: which anvil shape it works. Made by sneaking with a blank pattern on an
 * anvil that has a shape picked. Stands in until the machines get their own shape button.
 *
 * @param result the finished item's id, for the tooltip
 */
public record SmithingPattern(ResourceKey<Recipe<?>> recipe, Identifier result) {
    public static final Codec<SmithingPattern> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.RECIPE).fieldOf("recipe").forGetter(SmithingPattern::recipe),
            Identifier.CODEC.fieldOf("result").forGetter(SmithingPattern::result)
    ).apply(i, SmithingPattern::new));

    public static final StreamCodec<ByteBuf, SmithingPattern> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE), SmithingPattern::recipe,
            Identifier.STREAM_CODEC, SmithingPattern::result,
            SmithingPattern::new);

    /** "Pickaxe head". */
    public Component tooltip() {
        Component name = BuiltInRegistries.ITEM.getOptional(result).map(item -> item.getDefaultInstance().getHoverName()).orElse(Component.literal(result.toString()));
        return Component.translatable(StrataIndustria.MOD_ID + ".pattern.recorded", name);
    }
}
