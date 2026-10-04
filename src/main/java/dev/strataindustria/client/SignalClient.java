package dev.strataindustria.client;

import dev.strataindustria.client.screen.TimetableScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import dev.strataindustria.transport.signal.TimetableStops;

/** The client side of the line control: the timetable screen and the route switch screen. */
public final class SignalClient {
    private SignalClient() {}

    public static void openTimetable(TimetableStops stops) {
        Minecraft.getInstance().gui.setScreen(TimetableScreen.forTimetable(stops));
    }

    public static void openSwitch(BlockPos pos, List<String> stops) {
        Minecraft.getInstance().gui.setScreen(TimetableScreen.forSwitch(pos, stops));
    }
}
