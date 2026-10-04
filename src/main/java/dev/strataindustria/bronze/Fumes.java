package dev.strataindustria.bronze;

import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Arsenical bronze fumes (uniqueness 4.3). The pour gives off white fumes. In the open, or with a fume hood
 * set directly over the pot, they simply rise and drift off. Indoors they fill the room, and anyone close by
 * gets a short bout of nausea.
 */
public final class Fumes {
    public static final int WHITE = 0xF0F0EA;
    /** Blocks around the pot that the fumes reach when they are not vented. */
    public static final double RANGE = 5.0;
    /** Ticks of nausea each time the fumes catch someone. */
    public static final int NAUSEA_TICKS = 160;
    /** Ticks the fumes keep rising after the metal has stopped flowing. */
    public static final int LINGER = 40;

    private Fumes() {}

    /** Whether the fumes over a pot at {@code pos} go straight out: under open sky, or under a hood. */
    public static boolean vented(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BronzeRegistry.FUME_HOOD.get()) || level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
    }

    /** One tick of fumes over the pot; {@code left} counts down the ticks the pour will keep fuming. */
    public static void tick(ServerLevel level, BlockPos pos, int left) {
        boolean vented = vented(level, pos);
        if (left % 3 == 0) {
            double spread = vented ? 0.05 : 0.3;
            level.sendParticles(new DustParticleOptions(WHITE, 1.4f), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    vented ? 2 : 4, spread, 0.1, spread, vented ? 0.03 : 0.015);
        }
        if (vented || left % 20 != 0) return;
        List<ServerPlayer> near = level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(RANGE));
        for (ServerPlayer player : near) {
            if (player.hasEffect(MobEffects.NAUSEA)) continue;
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS));
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".fumes.sting"));
        }
    }
}
