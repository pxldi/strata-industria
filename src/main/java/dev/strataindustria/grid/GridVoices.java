package dev.strataindustria.grid;

import dev.strataindustria.power.ElectricNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

/**
 * Mains hum (uniqueness 2.1 and 7.1). Pole insulators and one cable in ten hum while their network works, higher
 * and louder as the load rises; a network at its limit buzzes instead. Each network spreads its hum points over a
 * 40 tick cycle, so a long line murmurs rather than pulses, and only points near a player make a sound.
 */
public final class GridVoices {
    /** Load share below which a line is quiet. */
    public static final double HUM_MIN = 0.05;
    /** Blocks within which a hum is played at all. */
    private static final double HEARING = 24.0;

    private GridVoices() {}

    /** Hum volume and pitch for a load share: a faint low drone at idle, louder and higher at the limit. */
    public static float volume(double load) {
        return (float) (0.12 + 0.35 * Math.min(1.0, load));
    }

    public static float pitch(double load) {
        return (float) (0.8 + 0.6 * Math.min(1.0, load));
    }

    /** Whether a network makes any noise: it carries a load, or it is at its limit. */
    public static boolean audible(ElectricNetwork network) {
        return network.load() >= HUM_MIN || network.strained();
    }

    /** One tick of hum for one network. */
    public static void hum(ServerLevel level, ElectricNetwork network) {
        if (!audible(network)) return;
        for (BlockPos pos : network.humPoints(level.getGameTime())) {
            if (!level.isLoaded(pos) || level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HEARING, false) == null) continue;
            if (network.strained()) {
                level.playSound(null, pos, GridSounds.MAINS_BUZZ.get(), SoundSource.BLOCKS, 0.4f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            } else {
                level.playSound(null, pos, GridSounds.MAINS_HUM.get(), SoundSource.BLOCKS, volume(network.load()), pitch(network.load()));
            }
        }
    }

    /** A transformer buzzes harder the nearer it runs to its limit (uniqueness 2.1). {@code load} is the share of its 128 J/t. */
    public static void transformer(ServerLevel level, BlockPos pos, double load) {
        if (load < 0.25 || (level.getGameTime() + pos.asLong()) % 30 != 0) return;
        if (load >= 0.9) {
            level.playSound(null, pos, GridSounds.MAINS_BUZZ.get(), SoundSource.BLOCKS, 0.5f, 0.85f + level.getRandom().nextFloat() * 0.1f);
        } else {
            level.playSound(null, pos, GridSounds.MAINS_HUM.get(), SoundSource.BLOCKS, volume(load) + 0.1f, pitch(load) - 0.1f);
        }
    }
}
