package dev.strataindustria.machine;

import dev.strataindustria.bloomery.BloomeryAir;
import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.forge.ForgeAir;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Each hand pump blows for 60 ticks; turned at 4 RPM or more it blows continuously (spec 8.3). Hand
 * pumping helps a forge only; a bloomery needs the steady draught of a powered bellows.
 */
public class BellowsBlockEntity extends KineticBlockEntity implements KineticConsumer, ForgeAir, BloomeryAir {
    public static final int IMPACT = 2, MIN_SPEED = 4;
    public static final int PUMP_TICKS = 60;
    /** How long the boards stay pressed together after a hand pump. */
    private static final int SQUEEZE_TICKS = 8;
    /** One full stroke takes this many ticks at 16 RPM. */
    private static final float STROKE_TICKS_AT_16 = 40.0f;

    private int pumpLeft;
    private int squeezeLeft;
    private float stroke;
    private boolean announced;

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BELLOWS.get(), pos, state);
    }

    public boolean powered() {
        return kinetic().rpm() >= MIN_SPEED;
    }

    private Direction facing() {
        return getBlockState().getValue(BellowsBlock.FACING);
    }

    public void pump(Player player) {
        if (!(level instanceof ServerLevel server) || squeezeLeft > 0) return;
        pumpLeft = PUMP_TICKS;
        squeezeLeft = SQUEEZE_TICKS;
        puff(server);
        player.causeFoodExhaustion(0.05f);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BellowsBlockEntity bellows) {
        ServerLevel server = (ServerLevel) level;
        if (bellows.pumpLeft > 0) bellows.pumpLeft--;
        boolean squeezed;
        if (bellows.powered()) {
            // The boards close for the first half of each stroke.
            bellows.stroke += bellows.kinetic().rpm() / 16.0f;
            if (bellows.stroke >= STROKE_TICKS_AT_16) bellows.stroke -= STROKE_TICKS_AT_16;
            squeezed = bellows.stroke < STROKE_TICKS_AT_16 / 2;
            if (squeezed && !state.getValue(BellowsBlock.COMPRESSED)) bellows.puff(server);
        } else {
            if (bellows.squeezeLeft > 0) bellows.squeezeLeft--;
            squeezed = bellows.squeezeLeft > 0;
        }
        if (state.getValue(BellowsBlock.COMPRESSED) != squeezed) {
            level.setBlock(pos, state.setValue(BellowsBlock.COMPRESSED, squeezed), Block.UPDATE_CLIENTS);
        }
        if (level.getGameTime() % 20 == 0) {
            boolean feeding = bellows.powered() && bellows.feedsFire();
            if (feeding && !bellows.announced) Journal.awardNear(level, pos, Journal.BELLOWS);
            bellows.announced = feeding;
        }
    }

    /** Whether the nozzle touches a forge or a refractory wall (of a bloomery, as far as a bellows can tell). */
    private boolean feedsFire() {
        if (level == null) return false;
        BlockPos target = worldPosition.relative(facing());
        return level.getBlockEntity(target) instanceof ForgeBlockEntity || level.getBlockState(target).is(ModTags.Blocks.REFRACTORY)
                && !(level.getBlockEntity(target) instanceof BloomeryBlockEntity);
    }

    private void puff(ServerLevel level) {
        Direction front = facing();
        double x = worldPosition.getX() + 0.5 + front.getStepX() * 0.6, y = worldPosition.getY() + 0.45,
                z = worldPosition.getZ() + 0.5 + front.getStepZ() * 0.6;
        level.sendParticles(ParticleTypes.POOF, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
        level.playSound(null, worldPosition, ModSounds.BELLOWS_PUMP.get(), SoundSource.BLOCKS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    @Override
    public boolean blowsInto(BlockPos target) {
        return worldPosition.relative(facing()).equals(target) && (powered() || pumpLeft > 0 && isForge(target));
    }

    private boolean isForge(BlockPos target) {
        return level != null && level.getBlockEntity(target) instanceof ForgeBlockEntity;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }
}
