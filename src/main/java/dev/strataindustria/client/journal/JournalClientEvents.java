package dev.strataindustria.client.journal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.JournalContent;
import dev.strataindustria.journal.JournalState;
import dev.strataindustria.journal.Leads;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Turns changes in the synced notebook into slips in the corner (journal leads spec): a new lead, a lead crossed
 * off, a hint remembered, a note, a new chapter. On joining, one slip reminds the player of an open lead.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class JournalClientEvents {
    /** Changes during the first ticks are the login sync, not news. */
    static final int SETTLE_TICKS = 60;
    static final int REMINDER_TICK = 100;
    /** More news than this at once is folded into one slip. */
    static final int MAX_SLIPS = 3;

    private static int seen = -1;
    private static boolean reminded;
    /** One pencil sound per batch of slips, however many arrive together. */
    private static long soundTick = -1;

    private JournalClientEvents() {}

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        seen = -1;
        reminded = false;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        JournalState state = player.getData(JournalContent.STATE);
        if (seen < 0 || player.tickCount < SETTLE_TICKS) {
            seen = state.seq();
            return;
        }
        if (!reminded && player.tickCount >= REMINDER_TICK) {
            reminded = true;
            List<JournalState.Lead> open = Leads.open(state);
            if (!open.isEmpty()) {
                JournalState.Lead lead = open.getLast();
                show(minecraft, new JournalToast(JournalText.ui("toast.reminder"), JournalText.question(lead),
                        JournalText.icon(lead.icon()), false), null);
            }
        }
        if (state.seq() <= seen) return;
        List<Runnable> slips = news(minecraft, state, seen);
        seen = state.seq();
        if (slips.size() > MAX_SLIPS) {
            int more = slips.size() - (MAX_SLIPS - 1);
            slips = new ArrayList<>(slips.subList(0, MAX_SLIPS - 1));
            slips.add(() -> show(minecraft, new JournalToast(JournalText.ui("toast.more_heading"), JournalText.ui("toast.more", more),
                    new ItemStack(dev.strataindustria.registry.ModItems.FIELD_JOURNAL.get()), false), JournalContent.WRITE.get()));
        }
        slips.forEach(Runnable::run);
    }

    private record Slip(int seq, Runnable show) {}

    /** One slip per change after {@code since}, oldest first. */
    static List<Runnable> news(Minecraft minecraft, JournalState state, int since) {
        List<Slip> slips = new ArrayList<>();
        Set<Integer> chapters = new HashSet<>();
        for (JournalState.Lead lead : state.leads().values()) {
            if (lead.openedSeq() >= 0 && lead.openedSeq() < since) chapters.add(JournalText.tier(lead));
        }
        for (JournalState.Lead lead : Leads.open(state)) {
            if (lead.openedSeq() < since) continue;
            int tier = JournalText.tier(lead);
            if (chapters.add(tier) && tier > 0) {
                slips.add(new Slip(lead.openedSeq(), () -> show(minecraft, new JournalToast(JournalText.ui("toast.chapter"), JournalText.chapter(tier),
                        JournalText.icon(lead.icon()), true), JournalContent.PAGE.get())));
            }
            slips.add(new Slip(lead.openedSeq(), () -> show(minecraft, new JournalToast(JournalText.ui("toast.lead"),
                    JournalText.question(lead), JournalText.icon(lead.icon()), false), JournalContent.WRITE.get())));
        }
        for (JournalState.Lead lead : state.leads().values()) {
            if (lead.closedSeq() >= since) {
                slips.add(new Slip(lead.closedSeq(), () -> show(minecraft, new JournalToast(JournalText.ui("toast.closed"),
                        JournalText.title(lead), JournalText.icon(lead.icon()), false), JournalContent.CROSS_OFF.get())));
            } else if (lead.open() && lead.hintedSeq() >= since) {
                slips.add(new Slip(lead.hintedSeq(), () -> show(minecraft, new JournalToast(JournalText.ui("toast.remember"),
                        JournalText.question(lead), JournalText.icon(lead.icon()), false), JournalContent.REMEMBER.get())));
            }
        }
        for (JournalState.Note note : state.notes()) {
            if (note.seq() < since || note.quiet()) continue;
            slips.add(new Slip(note.seq(), () -> show(minecraft, new JournalToast(JournalText.ui("toast.note"), JournalText.note(note),
                    note.icon().map(JournalText::icon).orElse(ItemStack.EMPTY), false), JournalContent.WRITE.get())));
        }
        slips.sort((a, b) -> Integer.compare(a.seq(), b.seq()));
        List<Runnable> out = new ArrayList<>();
        slips.forEach(slip -> out.add(slip.show()));
        return out;
    }

    static void show(Minecraft minecraft, JournalToast toast, SoundEvent sound) {
        minecraft.gui.toastManager().addToast(toast);
        long tick = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        if (sound != null && tick != soundTick) {
            soundTick = tick;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f, 0.8f));
        }
    }
}
