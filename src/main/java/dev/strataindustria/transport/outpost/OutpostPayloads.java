package dev.strataindustria.transport.outpost;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The charter board's two packets: the server opens it with a snapshot, the client sends a new name back. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class OutpostPayloads {
    private static final double REACH = 10;

    private OutpostPayloads() {}

    public record OpenBoard(BoardView view) implements CustomPacketPayload {
        public static final Type<OpenBoard> TYPE = new Type<>(StrataIndustria.id("charter_board"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoard> CODEC = BoardView.CODEC.map(OpenBoard::new, OpenBoard::view);

        @Override
        public Type<OpenBoard> type() {
            return TYPE;
        }
    }

    public record Rename(BlockPos pos, String name) implements CustomPacketPayload {
        public static final Type<Rename> TYPE = new Type<>(StrataIndustria.id("charter_rename"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rename> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Rename::pos, ByteBufCodecs.stringUtf8(RouteIndex.MAX_NAME * 4), Rename::name, Rename::new);

        @Override
        public Type<Rename> type() {
            return TYPE;
        }
    }

    public static void openBoard(ServerPlayer player, Charter charter) {
        ServerLevel level = player.level();
        RouteIndex index = RouteIndex.get(level);
        PacketDistributor.sendToPlayer(player, new OpenBoard(BoardView.of(level, index, charter, OutpostPlan.isOwner(level, player, charter))));
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenBoard.TYPE, OpenBoard.CODEC, (payload, context) ->
                context.enqueueWork(() -> dev.strataindustria.client.OutpostClient.openBoard(payload.view())));
        registrar.playToServer(Rename.TYPE, Rename.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            rename(player, payload.pos(), payload.name());
        }));
    }

    /** Renames the charter at {@code pos} for a player who owns it and stands near it. */
    public static boolean rename(ServerPlayer player, BlockPos pos, String name) {
        ServerLevel level = player.level();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH * REACH) return false;
        RouteIndex index = RouteIndex.get(level);
        Charter charter = index.charterAt(pos).orElse(null);
        if (charter == null || !OutpostPlan.isOwner(level, player, charter)) return false;
        index.rename(level, charter.id(), name);
        level.playSound(null, pos, TransportSounds.CHARTER_DEED.get(), SoundSource.BLOCKS, 0.7f, 1f);
        return true;
    }
}
