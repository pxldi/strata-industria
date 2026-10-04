package dev.strataindustria.client;

import dev.strataindustria.journal.Journal;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Client half of the field journal: only ever called on the client. */
public final class JournalClient {
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) return;
        ClientAdvancements advancements = connection.getAdvancements();
        minecraft.gui.setScreen(new AdvancementsScreen(advancements));
        AdvancementHolder root = advancements.get(Journal.ROOT);
        if (root != null) advancements.setSelectedTab(root, true);
    }

    private JournalClient() {}
}
