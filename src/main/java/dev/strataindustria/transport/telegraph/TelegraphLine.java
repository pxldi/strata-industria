package dev.strataindustria.transport.telegraph;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.OverheadLine;
import dev.strataindustria.electric.PoleInsulatorBlockEntity;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.rail.TramwayRoutes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Stringing telegraph wire between pole insulators, and what a key does to the line (outposts spec 9.1). */
public final class TelegraphLine {
    private TelegraphLine() {}

    /** Why a wire cannot be strung between two insulators, as a translation key suffix, or null if it can. */
    public static @Nullable String problem(ServerLevel level, BlockPos a, BlockPos b) {
        if (a.equals(b)) return "same";
        if (!(level.getBlockEntity(a) instanceof PoleInsulatorBlockEntity) || !(level.getBlockEntity(b) instanceof PoleInsulatorBlockEntity)) return "not_insulator";
        TelegraphIndex index = TelegraphIndex.get(level);
        if (index.wired(a, b)) return "already";
        if (OverheadLine.length(a, b) > Config.TRANSPORT_TELEGRAPH_SPAN.get()) return "too_far";
        if (!index.roomAt(a) || !index.roomAt(b)) return "full";
        if (blocked(level, a, b)) return "blocked";
        return null;
    }

    /** Whether a solid block lies between the two wire clamps; the blocks the insulators stand in do not count. */
    public static boolean blocked(Level level, BlockPos a, BlockPos b) {
        Vec3 from = PoleInsulatorBlockEntity.wireTip(a, level.getBlockState(a)), to = PoleInsulatorBlockEntity.wireTip(b, level.getBlockState(b));
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, level, (l, pos) -> {
            if (pos.equals(a) || pos.equals(b)) return null;
            VoxelShape shape = l.getBlockState(pos).getCollisionShape(l, pos);
            return shape.isEmpty() ? null : shape.clip(from, to, pos);
        }, l -> null);
        return hit != null;
    }

    /** Strings the wire; {@link #problem} must have returned null. A thin line of sparks runs along it so the player sees it land. */
    public static void connect(ServerLevel level, BlockPos a, BlockPos b) {
        if (!(level.getBlockEntity(a) instanceof PoleInsulatorBlockEntity first) || !(level.getBlockEntity(b) instanceof PoleInsulatorBlockEntity second)) return;
        first.addWire(b);
        second.addWire(a);
        TelegraphIndex.get(level).addWire(a, b);
        Vec3 from = PoleInsulatorBlockEntity.wireTip(a, level.getBlockState(a)), to = PoleInsulatorBlockEntity.wireTip(b, level.getBlockState(b));
        int steps = Math.max(3, (int) (OverheadLine.length(a, b) * 1.5));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.WAX_OFF, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.playSound(null, b, TelegraphRegistry.WIRE_STRUNG.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
    }

    /** An insulator was broken: its wires part, the far ends are freed, the wire comes back, and the lines over it are cut. */
    public static void insulatorRemoved(ServerLevel level, BlockPos pos, List<BlockPos> listed) {
        TelegraphIndex index = TelegraphIndex.get(level);
        List<BlockPos> far = index.removeInsulator(pos);
        Set<BlockPos> ends = new HashSet<>(far);
        ends.addAll(listed);
        int wire = 0;
        for (BlockPos other : ends) {
            wire += TelegraphIndex.wireFor(pos, other);
            if (level.isLoaded(other) && level.getBlockEntity(other) instanceof PoleInsulatorBlockEntity farEnd) farEnd.removeWire(pos);
        }
        if (ends.isEmpty()) return;
        for (int left = wire; left > 0; left -= 64) {
            Block.popResource(level, pos, new ItemStack(TelegraphRegistry.WIRE.get(), Math.min(64, left)));
        }
        level.playSound(null, pos, TelegraphRegistry.WIRE_SNAP.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        RouteIndex.get(level).cut(level, pos);
    }

    /** A new insulator: devices standing near without a pole take it, each with a spark and a click. */
    public static void insulatorPlaced(ServerLevel level, BlockPos pos) {
        for (BlockPos device : TelegraphIndex.get(level).insulatorPlaced(level, pos)) dropConnected(level, device, pos);
    }

    /** A device hangs its drop wire on an insulator: a short run of sparks and a click. */
    static void dropConnected(ServerLevel level, BlockPos device, BlockPos insulator) {
        Vec3 from = Vec3.atCenterOf(device), to = PoleInsulatorBlockEntity.wireTip(insulator, level.getBlockState(insulator));
        int steps = Math.max(3, (int) (from.distanceTo(to) * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.WAX_OFF, p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0.0);
        }
        level.playSound(null, device, TelegraphRegistry.DROP_HUNG.get(), SoundSource.BLOCKS, 0.7f, 1.2f);
    }

    // ---------------------------------------------------------------- calls

    /** What a key press did: sounders on the line that heard it, and charters it reached for the first time or again. */
    public record Call(boolean attached, int sounders, int charters) {}

    /**
     * A key goes down. Every sounder on its network clacks (those in loaded chunks; a sounder nobody has loaded still
     * counts as reached), and a sounder in the area of another charter proves a telegraph link between the two.
     */
    public static Call send(ServerLevel level, BlockPos key, @Nullable ServerPlayer by) {
        TelegraphIndex index = TelegraphIndex.get(level);
        BlockPos drop = index.dropOf(key);
        if (drop == null || !(level.getBlockEntity(drop) instanceof PoleInsulatorBlockEntity)) {
            drop = index.attach(level, key, TelegraphIndex.Kind.KEY);
            if (drop == null) drop = index.dropOf(key);
            if (drop != null) dropConnected(level, key, drop);
        }
        if (drop == null) return new Call(false, 0, 0);
        Set<BlockPos> network = index.network(drop);
        RouteIndex routes = RouteIndex.get(level);
        Charter from = TramwayRoutes.stationCharter(routes, key);
        Set<UUID> reached = new HashSet<>();
        int sounders = 0;
        for (TelegraphIndex.Device device : index.onNetwork(network, TelegraphIndex.Kind.SOUNDER)) {
            BlockPos at = device.pos();
            if (level.isLoaded(at)) {
                BlockState state = level.getBlockState(at);
                if (!(state.getBlock() instanceof TelegraphSounderBlock)) {
                    index.unregister(at);
                    continue;
                }
                TelegraphSounderBlock.clack(level, at, state);
            }
            sounders++;
            Charter to = TramwayRoutes.stationCharter(routes, at);
            if (from == null || to == null || to.id().equals(from.id()) || !reached.add(to.id())) continue;
            List<BlockPos> route = new ArrayList<>();
            route.add(key.immutable());
            route.addAll(index.path(drop, device.drop().get()));
            route.add(at.immutable());
            RouteIndex.Proof proof = routes.prove(level, from.id(), to.id(), LinkKind.TELEGRAPH, route);
            if (proof.link() != null) {
                routes.traffic(level, proof.link().id());
                Journal.awardOwners(level, from, Journal.TELEGRAPH_CALL);
                Journal.awardOwners(level, to, Journal.TELEGRAPH_CALL);
                if (by != null) Journal.award(by, Journal.TELEGRAPH_CALL);
            } else if (proof.refusal() != null && by != null) {
                by.sendOverlayMessage(proof.refusal());
            }
        }
        return new Call(true, sounders, reached.size());
    }

    public static String key(String suffix) {
        return StrataIndustria.MOD_ID + ".telegraph." + suffix;
    }
}
