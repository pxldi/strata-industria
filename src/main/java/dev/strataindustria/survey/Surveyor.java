package dev.strataindustria.survey;

import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.VeinCells;
import dev.strataindustria.geology.VeinType;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Reads survey notes against the vein cells (structures spec 5.3). Only cell lists are read, never
 * chunks, so the answer is the same for a given seed and spot and costs no chunk loading.
 */
public final class Surveyor {
    /** How far the writer of the notes had walked, in blocks. */
    public static final int RADIUS = 768;
    /**
     * Veins this close to where the notes are first read are the camp's own; the writer was already
     * standing on them and would not have bothered to write them down.
     */
    public static final int OWN_VEIN = 48;
    /** Handwritten lines per mineral. */
    public static final int HANDS = 6;

    private Surveyor() {}

    public static Optional<SurveyNotes.Entry> resolve(ServerLevel level, BlockPos from, SurveyNotes notes, RandomSource random) {
        GeologyContext geology = GeologyContext.get(level);
        if (geology == null) return Optional.empty();
        for (String target : notes.targets()) {
            OreMineral mineral = mineral(target);
            if (mineral == null) continue; // a mineral from a later tier that this build does not have yet
            VeinCells.Vein vein = nearest(geology.veins(), from, mineral);
            if (vein != null) return Optional.of(entry(geology, vein, mineral, random));
        }
        return Optional.empty();
    }

    public static OreMineral mineral(String id) {
        for (OreMineral m : OreMineral.values()) {
            if (m.id().equals(id)) return m;
        }
        return null;
    }

    private static VeinCells.Vein nearest(VeinCells veins, BlockPos from, OreMineral mineral) {
        int cx = Math.floorDiv(from.getX(), VeinCells.CELL_SIZE), cz = Math.floorDiv(from.getZ(), VeinCells.CELL_SIZE);
        int rings = RADIUS / VeinCells.CELL_SIZE;
        VeinCells.Vein best = null;
        double bestDist = Double.MAX_VALUE;
        for (int ring = 0; ring <= rings; ring++) {
            // Anything in a further ring is at least (ring - 1) cells away; stop once that beats the best.
            if (best != null && (ring - 1) * (double) VeinCells.CELL_SIZE > Math.sqrt(bestDist)) break;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    for (VeinCells.Vein vein : veins.cell(cx + dx, cz + dz)) {
                        if (!holds(vein.type(), mineral)) continue;
                        double ddx = vein.x() - from.getX(), ddz = vein.z() - from.getZ();
                        double dist = ddx * ddx + ddz * ddz;
                        if (dist < OWN_VEIN * OWN_VEIN || dist > (double) RADIUS * RADIUS) continue;
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = vein;
                        }
                    }
                }
            }
        }
        return best;
    }

    static boolean holds(VeinType type, OreMineral mineral) {
        for (VeinType.MineralWeight m : type.minerals()) {
            if (m.mineral() == mineral) return true;
        }
        return false;
    }

    private static SurveyNotes.Entry entry(GeologyContext geology, VeinCells.Vein vein, OreMineral mineral, RandomSource random) {
        int depth = Math.max(0, vein.surfaceY() - (vein.y() + vein.radiusV()));
        String host = geology.sampler().rockAt(vein.x(), vein.y(), vein.z(), vein.surfaceY()).id();
        double blocks = vein.estimatedOreBlocks();
        int size = blocks < 150 ? 0 : blocks <= 400 ? 1 : 2;
        return new SurveyNotes.Entry(mineral.id(), new BlockPos(vein.x(), vein.y(), vein.z()), vein.radiusH(), depth, host,
                size, random.nextInt(HANDS));
    }

    /** Whether a player at {@code pos} is standing at the deposit (structures spec 5.4). */
    public static boolean atDeposit(SurveyNotes.Entry entry, BlockPos pos) {
        double dx = pos.getX() - entry.pos().getX(), dz = pos.getZ() - entry.pos().getZ();
        if (dx * dx + dz * dz > (double) entry.radius() * entry.radius()) return false;
        // Anywhere from just above the surface over the vein down to its bottom counts.
        int top = entry.pos().getY() + entry.depth() + entry.radius() + 8;
        int bottom = entry.pos().getY() - entry.radius() - 4;
        return pos.getY() <= top && pos.getY() >= bottom;
    }
}
