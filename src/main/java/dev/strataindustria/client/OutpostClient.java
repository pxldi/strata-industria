package dev.strataindustria.client;

import dev.strataindustria.client.screen.CharterBoardScreen;
import dev.strataindustria.transport.outpost.BoardView;
import net.minecraft.client.Minecraft;

/** Client half of the charter board: only ever called on the client. */
public final class OutpostClient {
    public static void openBoard(BoardView view) {
        Minecraft.getInstance().gui.setScreen(new CharterBoardScreen(view));
    }

    private OutpostClient() {}
}
