package dev.strataindustria.journal;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registry entries of the leads notebook (journal leads spec): the per-player state and the notebook's sounds. */
public final class JournalContent {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StrataIndustria.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, StrataIndustria.MOD_ID);

    /** Only the owner ever receives their notebook. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<JournalState>> STATE = ATTACHMENTS.register("journal",
            () -> AttachmentType.builder(JournalState::new)
                    .serialize(JournalState.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, JournalState.STREAM_CODEC)
                    .build());

    /** A pencil writing a new lead or note. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WRITE = sound("journal.write");
    /** A lead crossed off: two quick strokes of the pencil. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CROSS_OFF = sound("journal.cross_off");
    /** Something remembered: a page leafed back to. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REMEMBER = sound("journal.remember");
    /** Turning a page of the notebook. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PAGE = sound("journal.page");
    /** A card pinned to the corkboard. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PIN = sound("journal.pin");
    /** Looking a block over closely and jotting it down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STUDY = sound("journal.study");

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        SOUNDS.register(bus);
    }

    public static JournalState state(ServerPlayer player) {
        return player.getData(STATE);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    private JournalContent() {}
}
