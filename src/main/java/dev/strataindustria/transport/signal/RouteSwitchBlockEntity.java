package dev.strataindustria.transport.signal;

import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.StopData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a route switch remembers: the stops it sends down the branch. Every other tick it looks for the lead that is
 * about to reach it and throws itself for that lead, unless a vehicle is already on the switch.
 */
public class RouteSwitchBlockEntity extends BlockEntity {
    /** Blocks out a lead is noticed. */
    private static final double SIGHT = 8.0;
    /** Blocks from the middle of the switch within which any vehicle holds the blades where they are, and the wider ring that holds them for another train's cars. */
    private static final double ON_SWITCH = 1.5;
    private static final double NEAR_SWITCH = 2.8;
    private static final int LOOK_EVERY = 2;

    private List<String> stops = new ArrayList<>();

    public RouteSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(SignalRegistry.ROUTE_SWITCH_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- the list

    /** The stop names the branch is for, at most {@link TimetableStops#MAX_STOPS}. */
    public List<String> stops() {
        return List.copyOf(stops);
    }

    public void setStops(List<String> names) {
        List<String> clean = new ArrayList<>();
        for (String name : names) {
            String text = StopData.cleanName(name);
            if (!text.isBlank() && clean.size() < TimetableStops.MAX_STOPS) clean.add(text);
        }
        stops = clean;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Whether a lead bound for the stop it is running to wants the branch. */
    public boolean sendsToBranch(MineTubEntity lead) {
        Timetable table = lead.timetable();
        if (table == null) return false;
        for (String name : stops) if (table.target().names(name)) return true;
        return false;
    }

    // ---------------------------------------------------------------- setting itself

    public void serverTick(ServerLevel server) {
        if (server.getGameTime() % LOOK_EVERY != 0) return;
        BlockState state = getBlockState();
        Direction facing = state.getValue(RouteSwitchBlock.FACING);
        Direction trunk = facing.getOpposite();
        Direction branch = RouteSwitchBlock.branchOf(server, worldPosition, facing);
        RailShape straight = RouteSwitchBlock.straight(facing);
        RailShape curve = branch == null ? straight : RouteSwitchBlock.joining(trunk, branch);
        RailShape now = state.getValue(RouteSwitchBlock.SHAPE);
        Vec3 centre = Vec3.atCenterOf(worldPosition);

        AABB sight = new AABB(worldPosition).inflate(SIGHT, 2, SIGHT);
        List<MineTubEntity> near = server.getEntitiesOfClass(MineTubEntity.class, sight);
        MineTubEntity coming = null;
        Direction from = null;
        double best = Double.MAX_VALUE;
        for (MineTubEntity vehicle : near) {
            if (!vehicle.isLead() || vehicle.holding()) continue;
            double dx = vehicle.getX() - centre.x, dz = vehicle.getZ() - centre.z;
            double distance = Math.hypot(dx, dz);
            Direction leg = Direction.getApproximateNearest(dx, 0, dz);
            if (leg != trunk && leg != facing && leg != branch) continue;
            Vec3 motion = vehicle.getDeltaMovement();
            if (motion.x * dx + motion.z * dz >= -0.002) continue;
            if (distance < best) {
                best = distance;
                coming = vehicle;
                from = leg;
            }
        }
        // Never move the blades under a vehicle: nothing on the switch, and no carriage of another train beside it.
        for (MineTubEntity vehicle : near) {
            double distance = Math.hypot(vehicle.getX() - centre.x, vehicle.getZ() - centre.z);
            if (distance < ON_SWITCH) return;
            if (distance < NEAR_SWITCH && (coming == null || !coming.consist().contains(vehicle))) return;
        }
        RailShape target;
        if (coming == null || from == null) {
            // Nothing coming: put right a shape a neighbouring rail has bent.
            if (now == straight || now == curve) return;
            target = straight;
        } else if (from == trunk) {
            target = branch != null && sendsToBranch(coming) ? curve : straight;
        } else if (from == facing) {
            target = straight;
        } else {
            target = curve;
        }
        if (target == now) return;
        throwTo(server, state, target, target != straight);
    }

    private void throwTo(ServerLevel server, BlockState state, RailShape shape, boolean diverging) {
        server.setBlock(worldPosition, state.setValue(RouteSwitchBlock.SHAPE, shape), Block.UPDATE_ALL);
        server.playSound(null, worldPosition, SignalRegistry.SWITCH_THROW.get(), SoundSource.BLOCKS, 0.7f, diverging ? 0.85f : 1.1f);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5, 5, 0.25, 0.05, 0.25, 0.03);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stops = new ArrayList<>();
        for (String name : input.listOrEmpty("stops", com.mojang.serialization.Codec.STRING)) stops.add(name);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var list = output.list("stops", com.mojang.serialization.Codec.STRING);
        for (String name : stops) list.add(name);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
