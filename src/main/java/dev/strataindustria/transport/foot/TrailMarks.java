package dev.strataindustria.transport.foot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The cairns and blazes each player has set, in the order they set them (spec 4.4). A mark within
 * {@code outposts.trailStep} blocks of the player's previous mark joins its trail; a farther one starts a new trail.
 * Marks load nothing and cost nothing: this is only a list a mark can be asked about.
 */
public final class TrailMarks extends SavedData {
    /** Most marks kept per player; the oldest are forgotten first. */
    public static final int MAX_MARKS = 512;

    public record Mark(Identifier dimension, BlockPos pos, int trail) {
        static final Codec<Mark> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Mark::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Mark::pos),
                Codec.INT.fieldOf("trail").forGetter(Mark::trail)
        ).apply(i, Mark::new));
    }

    private record Owner(UUID player, List<Mark> marks) {
        static final Codec<Owner> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Owner::player),
                Mark.CODEC.listOf().fieldOf("marks").forGetter(Owner::marks)
        ).apply(i, Owner::new));
    }

    private static final Codec<TrailMarks> CODEC = Owner.CODEC.listOf().fieldOf("players").codec()
            .xmap(TrailMarks::new, TrailMarks::owners);

    public static final SavedDataType<TrailMarks> TYPE = new SavedDataType<>(StrataIndustria.id("trail_marks"),
            TrailMarks::new, CODEC);

    /** What a mark knows about its neighbours on the trail. */
    public record Neighbours(@Nullable Mark next, @Nullable Mark back, int length) {}

    private final Map<UUID, List<Mark>> marks = new HashMap<>();

    public TrailMarks() {}

    private TrailMarks(List<Owner> owners) {
        for (Owner owner : owners) marks.put(owner.player(), new ArrayList<>(owner.marks()));
    }

    private List<Owner> owners() {
        List<Owner> out = new ArrayList<>();
        marks.forEach((player, list) -> out.add(new Owner(player, List.copyOf(list))));
        return out;
    }

    public static TrailMarks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Adds a mark for a player. Returns how many marks the mark's trail now has, counting this one, so a caller
     * can notice the fourth.
     */
    public int add(UUID player, ServerLevel level, BlockPos pos) {
        Identifier dimension = level.dimension().identifier();
        List<Mark> list = marks.computeIfAbsent(player, p -> new ArrayList<>());
        int step = Config.TRAIL_STEP.getAsInt();
        int trail = 0;
        if (!list.isEmpty()) {
            Mark last = list.getLast();
            trail = last.trail();
            boolean joins = last.dimension().equals(dimension) && last.pos().distSqr(pos) <= (long) step * step;
            if (!joins) trail++;
        }
        list.add(new Mark(dimension, pos.immutable(), trail));
        while (list.size() > MAX_MARKS) list.removeFirst();
        setDirty();
        return trailLength(list, trail);
    }

    /** Forgets whatever mark stood at {@code pos}, for whoever set it. */
    public void remove(ServerLevel level, BlockPos pos) {
        Identifier dimension = level.dimension().identifier();
        boolean changed = false;
        for (List<Mark> list : marks.values()) {
            changed |= list.removeIf(mark -> mark.dimension().equals(dimension) && mark.pos().equals(pos));
        }
        if (changed) setDirty();
    }

    /** The marks either side of {@code pos}, looking first at the asking player's trails and then at anyone's. */
    public @Nullable Neighbours neighbours(UUID asker, ServerLevel level, BlockPos pos) {
        Neighbours own = neighbours(marks.get(asker), level, pos);
        if (own != null) return own;
        for (List<Mark> list : marks.values()) {
            Neighbours found = neighbours(list, level, pos);
            if (found != null) return found;
        }
        return null;
    }

    private static @Nullable Neighbours neighbours(@Nullable List<Mark> list, ServerLevel level, BlockPos pos) {
        if (list == null) return null;
        Identifier dimension = level.dimension().identifier();
        for (int i = 0; i < list.size(); i++) {
            Mark mark = list.get(i);
            if (!mark.dimension().equals(dimension) || !mark.pos().equals(pos)) continue;
            Mark next = i + 1 < list.size() && list.get(i + 1).trail() == mark.trail() ? list.get(i + 1) : null;
            Mark back = i > 0 && list.get(i - 1).trail() == mark.trail() ? list.get(i - 1) : null;
            return new Neighbours(next, back, trailLength(list, mark.trail()));
        }
        return null;
    }

    private static int trailLength(List<Mark> list, int trail) {
        int n = 0;
        for (Mark mark : list) if (mark.trail() == trail) n++;
        return n;
    }

    /** Marks a player has set, for tests. */
    public int count(UUID player) {
        List<Mark> list = marks.get(player);
        return list == null ? 0 : list.size();
    }
}
