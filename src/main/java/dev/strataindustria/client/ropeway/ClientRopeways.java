package dev.strataindustria.client.ropeway;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.ropeway.RopewayPath;
import dev.strataindustria.transport.ropeway.RopewayPayloads;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * The ropeways this client has been told about (outposts spec 8.3): each line's shape, rope speed and buckets, as of
 * the last packet. The rope's advance runs on from there at the same speed until the next one, so buckets glide.
 * A line nobody has mentioned for a while is forgotten: its terminal is out of range or gone.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class ClientRopeways {
    /** Ticks without news after which a line is dropped; the server repeats itself every 40. */
    private static final long STALE = 120;

    /** One line as the last packet described it. */
    public static final class Line {
        public final BlockPos terminal;
        public final List<BlockPos> nodes;
        public final RopewayPath path;
        public final List<RopewayPayloads.BucketView> buckets;
        private final double advance;
        private final float speed;
        private final long received;

        Line(RopewayPayloads.LineState state, long received) {
            this.terminal = state.terminal();
            this.nodes = state.nodes();
            this.path = RopewayPath.of(state.nodes());
            this.buckets = state.buckets();
            this.advance = state.advance();
            this.speed = state.speed();
            this.received = received;
        }

        /** Where the rope has got to at {@code now} (game ticks and a fraction). */
        public double advance(double now) {
            return advance + speed * Math.max(0.0, now - received);
        }

        public float speed() {
            return speed;
        }
    }

    private static final Map<Long, Line> LINES = new HashMap<>();

    private ClientRopeways() {}

    public static void accept(RopewayPayloads.LineState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (state.nodes().size() < 2) {
            LINES.remove(state.terminal().asLong());
            return;
        }
        LINES.put(state.terminal().asLong(), new Line(state, minecraft.level.getGameTime()));
    }

    public static @Nullable Line get(BlockPos terminal) {
        return LINES.get(terminal.asLong());
    }

    /** Time now in ticks, with the fraction of the tick being drawn. */
    public static double now(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return (minecraft.level == null ? 0 : minecraft.level.getGameTime()) + (double) partialTick;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LINES.clear();
            return;
        }
        long time = minecraft.level.getGameTime();
        Iterator<Line> each = LINES.values().iterator();
        while (each.hasNext()) if (time - each.next().received > STALE) each.remove();
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LINES.clear();
    }
}
