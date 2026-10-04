package dev.strataindustria.logistics;

import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The two packets of the storage controller's screen (tier 5 spec 12.3). */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class StoragePayloads {
    private StoragePayloads() {}

    /** Server to client: everything stored, and whether the controller has power. */
    public record Snapshot(int containerId, List<StorageEntry> entries, boolean powered, int inventories) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(StrataIndustria.id("storage_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Snapshot::containerId,
                StorageEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), Snapshot::entries,
                ByteBufCodecs.BOOL, Snapshot::powered,
                ByteBufCodecs.VAR_INT, Snapshot::inventories,
                Snapshot::new);

        @Override
        public Type<Snapshot> type() {
            return TYPE;
        }
    }

    /** Client to server: a click on the grid. {@code stack} is the kind clicked, empty for none. */
    public record Click(int containerId, ItemStack stack, int button, boolean shift) implements CustomPacketPayload {
        public static final Type<Click> TYPE = new Type<>(StrataIndustria.id("storage_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Click> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Click::containerId,
                ItemStack.OPTIONAL_STREAM_CODEC, Click::stack,
                ByteBufCodecs.VAR_INT, Click::button,
                ByteBufCodecs.BOOL, Click::shift,
                Click::new);

        @Override
        public Type<Click> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (payload, context) -> StorageClient.apply(payload));
        registrar.playToServer(Click.TYPE, Click.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof StorageControllerMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.gridClick(player, payload.stack(), payload.button(), payload.shift());
            }
        }));
    }
}
