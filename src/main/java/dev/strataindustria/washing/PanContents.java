package dev.strataindustria.washing;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** What a loaded washing pan holds (tier 3 spec 11.1): always a single item. */
public record PanContents(Item item) {
    public static final Codec<PanContents> CODEC = BuiltInRegistries.ITEM.byNameCodec().xmap(PanContents::new, PanContents::item);
    public static final StreamCodec<RegistryFriendlyByteBuf, PanContents> STREAM_CODEC =
            ByteBufCodecs.registry(Registries.ITEM).map(PanContents::new, PanContents::item);

    public ItemStack stack() {
        return new ItemStack(item);
    }
}
