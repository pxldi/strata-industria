package dev.strataindustria.logistics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** One kind of item in the storage and how many of it there are. {@code stack} has a count of one. */
public record StorageEntry(ItemStack stack, long count) {
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageEntry> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, StorageEntry::stack,
            ByteBufCodecs.VAR_LONG, StorageEntry::count,
            StorageEntry::new);
}
