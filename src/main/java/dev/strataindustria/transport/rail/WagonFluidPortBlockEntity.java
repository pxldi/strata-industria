package dev.strataindustria.transport.rail;

import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The wagon fluid port's work (outposts spec 7.2): it finds the station track beside it, the tank wagon standing on
 * that stop, and moves up to {@link #RATE} mB a tick between them and the pipes. A filling port is a pipe port that
 * hands what it is given straight to the wagon; an emptying one pushes the wagon's fluid into its pipes.
 */
public class WagonFluidPortBlockEntity extends BlockEntity implements FluidPort {
    public static final int RATE = 200;
    /** Slower than this and a wagon counts as standing at the stop. */
    private static final double STANDING = 0.03;
    private static final int NETWORK_REFRESH = 20;

    private long budgetTick = -1;
    private int budget;
    private int age;
    private int quiet = 100;
    private FluidPipes.Network[] networks = new FluidPipes.Network[6];
    private @Nullable Direction trackSide;

    public WagonFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(RailwayRegistry.WAGON_FLUID_PORT_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- the wagon at the stop

    /** The side a station track lies on, or null when there is none. */
    public @Nullable Direction trackSide() {
        if (level == null) return null;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(worldPosition.relative(side)).getBlock() instanceof StationTrackBlock) return side;
        }
        return null;
    }

    /** The tank wagon standing on the station track beside the port, or null. */
    public @Nullable TankWagonEntity wagon() {
        if (level == null) return null;
        Direction side = trackSide();
        if (side == null) return null;
        BlockPos track = worldPosition.relative(side);
        List<TankWagonEntity> wagons = level.getEntitiesOfClass(TankWagonEntity.class, new AABB(track).inflate(0.2, 0.5, 0.2),
                wagon -> wagon.getDeltaMovement().horizontalDistance() < STANDING);
        return wagons.isEmpty() ? null : wagons.get(0);
    }

    private boolean filling() {
        return getBlockState().getValue(WagonFluidPortBlock.MODE) == WagonFluidPortBlock.Mode.LOAD;
    }

    // ---------------------------------------------------------------- pipes in

    @Override
    public boolean connectsFluid(Direction side) {
        return side != trackSide();
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (level == null || !filling() || level.isClientSide()) return 0;
        TankWagonEntity wagon = wagon();
        if (wagon == null) return 0;
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            budget = RATE;
        }
        int moved = wagon.fill(fluid, Math.min(amount, budget), simulate);
        if (!simulate && moved > 0) {
            budget -= moved;
            flowed((ServerLevel) level, wagon);
        }
        return moved;
    }

    // ---------------------------------------------------------------- pipes out

    public void serverTick(ServerLevel server, BlockState state) {
        age++;
        quiet++;
        if (filling()) return;
        TankWagonEntity wagon = wagon();
        if (wagon == null || wagon.amount() <= 0) return;
        Fluid fluid = wagon.fluid();
        Direction track = trackSide();
        int left = Math.min(RATE, wagon.amount());
        for (Direction side : Direction.values()) {
            if (side == track || left <= 0) continue;
            FluidPipes.Network network = networks[side.ordinal()];
            if (network == null || age % NETWORK_REFRESH == 0) {
                network = FluidPipes.find(server, worldPosition, side);
                networks[side.ordinal()] = network;
            }
            if (network.isEmpty()) continue;
            FluidPipes.Push push = FluidPipes.push(server, network, fluid, left, 20f, 0f);
            if (push.moved() > 0) {
                wagon.drain(push.moved(), false);
                left -= push.moved();
                flowed(server, wagon);
            }
        }
    }

    /** A little splashing at the hatch and a gurgle every half second while fluid is moving. */
    private void flowed(ServerLevel server, TankWagonEntity wagon) {
        if (quiet < 10) return;
        quiet = 0;
        server.playSound(null, wagon.getX(), wagon.getY(), wagon.getZ(), RailwayRegistry.PORT_FLOW.get(), SoundSource.BLOCKS, 0.6f,
                0.9f + 0.2f * server.getRandom().nextFloat());
        server.sendParticles(ParticleTypes.SPLASH, wagon.getX(), wagon.getY() + 0.9, wagon.getZ(), 4, 0.2, 0.05, 0.2, 0.0);
    }
}
