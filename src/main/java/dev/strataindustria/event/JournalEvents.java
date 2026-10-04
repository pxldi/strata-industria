package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The field journal (spec 3.6 and 11): the journal on first join, the next-goal reminder, worn armour. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class JournalEvents {
    static final String GIVEN = StrataIndustria.MOD_ID + ":journal_given";
    /** The reminder waits until the world has drawn and the player can read it. */
    static final int REMINDER_DELAY = 100;
    static final int ARMOUR_INTERVAL = 40;

    private static final Map<UUID, Integer> REMINDERS = new HashMap<>();

    private JournalEvents() {}

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        REMINDERS.put(player.getUUID(), REMINDER_DELAY);
        if (!Config.JOURNAL_GIVE_ON_JOIN.get()) return;
        // Kept under the persisted tag so it survives death; the journal comes once per player.
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        if (persisted.getBooleanOr(GIVEN, false)) return;
        persisted.putBoolean(GIVEN, true);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        ItemStack journal = new ItemStack(ModItems.FIELD_JOURNAL.get());
        player.getInventory().placeItemBackInInventory(journal, Prediction.SERVER_ONLY);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REMINDERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer left = REMINDERS.get(player.getUUID());
        if (left != null) {
            if (left <= 0) {
                REMINDERS.remove(player.getUUID());
                remind(player);
            } else {
                REMINDERS.put(player.getUUID(), left - 1);
            }
        }
        if (player.tickCount % ARMOUR_INTERVAL == 0 && wearsFullBronze(player)) Journal.award(player, Journal.BRONZE_ARMOUR);
    }

    private static void remind(ServerPlayer player) {
        AdvancementHolder next = Journal.nextGoal(player);
        if (next == null || next.value().display().isEmpty()) return;
        player.sendSystemMessage(Component.translatable("journal." + StrataIndustria.MOD_ID + ".next",
                next.value().display().get().title().copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
    }

    /** All four pieces from any bronze (spec 11, goal 24). */
    static boolean wearsFullBronze(ServerPlayer player) {
        for (ArmorType type : ModItems.armourTypes()) {
            ItemStack worn = player.getItemBySlot(type.getSlot());
            boolean bronze = false;
            for (Metal metal : Metal.values()) {
                if (!metal.isBronze() || !ModItems.ARMOUR.containsKey(metal)) continue;
                if (worn.is(ModItems.ARMOUR.get(metal).get(type).get())) bronze = true;
            }
            if (!bronze) return false;
        }
        return true;
    }
}
