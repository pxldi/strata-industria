package dev.strataindustria.client;

import dev.strataindustria.client.screen.OreScannerScreen;
import dev.strataindustria.oil.SeismicSurvey;
import dev.strataindustria.prospecting.OreScan;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.registry.Tier6DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Client half of the ore scanner: only ever called on the client. */
public final class OreScannerClient {
    /** Opens the map of the last scan on the scanner in either hand. */
    public static void openHeld() {
        openHeld(false);
    }

    /** Opens the scanner in either hand on its ore tab, or on its seismic tab when {@code seismic} and it has a survey. */
    public static void openHeld(boolean seismic) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (!stack.is(Tier5Items.ORE_SCANNER.get())) continue;
            OreScan scan = stack.get(Tier5DataComponents.ORE_SCAN.get());
            SeismicSurvey survey = stack.get(Tier6DataComponents.SEISMIC_RESULT.get());
            if (scan == null && survey == null) continue;
            minecraft.gui.setScreen(new OreScannerScreen(scan, survey, seismic && survey != null || scan == null));
            return;
        }
    }

    private OreScannerClient() {}
}
