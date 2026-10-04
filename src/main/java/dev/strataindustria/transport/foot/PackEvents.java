package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The worn pack frame: its goal, its weight, and the key that opens it. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class PackEvents {
    /** Filled slots above which the carrier cannot sprint: more than half of nine. */
    public static final int HEAVY_SLOTS = 5;

    private PackEvents() {}

    /** The pack key pressed on a client. */
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(StrataIndustria.id("pack_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Open.TYPE, Open.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) PackFrameItem.openWorn(player);
        }));
    }

    @SubscribeEvent
    static void onEquip(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.CHEST || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getTo().is(FootRegistry.PACK_FRAME.get())) Journal.award(player, Journal.PACK_FRAME_WORN);
        refresh(player);
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) refresh(player);
    }

    /** Sets the burden marker from what the player wears now. */
    public static void refresh(ServerPlayer player) {
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean heavy = worn.is(FootRegistry.PACK_FRAME.get()) && PackContents.filledSlots(worn) >= HEAVY_SLOTS;
        Burden.setBurdened(player, heavy);
    }
}
