package dev.strataindustria.transport.signal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.TransportSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.List;

/** The timetable screen and the route switch screen: the server opens them with what is stored, the client sends back the new lines. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class TimetablePayloads {
    private static final double REACH = 10;

    private TimetablePayloads() {}

    public record OpenItem(TimetableStops stops) implements CustomPacketPayload {
        public static final Type<OpenItem> TYPE = new Type<>(StrataIndustria.id("timetable_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenItem> CODEC = TimetableStops.STREAM_CODEC.map(OpenItem::new, OpenItem::stops);

        @Override
        public Type<OpenItem> type() {
            return TYPE;
        }
    }

    public record UpdateItem(TimetableStops stops) implements CustomPacketPayload {
        public static final Type<UpdateItem> TYPE = new Type<>(StrataIndustria.id("timetable_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateItem> CODEC = TimetableStops.STREAM_CODEC.map(UpdateItem::new, UpdateItem::stops);

        @Override
        public Type<UpdateItem> type() {
            return TYPE;
        }
    }

    public record OpenSwitch(BlockPos pos, List<String> stops) implements CustomPacketPayload {
        public static final Type<OpenSwitch> TYPE = new Type<>(StrataIndustria.id("route_switch_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSwitch> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenSwitch::pos,
                ByteBufCodecs.stringUtf8(96).apply(ByteBufCodecs.list(TimetableStops.MAX_STOPS)), OpenSwitch::stops,
                OpenSwitch::new);

        @Override
        public Type<OpenSwitch> type() {
            return TYPE;
        }
    }

    public record UpdateSwitch(BlockPos pos, List<String> stops) implements CustomPacketPayload {
        public static final Type<UpdateSwitch> TYPE = new Type<>(StrataIndustria.id("route_switch_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSwitch> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, UpdateSwitch::pos,
                ByteBufCodecs.stringUtf8(96).apply(ByteBufCodecs.list(TimetableStops.MAX_STOPS)), UpdateSwitch::stops,
                UpdateSwitch::new);

        @Override
        public Type<UpdateSwitch> type() {
            return TYPE;
        }
    }

    public static void openItem(ServerPlayer player, TimetableStops stops) {
        PacketDistributor.sendToPlayer(player, new OpenItem(stops));
    }

    public static void openSwitch(ServerPlayer player, RouteSwitchBlockEntity entity) {
        PacketDistributor.sendToPlayer(player, new OpenSwitch(entity.getBlockPos(), entity.stops()));
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenItem.TYPE, OpenItem.CODEC, (payload, context) ->
                context.enqueueWork(() -> dev.strataindustria.client.SignalClient.openTimetable(payload.stops())));
        registrar.playToClient(OpenSwitch.TYPE, OpenSwitch.CODEC, (payload, context) ->
                context.enqueueWork(() -> dev.strataindustria.client.SignalClient.openSwitch(payload.pos(), payload.stops())));
        registrar.playToServer(UpdateItem.TYPE, UpdateItem.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) updateItem(player, payload.stops());
        }));
        registrar.playToServer(UpdateSwitch.TYPE, UpdateSwitch.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) updateSwitch(player, payload.pos(), payload.stops());
        }));
    }

    /** Writes the lines to the timetable in the player's hand (the main hand first). */
    public static boolean updateItem(ServerPlayer player, TimetableStops stops) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(SignalRegistry.TIMETABLE.get())) continue;
            TimetableStops clean = stops.cleaned();
            if (clean.isEmpty()) stack.remove(SignalRegistry.TIMETABLE_STOPS.get());
            else stack.set(SignalRegistry.TIMETABLE_STOPS.get(), clean);
            player.level().playSound(null, player.blockPosition(), TransportSounds.CHARTER_DEED.get(), SoundSource.PLAYERS, 0.5f, 1.3f);
            return true;
        }
        return false;
    }

    /** Applies a new list to the switch at {@code pos} for a player who stands near it. */
    public static boolean updateSwitch(ServerPlayer player, BlockPos pos, List<String> stops) {
        ServerLevel level = player.level();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH * REACH) return false;
        if (!(level.getBlockEntity(pos) instanceof RouteSwitchBlockEntity entity)) return false;
        entity.setStops(stops);
        level.playSound(null, pos, TransportSounds.CHARTER_DEED.get(), SoundSource.BLOCKS, 0.5f, 1.2f);
        return true;
    }
}
