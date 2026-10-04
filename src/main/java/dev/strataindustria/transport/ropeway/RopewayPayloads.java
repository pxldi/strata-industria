package dev.strataindustria.transport.ropeway;

import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * What a ropeway tells the players near its line (outposts spec 8.3): the line's shape once in a while, and its
 * buckets and rope speed whenever they change. Buckets are not entities, and the middle of a long line sits in
 * chunks the terminal knows nothing about, so the server sends the picture to whoever can see a piece of it.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class RopewayPayloads {
    private RopewayPayloads() {}

    /** A bucket as a client needs it: where it hangs on the rope, what is in it and whether it carries a rider's seat. */
    public record BucketView(float offset, ItemStack stack, boolean seat) {
        public static final StreamCodec<RegistryFriendlyByteBuf, BucketView> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, BucketView::offset, ItemStack.OPTIONAL_STREAM_CODEC, BucketView::stack, ByteBufCodecs.BOOL, BucketView::seat, BucketView::new);
    }

    /** The picture of one line. An empty node list means the line is gone. */
    public record LineState(BlockPos terminal, List<BlockPos> nodes, double advance, float speed, List<BucketView> buckets)
            implements CustomPacketPayload {
        public static final Type<LineState> TYPE = new Type<>(StrataIndustria.id("ropeway_line"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LineState> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, LineState::terminal,
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(32)), LineState::nodes,
                ByteBufCodecs.DOUBLE, LineState::advance,
                ByteBufCodecs.FLOAT, LineState::speed,
                BucketView.CODEC.apply(ByteBufCodecs.list(512)), LineState::buckets,
                LineState::new);

        @Override
        public Type<LineState> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(LineState.TYPE, LineState.CODEC, (payload, context) ->
                context.enqueueWork(() -> dev.strataindustria.client.ropeway.ClientRopeways.accept(payload)));
    }
}
