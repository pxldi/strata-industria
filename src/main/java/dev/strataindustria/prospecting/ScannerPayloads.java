package dev.strataindustria.prospecting;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.OreScannerClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The one packet of the ore scanner: tells the client a scan or a seismic survey finished so it opens the map. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class ScannerPayloads {
    private ScannerPayloads() {}

    /** @param seismic open on the seismic tab */
    public record Open(boolean seismic) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(StrataIndustria.id("scanner_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Open::seismic, Open::new);

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Open.TYPE, Open.CODEC, (payload, context) -> context.enqueueWork(() -> OreScannerClient.openHeld(payload.seismic())));
    }
}
