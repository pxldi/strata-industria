package dev.strataindustria.event;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.journal.Leads;
import dev.strataindustria.journal.Observations;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The field journal (spec 3.6 and 11, journal leads spec): the journal on first join, keeping the leads notebook in
 * step with the player's goals, observations, remembered hints, and worn armour.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class JournalEvents {
    static final String GIVEN = StrataIndustria.MOD_ID + ":journal_given";
    static final int ARMOUR_INTERVAL = 40;
    static final int FIND_INTERVAL = 20;
    static final int REMEMBER_INTERVAL = 200;

    private JournalEvents() {}

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Leads.refresh(player);
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

    /** A goal done opens the leads under it and closes its own. */
    @SubscribeEvent
    static void onEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Leads.isLead(event.getAdvancement().id())) Leads.refresh(player);
    }

    /** Datapack reloads can add goals. */
    @SubscribeEvent
    static void onReload(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(Leads::refresh);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % ARMOUR_INTERVAL == 0 && wearsFullBronze(player)) Journal.award(player, Journal.BRONZE_ARMOUR);
        if (player.tickCount % FIND_INTERVAL == 0) Observations.scan(player);
        if (player.tickCount % REMEMBER_INTERVAL == 0) Leads.remember(player);
    }

    /** Swinging a pick at ore it cannot break. */
    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer player) Observations.tooHard(player, event.getState());
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
