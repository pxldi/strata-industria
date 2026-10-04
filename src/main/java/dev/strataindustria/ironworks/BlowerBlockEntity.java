package dev.strataindustria.ironworks;

import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The blower (tier 4 spec 11.1 and 11.6): 8 SU per RPM, and it blows once it turns at 16 RPM or more.
 * One blower is one unit of air blast.
 */
public class BlowerBlockEntity extends KineticBlockEntity implements KineticConsumer, AirBlast {
    public static final int IMPACT = 8, MIN_SPEED = 16;

    public BlowerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.BLOWER.get(), pos, state);
    }

    public boolean blowing() {
        return kinetic().rpm() >= MIN_SPEED;
    }

    @Override
    public int airOut(Direction out) {
        return blowing() && out == getBlockState().getValue(BlowerBlock.FACING) ? 1 : 0;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    /** The fan's whoosh, higher the faster it turns (spec 21.7). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BlowerBlockEntity blower) {
        if (!blower.blowing() || Math.floorMod(level.getGameTime() + pos.asLong(), 40) != 0) return;
        float rpm = blower.kinetic().rpm();
        float pitch = rpm >= 128 ? 1.4f : rpm >= 32 ? 1.2f : 1.0f;
        level.playSound(null, pos, Tier4Sounds.BLOWER_RUN.get(), SoundSource.BLOCKS, 0.4f, pitch * (0.95f + level.getRandom().nextFloat() * 0.1f));
    }
}
