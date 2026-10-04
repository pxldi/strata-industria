package dev.strataindustria.transport.rail;

import dev.strataindustria.fluid.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Pours the tower's water into a locomotive (outposts spec 7.4): a standing engine within two blocks of the spout, in
 * reach of the track, takes 400 mB a tick from the tank column the spout stands on. The pour is a stream of drops
 * from the arm to the filler, a sound whose pitch climbs as the boiler fills, and a clunk when it is full.
 */
public class WaterTowerSpoutBlockEntity extends BlockEntity {
    public static final int RATE = 400;
    /** Blocks sideways from the spout an engine may stand, and blocks down to the track it may stand on. */
    public static final double REACH = 2.5;
    public static final double DROP = 7.0;
    private static final int LOOK_EVERY = 5;
    private static final int POUR_SOUND_EVERY = 6;

    private int pouringFor;
    private int soundClock;
    private boolean wasFull;

    public WaterTowerSpoutBlockEntity(BlockPos pos, BlockState state) {
        super(RailwayRegistry.WATER_TOWER_SPOUT_ENTITY.get(), pos, state);
    }

    /** The tank the spout stands on, or null. */
    public @Nullable FluidTankBlockEntity tank() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof FluidTankBlockEntity tank ? tank : null;
    }

    /** The standing engine in reach that still has room for water. */
    public @Nullable SteamLocomotiveEntity thirsty(ServerLevel server) {
        AABB box = new AABB(worldPosition).inflate(REACH, 0, REACH).expandTowards(0, -DROP, 0);
        SteamLocomotiveEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (SteamLocomotiveEntity loco : server.getEntitiesOfClass(SteamLocomotiveEntity.class, box, SteamLocomotiveEntity::holding)) {
            double distance = loco.distanceToSqr(worldPosition.getX() + 0.5, loco.getY(), worldPosition.getZ() + 0.5);
            if (distance < bestDistance && loco.water() < SteamLocomotiveEntity.WATER_CAPACITY - 1) {
                best = loco;
                bestDistance = distance;
            }
        }
        return best;
    }

    public void serverTick(ServerLevel server, BlockPos pos, BlockState state) {
        if (pouringFor > 0) pouringFor--;
        if (pouringFor == 0 && !((server.getGameTime() + pos.asLong()) % LOOK_EVERY == 0)) return;
        SteamLocomotiveEntity loco = thirsty(server);
        FluidTankBlockEntity tank = tank();
        int give = 0;
        if (loco != null && tank != null) {
            int room = (int) Math.floor(SteamLocomotiveEntity.WATER_CAPACITY - loco.water());
            if (FluidTankBlockEntity.fluidOf(tank.group()).isSame(Fluids.WATER)) {
                give = tank.drain(Math.min(RATE, room), true);
                if (give > 0) {
                    tank.drain(give, false);
                    loco.takeWater(give);
                }
            }
        }
        if (give > 0) {
            pouringFor = 8;
            wasFull = false;
            swing(server, pos, state, loco, true);
            stream(server, pos, state, loco);
            if (soundClock-- <= 0) {
                soundClock = POUR_SOUND_EVERY;
                float filled = loco.water() / SteamLocomotiveEntity.WATER_CAPACITY;
                server.playSound(null, pos, RailwayRegistry.WATER_POUR.get(), SoundSource.BLOCKS, 0.8f, 0.7f + 0.6f * filled);
            }
            return;
        }
        if (pouringFor == 0 && state.getValue(WaterTowerSpoutBlock.POURING)) {
            swing(server, pos, state, null, false);
            if (!wasFull) {
                wasFull = true;
                server.playSound(null, pos, RailwayRegistry.BUFFER_CLANG.get(), SoundSource.BLOCKS, 0.5f, 1.6f);
            }
        }
    }

    /** Turns the arm toward the engine, or back in. */
    private void swing(ServerLevel server, BlockPos pos, BlockState state, @Nullable SteamLocomotiveEntity loco, boolean out) {
        Direction face = state.getValue(WaterTowerSpoutBlock.FACING);
        if (loco != null) {
            double dx = loco.getX() - (pos.getX() + 0.5), dz = loco.getZ() - (pos.getZ() + 0.5);
            face = Direction.getApproximateNearest(dx, 0, dz);
        }
        if (state.getValue(WaterTowerSpoutBlock.POURING) != out || state.getValue(WaterTowerSpoutBlock.FACING) != face) {
            server.setBlock(pos, state.setValue(WaterTowerSpoutBlock.POURING, out).setValue(WaterTowerSpoutBlock.FACING, face), Block.UPDATE_ALL);
            if (out) server.playSound(null, pos, RailRegistry.TUB_STOP_BRAKE.get(), SoundSource.BLOCKS, 0.6f, 0.7f);
        }
    }

    /** A stream of drops from the tip of the arm down onto the engine's filler. */
    private void stream(ServerLevel server, BlockPos pos, BlockState state, SteamLocomotiveEntity loco) {
        Direction face = state.getValue(WaterTowerSpoutBlock.FACING);
        Vec3 tip = new Vec3(pos.getX() + 0.5 + face.getStepX() * 0.45, pos.getY() + 0.55, pos.getZ() + 0.5 + face.getStepZ() * 0.45);
        Vec3 target = new Vec3(loco.getX(), loco.getY() + 0.95, loco.getZ());
        for (int i = 0; i < 4; i++) {
            double t = i / 3.0;
            double x = Mth.lerp(t, tip.x, target.x), z = Mth.lerp(t, tip.z, target.z);
            double y = Mth.lerp(t * t, tip.y, target.y);
            server.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 1, 0.03, 0.0, 0.03, 0.0);
        }
        server.sendParticles(ParticleTypes.SPLASH, target.x, target.y, target.z, 2, 0.12, 0.02, 0.12, 0.05);
    }

}
