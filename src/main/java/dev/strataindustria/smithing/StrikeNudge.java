package dev.strataindustria.smithing;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.StrikeNudgeClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The small push of the camera on a true blow and on the last blow (config smithing.screenNudge). */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class StrikeNudge {
    private StrikeNudge() {}

    public record Nudge(float strength) implements CustomPacketPayload {
        public static final Type<Nudge> TYPE = new Type<>(StrataIndustria.id("strike_nudge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Nudge> CODEC = ByteBufCodecs.FLOAT.map(Nudge::new, Nudge::strength).cast();

        @Override
        public Type<Nudge> type() {
            return TYPE;
        }
    }

    public static void send(ServerPlayer player, float strength) {
        PacketDistributor.sendToPlayer(player, new Nudge(strength));
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Nudge.TYPE, Nudge.CODEC, (payload, context) ->
                context.enqueueWork(() -> StrikeNudgeClient.nudge(payload.strength())));
    }
}
