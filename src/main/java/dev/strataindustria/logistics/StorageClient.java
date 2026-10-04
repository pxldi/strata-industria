package dev.strataindustria.logistics;

import net.minecraft.client.Minecraft;

/** The client's end of the snapshot packet, kept apart so the common packet class never touches client code. */
final class StorageClient {
    private StorageClient() {}

    static void apply(StoragePayloads.Snapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null && minecraft.player.containerMenu instanceof StorageControllerMenu menu
                    && menu.containerId == snapshot.containerId()) {
                menu.apply(snapshot.entries(), snapshot.powered(), snapshot.inventories());
            }
        });
    }
}
