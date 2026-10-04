package dev.strataindustria.transport.telegraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;

/**
 * What the full dispatch board screen shows (outposts spec 9.1): one line per reporting charter, with ages worked
 * out on the server at the moment the board is opened. A negative age or fill means there is nothing to show.
 */
public record DispatchView(BlockPos pos, boolean onLine, List<Line> lines) {
    /** Heard from this long ago, in ticks, and silent beyond {@link #SILENT_AFTER}. */
    public static final long SILENT_AFTER = 1800;

    public record Line(String name, int state, int line, String kind, int fill, long trafficAgo, long heardAgo) {
        public boolean silent() {
            return heardAgo > SILENT_AFTER;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, DispatchView> CODEC = StreamCodec.of(DispatchView::write, DispatchView::read);

    private static void write(RegistryFriendlyByteBuf buf, DispatchView v) {
        buf.writeBlockPos(v.pos);
        buf.writeBoolean(v.onLine);
        buf.writeVarInt(v.lines.size());
        for (Line line : v.lines) {
            buf.writeUtf(line.name, 128);
            buf.writeVarInt(line.state);
            buf.writeVarInt(line.line);
            buf.writeUtf(line.kind, 64);
            buf.writeVarInt(line.fill + 1);
            buf.writeVarLong(line.trafficAgo + 1);
            buf.writeVarLong(line.heardAgo);
        }
    }

    private static DispatchView read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean onLine = buf.readBoolean();
        int count = buf.readVarInt();
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(new Line(buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64), buf.readVarInt() - 1,
                    buf.readVarLong() - 1, buf.readVarLong()));
        }
        return new DispatchView(pos, onLine, lines);
    }

    /** The board at {@code pos} as it reads right now. */
    public static DispatchView of(ServerLevel level, BlockPos pos) {
        TelegraphIndex index = TelegraphIndex.get(level);
        BlockPos drop = index.dropOf(pos);
        if (drop == null) return new DispatchView(pos, false, List.of());
        Set<BlockPos> network = index.network(drop);
        long now = level.getGameTime();
        List<Line> lines = new ArrayList<>();
        for (TelegraphIndex.Report r : index.reportsOn(network)) {
            lines.add(new Line(r.name(), r.state(), r.line(), r.kind(), r.fill(), r.lastTraffic() < 0 ? -1 : Math.max(0, now - r.lastTraffic()),
                    Math.max(0, now - r.at())));
        }
        return new DispatchView(pos, true, lines);
    }
}
