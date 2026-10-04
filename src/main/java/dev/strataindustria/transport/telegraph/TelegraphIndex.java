package dev.strataindustria.transport.telegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.OverheadLine;
import dev.strataindustria.electric.PoleInsulatorBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The telegraph network of a dimension (outposts spec 9.1): which pole insulators are joined by telegraph wire, which
 * keys, sounders and boards hang on which insulator, and the last report each key sent. Kept as saved data rather than
 * read off block entities because a line runs through chunks nobody has loaded; a call still gets through.
 */
public final class TelegraphIndex extends SavedData {
    /** How far a key, sounder or board reaches for a pole to hang its drop wire on. */
    public static final int DROP_REACH = 8;

    public enum Kind implements StringRepresentable {
        KEY("key"), SOUNDER("sounder"), BOARD("board");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String id;

        Kind(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** A device and the insulator its drop wire runs to, if it has one. */
    public record Device(BlockPos pos, Kind kind, Optional<BlockPos> drop) {
        static final Codec<Device> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Device::pos),
                Kind.CODEC.fieldOf("kind").forGetter(Device::kind),
                BlockPos.CODEC.optionalFieldOf("drop").forGetter(Device::drop)
        ).apply(i, Device::new));
    }

    /**
     * What a key last said about its charter. {@code state}: 0 loaded, 1 idle, 2 no line, 3 owner away, 4 area cut down.
     * {@code line}: 0 open, 1 cut, 2 telegraph only, 3 none. {@code kind} is the lang key of the best line, or empty.
     * {@code fill} is the percentage the container the key faces holds, or -1 with none. Times are game time.
     */
    public record Report(BlockPos key, UUID charter, String name, int state, int line, String kind, int fill, long lastTraffic, long at) {
        static final Codec<Report> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("key").forGetter(Report::key),
                UUIDUtil.CODEC.fieldOf("charter").forGetter(Report::charter),
                Codec.STRING.fieldOf("name").forGetter(Report::name),
                Codec.INT.fieldOf("state").forGetter(Report::state),
                Codec.INT.fieldOf("line").forGetter(Report::line),
                Codec.STRING.fieldOf("kind").forGetter(Report::kind),
                Codec.INT.fieldOf("fill").forGetter(Report::fill),
                Codec.LONG.fieldOf("last_traffic").forGetter(Report::lastTraffic),
                Codec.LONG.fieldOf("at").forGetter(Report::at)
        ).apply(i, Report::new));
    }

    private record Wire(BlockPos a, BlockPos b) {
        static final Codec<Wire> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("a").forGetter(Wire::a),
                BlockPos.CODEC.fieldOf("b").forGetter(Wire::b)
        ).apply(i, Wire::new));
    }

    private record Data(List<Wire> wires, List<Device> devices, List<Report> reports) {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Wire.CODEC.listOf().fieldOf("wires").forGetter(Data::wires),
                Device.CODEC.listOf().fieldOf("devices").forGetter(Data::devices),
                Report.CODEC.listOf().fieldOf("reports").forGetter(Data::reports)
        ).apply(i, Data::new));
    }

    public static final SavedDataType<TelegraphIndex> TYPE = new SavedDataType<>(StrataIndustria.id("telegraph_index"),
            TelegraphIndex::new, Data.CODEC.xmap(TelegraphIndex::new, TelegraphIndex::data));

    private final Map<BlockPos, Set<BlockPos>> wires = new LinkedHashMap<>();
    private final Map<BlockPos, Device> devices = new LinkedHashMap<>();
    private final Map<BlockPos, Report> reports = new LinkedHashMap<>();

    public TelegraphIndex() {}

    private TelegraphIndex(Data data) {
        for (Wire wire : data.wires()) link(wire.a(), wire.b());
        for (Device device : data.devices()) devices.put(device.pos(), device);
        for (Report report : data.reports()) reports.put(report.key(), report);
    }

    private Data data() {
        List<Wire> list = new ArrayList<>();
        for (var entry : wires.entrySet()) {
            for (BlockPos other : entry.getValue()) if (entry.getKey().asLong() < other.asLong()) list.add(new Wire(entry.getKey(), other));
        }
        return new Data(list, List.copyOf(devices.values()), List.copyOf(reports.values()));
    }

    public static TelegraphIndex get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ---------------------------------------------------------------- wires

    private void link(BlockPos a, BlockPos b) {
        wires.computeIfAbsent(a.immutable(), k -> new LinkedHashSet<>()).add(b.immutable());
        wires.computeIfAbsent(b.immutable(), k -> new LinkedHashSet<>()).add(a.immutable());
    }

    public void addWire(BlockPos a, BlockPos b) {
        link(a, b);
        setDirty();
    }

    public List<BlockPos> wiresOf(BlockPos insulator) {
        Set<BlockPos> set = wires.get(insulator);
        return set == null ? List.of() : List.copyOf(set);
    }

    public boolean wired(BlockPos a, BlockPos b) {
        Set<BlockPos> set = wires.get(a);
        return set != null && set.contains(b);
    }

    /** An insulator is gone: its wires go with it and the devices hanging on it lose their drop. Returns the far ends. */
    public List<BlockPos> removeInsulator(BlockPos insulator) {
        Set<BlockPos> far = wires.remove(insulator);
        List<BlockPos> ends = far == null ? List.of() : List.copyOf(far);
        for (BlockPos other : ends) {
            Set<BlockPos> set = wires.get(other);
            if (set == null) continue;
            set.remove(insulator);
            if (set.isEmpty()) wires.remove(other);
        }
        for (var entry : List.copyOf(devices.entrySet())) {
            Device device = entry.getValue();
            if (device.drop().isPresent() && device.drop().get().equals(insulator)) devices.put(entry.getKey(), new Device(device.pos(), device.kind(), Optional.empty()));
        }
        setDirty();
        return ends;
    }

    // ---------------------------------------------------------------- devices

    public void register(BlockPos pos, Kind kind) {
        Device old = devices.get(pos);
        if (old != null && old.kind() == kind) return;
        devices.put(pos.immutable(), new Device(pos.immutable(), kind, old == null ? Optional.empty() : old.drop()));
        setDirty();
    }

    public void unregister(BlockPos pos) {
        boolean changed = devices.remove(pos) != null;
        changed |= reports.remove(pos) != null;
        if (changed) setDirty();
    }

    public @Nullable Device device(BlockPos pos) {
        return devices.get(pos);
    }

    public List<Device> devices(Kind kind) {
        return devices.values().stream().filter(d -> d.kind() == kind).toList();
    }

    /** The insulator a device hangs on, if any. */
    public @Nullable BlockPos dropOf(BlockPos pos) {
        Device device = devices.get(pos);
        return device == null ? null : device.drop().orElse(null);
    }

    /**
     * Hangs a device's drop wire on the nearest pole insulator in reach with a clear line to it. Returns the insulator,
     * or null when there is none (or the device already hangs on one that is still there).
     */
    public @Nullable BlockPos attach(ServerLevel level, BlockPos pos, Kind kind) {
        register(pos, kind);
        Device device = devices.get(pos);
        if (device.drop().isPresent() && level.getBlockEntity(device.drop().get()) instanceof PoleInsulatorBlockEntity) return null;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-DROP_REACH, -DROP_REACH, -DROP_REACH), pos.offset(DROP_REACH, DROP_REACH, DROP_REACH))) {
            if (!(level.getBlockEntity(near) instanceof PoleInsulatorBlockEntity)) continue;
            double distance = near.distSqr(pos);
            if (distance >= bestDistance || distance > DROP_REACH * DROP_REACH) continue;
            if (!clear(level, pos, near)) continue;
            best = near.immutable();
            bestDistance = distance;
        }
        if (best == null) return null;
        devices.put(pos.immutable(), new Device(pos.immutable(), kind, Optional.of(best)));
        setDirty();
        return best;
    }

    /** Gives every device without a drop the chance to take the insulator just placed at {@code insulator}. */
    public List<BlockPos> insulatorPlaced(ServerLevel level, BlockPos insulator) {
        List<BlockPos> taken = new ArrayList<>();
        for (Device device : List.copyOf(devices.values())) {
            if (device.drop().isPresent() && level.getBlockEntity(device.drop().get()) instanceof PoleInsulatorBlockEntity) continue;
            if (device.pos().distSqr(insulator) > DROP_REACH * DROP_REACH || !level.isLoaded(device.pos())) continue;
            if (attach(level, device.pos(), device.kind()) != null) taken.add(device.pos());
        }
        return taken;
    }

    /** Whether no solid block stands between a device and an insulator's clamp. */
    public static boolean clear(ServerLevel level, BlockPos device, BlockPos insulator) {
        Vec3 from = Vec3.atCenterOf(device), to = PoleInsulatorBlockEntity.tip(insulator, level.getBlockState(insulator));
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, level, (l, p) -> {
            if (p.equals(device) || p.equals(insulator)) return null;
            VoxelShape shape = l.getBlockState(p).getCollisionShape(l, p);
            return shape.isEmpty() ? null : shape.clip(from, to, p);
        }, l -> null);
        return hit == null;
    }

    // ---------------------------------------------------------------- networks

    /** The insulators joined to {@code start} by telegraph wire, start included. */
    public Set<BlockPos> network(BlockPos start) {
        Set<BlockPos> seen = new LinkedHashSet<>();
        List<BlockPos> open = new ArrayList<>(List.of(start));
        while (!open.isEmpty()) {
            BlockPos next = open.remove(open.size() - 1);
            if (!seen.add(next)) continue;
            Set<BlockPos> far = wires.get(next);
            if (far != null) open.addAll(far);
        }
        return seen;
    }

    /** The devices of a kind whose drop is on {@code network}. */
    public List<Device> onNetwork(Set<BlockPos> network, Kind kind) {
        List<Device> list = new ArrayList<>();
        for (Device device : devices.values()) {
            if (device.kind() == kind && device.drop().isPresent() && network.contains(device.drop().get())) list.add(device);
        }
        return list;
    }

    /** The shortest chain of insulators from {@code from} to {@code to}, both included, or empty when they are not joined. */
    public List<BlockPos> path(BlockPos from, BlockPos to) {
        Map<BlockPos, BlockPos> came = new HashMap<>();
        List<BlockPos> queue = new ArrayList<>(List.of(from));
        Set<BlockPos> seen = new HashSet<>(queue);
        for (int head = 0; head < queue.size(); head++) {
            BlockPos at = queue.get(head);
            if (at.equals(to)) {
                List<BlockPos> chain = new ArrayList<>();
                for (BlockPos step = to; step != null; step = came.get(step)) chain.add(0, step);
                return chain;
            }
            for (BlockPos far : wires.getOrDefault(at, Set.of())) {
                if (seen.add(far)) {
                    came.put(far, at);
                    queue.add(far);
                }
            }
        }
        return List.of();
    }

    /** Forgets every wire, device and report within {@code radius} blocks of {@code centre}. For tests and server tools. */
    public void forget(BlockPos centre, int radius) {
        long limit = (long) radius * radius;
        boolean changed = wires.keySet().removeIf(p -> p.distSqr(centre) <= limit);
        for (Set<BlockPos> set : wires.values()) changed |= set.removeIf(p -> p.distSqr(centre) <= limit);
        wires.values().removeIf(Set::isEmpty);
        changed |= devices.keySet().removeIf(p -> p.distSqr(centre) <= limit);
        changed |= reports.keySet().removeIf(p -> p.distSqr(centre) <= limit);
        if (changed) setDirty();
    }

    // ---------------------------------------------------------------- reports

    public void report(Report report) {
        reports.put(report.key(), report);
        setDirty();
    }

    /** The latest report of each charter heard on {@code network}, oldest charter name first. */
    public List<Report> reportsOn(Set<BlockPos> network) {
        Map<UUID, Report> latest = new LinkedHashMap<>();
        for (Report report : reports.values()) {
            BlockPos drop = dropOf(report.key());
            if (drop == null || !network.contains(drop)) continue;
            Report held = latest.get(report.charter());
            if (held == null || held.at() < report.at()) latest.put(report.charter(), report);
        }
        List<Report> list = new ArrayList<>(latest.values());
        list.sort((x, y) -> x.name().compareToIgnoreCase(y.name()));
        return list;
    }

    /** Number of wires, for tests and for the board. */
    public int wireCount() {
        int count = 0;
        for (Set<BlockPos> set : wires.values()) count += set.size();
        return count / 2;
    }

    /** Whether an insulator may take one more telegraph wire. */
    public boolean roomAt(BlockPos insulator) {
        return wires.getOrDefault(insulator, Set.of()).size() < PoleInsulatorBlockEntity.MAX_SPANS;
    }

    /** Wires a span between two clamps costs: one per eight blocks. */
    public static int wireFor(BlockPos a, BlockPos b) {
        return (int) Math.ceil(OverheadLine.length(a, b) / 8.0 - 1.0e-9);
    }
}
