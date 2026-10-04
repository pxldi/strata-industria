package dev.strataindustria.transport.outpost;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/** What the charter board shows (outposts spec 3.1), built on the server and sent when the board opens. */
public record BoardView(BlockPos pos, String name, String ownerName, boolean mine, OutpostPlan.State state, int wanted, int side,
        List<Line> lines, long lastTrafficAgo) {
    /** One line of the board: a link to another charter. {@code cutAt} is null while it is open. */
    public record Line(String other, String kind, boolean open, int length, @Nullable BlockPos cutAt) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardView> CODEC = StreamCodec.of(BoardView::write, BoardView::read);

    private static void write(RegistryFriendlyByteBuf buf, BoardView v) {
        buf.writeBlockPos(v.pos);
        buf.writeUtf(v.name, RouteIndex.MAX_NAME * 4);
        buf.writeUtf(v.ownerName, 64);
        buf.writeBoolean(v.mine);
        buf.writeEnum(v.state);
        buf.writeVarInt(v.wanted);
        buf.writeVarInt(v.side);
        buf.writeVarInt(v.lines.size());
        for (Line line : v.lines) {
            buf.writeUtf(line.other, RouteIndex.MAX_NAME * 4);
            buf.writeUtf(line.kind, 64);
            buf.writeBoolean(line.open);
            buf.writeVarInt(line.length);
            buf.writeBoolean(line.cutAt != null);
            if (line.cutAt != null) buf.writeBlockPos(line.cutAt);
        }
        buf.writeVarLong(v.lastTrafficAgo + 1);
    }

    private static BoardView read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String name = buf.readUtf(RouteIndex.MAX_NAME * 4), owner = buf.readUtf(64);
        boolean mine = buf.readBoolean();
        OutpostPlan.State state = buf.readEnum(OutpostPlan.State.class);
        int wanted = buf.readVarInt(), side = buf.readVarInt();
        int count = buf.readVarInt();
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String other = buf.readUtf(RouteIndex.MAX_NAME * 4), kind = buf.readUtf(64);
            boolean open = buf.readBoolean();
            int length = buf.readVarInt();
            BlockPos cut = buf.readBoolean() ? buf.readBlockPos() : null;
            lines.add(new Line(other, kind, open, length, cut));
        }
        return new BoardView(pos, name, owner, mine, state, wanted, side, lines, buf.readVarLong() - 1);
    }

    /** The board of {@code charter} as seen by a player who is or is not one of its owners. */
    public static BoardView of(ServerLevel level, RouteIndex index, Charter charter, boolean mine) {
        OutpostPlan.Entry entry = index.plan(level).entry(charter.id());
        List<Line> lines = new ArrayList<>();
        long latest = -1;
        for (Link link : index.linksOf(charter.id())) {
            lines.add(new Line(index.name(link.other(charter.id())), link.kind().langKey(), link.open(), link.length(), link.cutAt().orElse(null)));
            latest = Math.max(latest, link.lastTraffic());
        }
        long ago = latest < 0 ? -1 : Math.max(0, level.getGameTime() - latest);
        OutpostPlan.State state = entry == null ? OutpostPlan.State.NO_LINE : entry.state();
        return new BoardView(charter.pos(), charter.name(), charter.ownerName(), mine, state, entry == null ? 0 : entry.wanted(),
                entry == null ? 0 : entry.side(), lines, ago);
    }
}
