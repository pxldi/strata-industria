package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.TransportSounds;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.RouteIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The tub stop screen's two packets and the locomotive whistle key: the server opens the screen with the stop's settings, the client sends new ones back. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class RailPayloads {
    private static final double REACH = 10;

    private RailPayloads() {}

    public record OpenStop(StopData data, String station) implements CustomPacketPayload {
        public static final Type<OpenStop> TYPE = new Type<>(StrataIndustria.id("tub_stop_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenStop> CODEC = StreamCodec.composite(
                StopData.CODEC, OpenStop::data, ByteBufCodecs.stringUtf8(RouteIndex.MAX_NAME * 4), OpenStop::station, OpenStop::new);

        @Override
        public Type<OpenStop> type() {
            return TYPE;
        }
    }

    public record UpdateStop(StopData data) implements CustomPacketPayload {
        public static final Type<UpdateStop> TYPE = new Type<>(StrataIndustria.id("tub_stop_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateStop> CODEC = StopData.CODEC.map(UpdateStop::new, UpdateStop::data);

        @Override
        public Type<UpdateStop> type() {
            return TYPE;
        }
    }

    /** The whistle key pressed on a client, by a rider of a locomotive. */
    public record Whistle() implements CustomPacketPayload {
        public static final Type<Whistle> TYPE = new Type<>(StrataIndustria.id("locomotive_whistle"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Whistle> CODEC = StreamCodec.unit(new Whistle());

        @Override
        public Type<Whistle> type() {
            return TYPE;
        }
    }

    /** Opens the screen; the station line names the charter whose area the stop is in, if any. */
    public static void openStop(ServerPlayer player, ServerLevel level, TubStopBlockEntity stop) {
        Charter charter = TramwayRoutes.stationCharter(RouteIndex.get(level), stop.getBlockPos());
        PacketDistributor.sendToPlayer(player, new OpenStop(stop.data(), charter == null ? "" : charter.name()));
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenStop.TYPE, OpenStop.CODEC, (payload, context) ->
                context.enqueueWork(() -> dev.strataindustria.client.RailClient.openStop(payload.data(), payload.station())));
        registrar.playToServer(Whistle.TYPE, Whistle.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.getVehicle() instanceof SteamLocomotiveEntity loco) loco.whistleBy(player.level(), player);
            else if (player.getVehicle() instanceof ElectricTramEntity tram) tram.bellBy(player.level(), player);
        }));
        registrar.playToServer(UpdateStop.TYPE, UpdateStop.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) update(player, payload.data());
        }));
    }

    /** Applies new settings to the stop at {@code data.pos()} for a player who stands near it. */
    public static boolean update(ServerPlayer player, StopData data) {
        ServerLevel level = player.level();
        BlockPos pos = data.pos();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH * REACH) return false;
        if (!(level.getBlockEntity(pos) instanceof TubStopBlockEntity stop)) return false;
        stop.apply(data);
        level.playSound(null, pos, TransportSounds.CHARTER_DEED.get(), SoundSource.BLOCKS, 0.5f, 1.2f);
        return true;
    }
}
