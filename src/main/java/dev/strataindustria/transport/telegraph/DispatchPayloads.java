package dev.strataindustria.transport.telegraph;

import dev.strataindustria.StrataIndustria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The dispatch board's one packet: the server opens the full screen with a snapshot. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class DispatchPayloads {
    private DispatchPayloads() {}

    public record OpenBoard(DispatchView view) implements CustomPacketPayload {
        public static final Type<OpenBoard> TYPE = new Type<>(StrataIndustria.id("dispatch_board"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoard> CODEC = DispatchView.CODEC.map(OpenBoard::new, OpenBoard::view);

        @Override
        public Type<OpenBoard> type() {
            return TYPE;
        }
    }

    public static void open(ServerPlayer player, ServerLevel level, net.minecraft.core.BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new OpenBoard(DispatchView.of(level, pos)));
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenBoard.TYPE, OpenBoard.CODEC, (payload, context) ->
                context.enqueueWork(() -> dev.strataindustria.client.DispatchClient.open(payload.view())));
    }
}
