package dev.strataindustria.transport.ropeway;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The geometry of one ropeway (outposts spec 8.3), shared by the server's simulation and the client's drawing: the
 * sheave points of the terminal, the towers and the return, joined by straight spans. The line is a loop: a point
 * {@code s} from 0 to twice the length runs out along the outbound rope and back along the return rope, which hangs
 * on the other side of each span.
 */
public final class RopewayPath {
    /** How far either rope hangs from the centre line, which is also the radius of the bull wheels. */
    public static final double LATERAL = 0.3;
    /** Blocks over which a bucket swings round a wheel from one rope to the other. */
    public static final double TURN = 0.45;
    /** Sag at the middle of a span, as a fraction of its length. */
    public static final double SAG = 0.025;
    /** Height of the sheave point above the floor of its block. */
    public static final double RISE = 0.78;

    private final Vec3[] anchors;
    private final double[] cumulative;
    private final double length;

    /** A point on the line, and the way a bucket there is travelling. */
    public record Point(Vec3 position, Vec3 heading, int span) {}

    public RopewayPath(List<Vec3> anchors) {
        this.anchors = anchors.toArray(new Vec3[0]);
        this.cumulative = new double[this.anchors.length];
        for (int i = 1; i < this.anchors.length; i++) {
            cumulative[i] = cumulative[i - 1] + this.anchors[i].distanceTo(this.anchors[i - 1]);
        }
        this.length = this.anchors.length == 0 ? 0 : cumulative[this.anchors.length - 1];
    }

    public static Vec3 anchor(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + RISE, pos.getZ() + 0.5);
    }

    public static RopewayPath of(List<BlockPos> nodes) {
        return new RopewayPath(nodes.stream().map(RopewayPath::anchor).toList());
    }

    /** Blocks of line from the terminal to the return. */
    public double length() {
        return length;
    }

    /** Spans: one more than the towers. */
    public int spans() {
        return Math.max(0, anchors.length - 1);
    }

    public int nodes() {
        return anchors.length;
    }

    public Vec3 anchor(int node) {
        return anchors[node];
    }

    /** Distance along the line from the terminal to node {@code node}. */
    public double at(int node) {
        return cumulative[node];
    }

    public double spanLength(int span) {
        return cumulative[span + 1] - cumulative[span];
    }

    /** Horizontal unit vector at right angles to a span, to the right of the way out. */
    public Vec3 side(int span) {
        Vec3 along = anchors[span + 1].subtract(anchors[span]);
        double flat = Math.sqrt(along.x * along.x + along.z * along.z);
        return flat < 1.0E-6 ? new Vec3(1, 0, 0) : new Vec3(-along.z / flat, 0, along.x / flat);
    }

    /**
     * The unit offset of the outbound rope from the centre line at node {@code node}: the side of the span at the ends,
     * and at a bend the mitre of the two spans, so the ropes stay joined as they turn round an angle station.
     */
    public Vec3 lateral(int node) {
        if (spans() == 0) return new Vec3(1, 0, 0);
        if (node <= 0) return side(0);
        if (node >= spans()) return side(spans() - 1);
        Vec3 in = side(node - 1), out = side(node);
        double d = 1.0 + in.dot(out);
        return d < 0.25 ? out : in.add(out).scale(1.0 / d);
    }

    private Vec3 lateralAt(int span, double t) {
        return lateral(span).lerp(lateral(span + 1), t);
    }

    /** A point at fraction {@code t} of a span on the outbound rope ({@code side} 1) or the return rope (-1). */
    public Vec3 rope(int span, double t, int side) {
        Vec3 point = anchors[span].lerp(anchors[span + 1], t);
        double sag = 4.0 * SAG * spanLength(span) * t * (1.0 - t);
        return point.add(lateralAt(span, t).scale(side * LATERAL)).add(0, -sag, 0);
    }

    /** The span holding distance {@code u} along the line. */
    public int spanOf(double u) {
        int span = 0;
        while (span < spans() - 1 && u > cumulative[span + 1]) span++;
        return span;
    }

    /** Where a bucket at loop position {@code s} hangs, and which way it is going. */
    public Point point(double s) {
        double loop = 2.0 * length;
        s = ((s % loop) + loop) % loop;
        boolean out = s < length;
        double u = out ? s : loop - s;
        int span = spanOf(u);
        double len = spanLength(span);
        double t = len < 1.0E-6 ? 0.0 : Mth.clamp((u - cumulative[span]) / len, 0.0, 1.0);
        // Round a wheel the bucket crosses from one rope to the other.
        double toEnd = Math.min(u, length - u);
        double swing = Math.sin(Math.min(1.0, toEnd / TURN) * Math.PI / 2.0);
        Vec3 point = anchors[span].lerp(anchors[span + 1], t);
        double sag = 4.0 * SAG * len * t * (1.0 - t);
        Vec3 position = point.add(lateralAt(span, t).scale((out ? 1 : -1) * LATERAL * swing)).add(0, -sag, 0);
        Vec3 heading = headingAt(span, u);
        return new Point(position, out ? heading : heading.scale(-1), span);
    }

    private Vec3 direction(int span) {
        Vec3 along = anchors[span + 1].subtract(anchors[span]);
        return along.lengthSqr() < 1.0E-9 ? new Vec3(0, 0, 1) : along.normalize();
    }

    /** The way the outbound rope runs at {@code u} along the line, turning gently through the last blocks before a bend. */
    private Vec3 headingAt(int span, double u) {
        Vec3 along = direction(span);
        double into = u - cumulative[span], rest = cumulative[span + 1] - u;
        if (span > 0 && into < TURN) along = direction(span - 1).lerp(along, 0.5 + 0.5 * into / TURN);
        else if (span < spans() - 1 && rest < TURN) along = along.lerp(direction(span + 1), 0.5 - 0.5 * rest / TURN);
        return along.lengthSqr() < 1.0E-9 ? direction(span) : along.normalize();
    }

    /** How far {@code point} is from the nearest part of the line. */
    public double distanceTo(Vec3 point) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < spans(); i++) {
            Vec3 a = anchors[i], ab = anchors[i + 1].subtract(a);
            double len2 = ab.lengthSqr();
            double t = len2 < 1.0E-9 ? 0 : Mth.clamp(point.subtract(a).dot(ab) / len2, 0.0, 1.0);
            best = Math.min(best, a.add(ab.scale(t)).distanceTo(point));
        }
        return best;
    }

    /** Whether the loop passes {@code mark} while moving from {@code from} to {@code to} (both in [0, 2 x length)). */
    public static boolean crosses(double from, double to, double mark, double loop) {
        double a = ((from % loop) + loop) % loop, b = ((to % loop) + loop) % loop;
        double m = ((mark % loop) + loop) % loop;
        return a <= b ? a < m && m <= b : m > a || m <= b;
    }
}
