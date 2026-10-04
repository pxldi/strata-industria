package dev.strataindustria.client;

import dev.strataindustria.client.screen.OreScannerScreen;
import dev.strataindustria.prospecting.OreScan;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.registry.Tier5Items;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Client half of the ore scanner: only ever called on the client. */
public final class OreScannerClient {
    /** Opens the map of the last scan on the scanner in either hand. */
    public static void openHeld() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            OreScan scan = stack.is(Tier5Items.ORE_SCANNER.get()) ? stack.get(Tier5DataComponents.ORE_SCAN.get()) : null;
            if (scan != null) {
                minecraft.gui.setScreen(new OreScannerScreen(scan));
                return;
            }
        }
    }

    private OreScannerClient() {}
}
