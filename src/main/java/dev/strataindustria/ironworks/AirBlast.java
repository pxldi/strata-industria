package dev.strataindustria.ironworks;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Something that blows air through a face: a blower, later a blowing engine (tier 4 spec 11.6 and 10.5). */
public interface AirBlast {
    /** How many blowers' worth of air goes out through {@code out} right now. */
    int airOut(Direction out);

    /** At most two blowers' worth of air counts on a furnace (spec 12.1 and 12.2). */
    int MAX_AIR = 2;

    /** Air blown into these tuyeres from outside, at most {@link #MAX_AIR}. */
    static int into(Level level, List<BlastFurnaceStructure.Opening> tuyeres) {
        int total = 0;
        for (BlastFurnaceStructure.Opening tuyere : tuyeres) {
            BlockPos outside = tuyere.pos().relative(tuyere.out());
            if (level.getBlockEntity(outside) instanceof AirBlast blast) total += blast.airOut(tuyere.out().getOpposite());
        }
        return Math.min(MAX_AIR, total);
    }
}
