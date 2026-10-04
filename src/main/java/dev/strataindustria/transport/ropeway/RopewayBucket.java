package dev.strataindustria.transport.ropeway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/**
 * One bucket on a ropeway's loop. Its place is a fixed {@code offset} from the rope's advance, so every bucket moves
 * with the rope and only loads and unloads change what a client has to be told.
 */
public final class RopewayBucket {
    public static final Codec<RopewayBucket> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("offset").forGetter(b -> b.offset),
            ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(b -> b.stack),
            Codec.BOOL.optionalFieldOf("delivered", false).forGetter(b -> b.delivered)
    ).apply(i, RopewayBucket::new));

    /** Where the bucket hangs on the loop when the rope's advance is zero. */
    public double offset;
    public ItemStack stack;
    /** Tipped out at the return and on its way home: arriving back at the terminal completes a trip. */
    public boolean delivered;

    public RopewayBucket(double offset, ItemStack stack, boolean delivered) {
        this.offset = offset;
        this.stack = stack;
        this.delivered = delivered;
    }

    /** Position on the loop for a rope advance, in [0, loop). */
    public double at(double advance, double loop) {
        return (((offset + advance) % loop) + loop) % loop;
    }
}
