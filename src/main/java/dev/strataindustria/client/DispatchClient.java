package dev.strataindustria.client;

import dev.strataindustria.client.screen.DispatchBoardScreen;
import dev.strataindustria.transport.telegraph.DispatchView;
import net.minecraft.client.Minecraft;

/** Client half of the dispatch board: only ever called on the client. */
public final class DispatchClient {
    public static void open(DispatchView view) {
        Minecraft.getInstance().gui.setScreen(new DispatchBoardScreen(view));
    }

    private DispatchClient() {}
}
