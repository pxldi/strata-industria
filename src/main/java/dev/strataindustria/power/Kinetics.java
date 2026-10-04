package dev.strataindustria.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Shared helpers for kinetic blocks. */
public final class Kinetics {
    private Kinetics() {}

    /** Sneak + empty hand on any kinetic block shows the network line (spec 7.1). Returns whether it did. */
    public static boolean report(Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof Kinetic kinetic)) return false;
        if (!level.isClientSide()) {
            var line = kinetic.kinetic().report();
            if (kinetic instanceof KineticSource source && source.idleReason() != null) {
                line = source.idleReason().copy().append(" · ").append(line);
            }
            player.sendOverlayMessage(line);
        }
        return true;
    }

    /** Rotation angle in degrees for drawing, the same for every block of a network at this moment. */
    public static float angle(float rpm, long gameTime, float partialTick) {
        // One RPM is 360 degrees per 1200 ticks.
        return (float) (((gameTime % 24000L) + partialTick) * rpm * 0.3) % 360.0f;
    }
}
