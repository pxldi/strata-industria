package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** The line a trail mark gives when it is touched bare-handed: where the next and the previous mark lie. */
public final class TrailMessages {
    private static final String[] COMPASS = {"east", "south_east", "south", "south_west", "west", "north_west", "north", "north_east"};

    private TrailMessages() {}

    /** Compass name, 8-way, with north at negative z. */
    public static String direction(BlockPos from, BlockPos to) {
        double angle = Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX());
        int sector = Math.floorMod((int) Math.round(angle / (Math.PI / 4)), 8);
        return COMPASS[sector];
    }

    public static int distance(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        return (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
    }

    private static Component leg(String key, BlockPos from, BlockPos to) {
        return Component.translatable(StrataIndustria.MOD_ID + ".trail." + key, distance(from, to),
                Component.translatable(StrataIndustria.MOD_ID + ".trail.dir." + direction(from, to)));
    }

    /** The overlay line for a mark, or null when nothing is known about it. */
    public static Component describe(ServerLevel level, ServerPlayer player, BlockPos pos) {
        TrailMarks.Neighbours near = TrailMarks.get(level.getServer()).neighbours(player.getUUID(), level, pos);
        if (near == null || (near.next() == null && near.back() == null)) {
            return Component.translatable(StrataIndustria.MOD_ID + ".trail.alone");
        }
        MutableComponent line = near.next() == null
                ? Component.translatable(StrataIndustria.MOD_ID + ".trail.end")
                : leg("next", pos, near.next().pos()).copy();
        if (near.back() != null) line.append(" ").append(leg("back", pos, near.back().pos()));
        return line;
    }
}
