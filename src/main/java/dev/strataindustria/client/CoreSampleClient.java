package dev.strataindustria.client;

import dev.strataindustria.client.screen.CoreSampleScreen;
import dev.strataindustria.prospecting.CoreSample;
import net.minecraft.client.Minecraft;

/** Client half of the core sample: only ever called on the client. */
public final class CoreSampleClient {
    public static void open(CoreSample sample) {
        Minecraft.getInstance().gui.setScreen(new CoreSampleScreen(sample));
    }

    private CoreSampleClient() {}
}
