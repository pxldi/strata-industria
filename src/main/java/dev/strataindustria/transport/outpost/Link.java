package dev.strataindustria.transport.outpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

/**
 * A proven route between two charters (outposts spec 3.3): the exact blocks that carried something from one
 * station to the other. Open until one of those blocks is removed.
 */
public record Link(UUID id, UUID a, UUID b, LinkKind kind, List<BlockPos> route, Optional<BlockPos> cutAt, long lastTraffic) {
    public static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Link::id),
            UUIDUtil.CODEC.fieldOf("a").forGetter(Link::a),
            UUIDUtil.CODEC.fieldOf("b").forGetter(Link::b),
            LinkKind.CODEC.fieldOf("kind").forGetter(Link::kind),
            BlockPos.CODEC.listOf().fieldOf("route").forGetter(Link::route),
            BlockPos.CODEC.optionalFieldOf("cut_at").forGetter(Link::cutAt),
            Codec.LONG.optionalFieldOf("last_traffic", -1L).forGetter(Link::lastTraffic)
    ).apply(i, Link::new));

    public boolean open() {
        return cutAt.isEmpty();
    }

    public int length() {
        return route.size();
    }

    public boolean joins(UUID charter) {
        return a.equals(charter) || b.equals(charter);
    }

    public UUID other(UUID charter) {
        return a.equals(charter) ? b : a;
    }

    public Link cut(BlockPos at) {
        return new Link(id, a, b, kind, route, Optional.of(at), lastTraffic);
    }

    public Link withTraffic(long time) {
        return new Link(id, a, b, kind, route, cutAt, time);
    }
}
