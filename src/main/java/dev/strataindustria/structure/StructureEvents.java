package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The "Places" pages of the field journal (structures spec 11): written the first time a player walks into a camp. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class StructureEvents {
    static final int INTERVAL = 40;
    public static final String PLACE = "place/";

    private StructureEvents() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INTERVAL != 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        var structures = level.structureManager();
        for (CampStructure.Layout layout : CampStructure.Layout.values()) {
            if (structures.getStructureWithPieceAt(player.blockPosition(), layout.key()).isValid()) {
                Journal.award(player, PLACE + layout.id());
            }
        }
    }

    /** A soft page turn and a line above the hotbar instead of a toast, so a place reads as a discovery. */
    @SubscribeEvent
    static void onEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var id = event.getAdvancement().id();
        if (!id.getNamespace().equals(StrataIndustria.MOD_ID) || !id.getPath().startsWith("journal/" + PLACE)) return;
        var display = event.getAdvancement().value().display();
        if (display.isEmpty()) return;
        player.connection.send(new ClientboundSoundPacket(StructureContent.JOURNAL_PLACE, SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(), 0.8f, 1.0f, player.getRandom().nextLong()));
        player.sendOverlayMessage(Component.translatable("journal." + StrataIndustria.MOD_ID + ".place.noted",
                display.get().getTitle()));
    }
}
