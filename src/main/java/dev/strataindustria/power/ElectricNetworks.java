package dev.strataindustria.power;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Electric networks per level (tier 5 spec 6.6). Like the kinetic networks, a network is rebuilt only
 * when one of its blocks is placed, broken, loaded or unloaded: those mark a position dirty, and at the
 * end of the level tick each dirty position is flood-filled once. Then every network ticks, which is
 * arithmetic only.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class ElectricNetworks {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> DIRTY = new HashMap<>();
    private static final Map<ResourceKey<Level>, Grid> GRIDS = new HashMap<>();

    /** The networks of one level, and which network each block belongs to. */
    private static final class Grid {
        final Map<BlockPos, ElectricNetwork> byPos = new HashMap<>();
        final Set<ElectricNetwork> networks = new LinkedHashSet<>();

        void drop(ElectricNetwork network) {
            if (!networks.remove(network)) return;
            for (BlockPos pos : network.members()) byPos.remove(pos, network);
        }
    }

    private ElectricNetworks() {}

    /** Rebuild the network through {@code pos} (or its neighbours, if {@code pos} is gone) at the end of this tick. */
    public static void markDirty(@Nullable Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        Set<BlockPos> dirty = DIRTY.computeIfAbsent(server.dimension(), k -> new HashSet<>());
        dirty.add(pos.immutable());
        for (Direction side : Direction.values()) dirty.add(pos.relative(side));
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        rebuildDirty(level);
        Grid grid = GRIDS.get(level.dimension());
        if (grid == null) return;
        for (ElectricNetwork network : grid.networks) {
            if (network.tick() && network.overvoltageCable() != null) {
                level.playSound(null, network.overvoltageCable(), Tier5Sounds.ELECTRIC_OVERVOLTAGE.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            }
            BlockPos fault = network.overvoltageCable();
            if (fault != null && level.getGameTime() % 10 == 0) sparks(level, fault);
        }
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DIRTY.remove(level.dimension());
            GRIDS.remove(level.dimension());
        }
    }

    /** Sparks and a crackle at a cable that is carrying more than it is rated for (spec 6.2 and 23.7). */
    private static void sparks(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
        if (level.getRandom().nextInt(3) == 0) {
            level.playSound(null, pos, Tier5Sounds.ELECTRIC_SPARK.get(), SoundSource.BLOCKS, 0.3f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
    }

    /** Rebuild the network through {@code pos} right away, for game tests. */
    public static @Nullable ElectricNetwork rebuildNow(ServerLevel level, BlockPos pos) {
        markDirty(level, pos);
        rebuildDirty(level);
        return networkAt(level, pos);
    }

    public static @Nullable ElectricNetwork networkAt(Level level, BlockPos pos) {
        Grid grid = GRIDS.get(level.dimension());
        return grid == null ? null : grid.byPos.get(pos);
    }

    /** What the network did for the device at {@code pos} on its last tick. */
    public static ElectricNetwork.Report report(Level level, BlockPos pos) {
        ElectricNetwork network = networkAt(level, pos);
        return network == null ? ElectricNetwork.Report.NONE : network.report(pos);
    }

    private static void rebuildDirty(ServerLevel level) {
        Set<BlockPos> dirty = DIRTY.remove(level.dimension());
        if (dirty == null) return;
        Grid grid = GRIDS.computeIfAbsent(level.dimension(), k -> new Grid());
        // Every block of a network touched by a change is rebuilt, so no part of it is left without a network.
        Set<BlockPos> seeds = new LinkedHashSet<>(dirty);
        for (BlockPos pos : dirty) {
            ElectricNetwork old = grid.byPos.get(pos);
            if (old != null) {
                seeds.addAll(old.members());
                grid.drop(old);
            }
        }
        Set<BlockPos> done = new HashSet<>();
        for (BlockPos pos : seeds) {
            if (!done.contains(pos)) build(level, grid, pos, done);
        }
    }

    private static void build(ServerLevel level, Grid grid, BlockPos start, Set<BlockPos> done) {
        if (!level.isLoaded(start) || !(level.getBlockEntity(start) instanceof ElectricNode first)) return;
        ElectricNetwork existing = grid.byPos.get(start);
        if (existing != null) grid.drop(existing);
        int max = Config.ELECTRIC_MAX_NETWORK.get();
        Map<BlockPos, ElectricNode> nodes = new LinkedHashMap<>();
        Map<BlockPos, List<BlockPos>> edges = new HashMap<>();
        nodes.put(start, first);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        boolean tooLarge = false;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            ElectricNode node = nodes.get(pos);
            for (Direction side : Direction.values()) {
                if (!node.connectsElectric(side)) continue;
                BlockPos next = pos.relative(side);
                // A network split by an unloaded chunk runs each loaded part on its own (spec 6.6).
                if (!level.isLoaded(next)) continue;
                if (!(level.getBlockEntity(next) instanceof ElectricNode neighbour) || !neighbour.connectsElectric(side.getOpposite())) continue;
                edges.computeIfAbsent(pos, k -> new ArrayList<>()).add(next.immutable());
                if (nodes.containsKey(next)) continue;
                if (nodes.size() >= max) {
                    tooLarge = true;
                    continue;
                }
                nodes.put(next.immutable(), neighbour);
                queue.add(next.immutable());
            }
        }
        done.addAll(nodes.keySet());
        for (BlockPos pos : nodes.keySet()) {
            ElectricNetwork old = grid.byPos.get(pos);
            if (old != null) grid.drop(old);
        }
        ElectricNetwork network = new ElectricNetwork(nodes, edges, tooLarge);
        grid.networks.add(network);
        for (BlockPos pos : nodes.keySet()) grid.byPos.put(pos, network);
    }

    // ---------------------------------------------------------------- diagnostics (spec 6.5)

    /** Sneak + empty hand on any electric block shows the network line in the action bar. Returns whether it did. */
    public static boolean report(Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof ElectricNode)) return false;
        if (!level.isClientSide()) player.sendOverlayMessage(line(level, pos));
        return true;
    }

    /** The diagnostics line for the block at {@code pos}: the network for a cable, the device's own numbers otherwise. */
    public static Component line(Level level, BlockPos pos) {
        ElectricNetwork network = networkAt(level, pos);
        String prefix = StrataIndustria.MOD_ID + ".electric.";
        if (network == null) return Component.translatable(ElectricStatus.NO_SOURCE.key());
        if (network.tooLarge()) return Component.translatable(ElectricStatus.TOO_LARGE.key()).withStyle(ChatFormatting.RED);
        BlockPos fault = network.overvoltageCable();
        if (fault != null) {
            return Component.translatable(ElectricStatus.CABLE_OVERVOLTAGE.key(), network.weakestCable().label(), fault.getX(), fault.getY(), fault.getZ())
                    .withStyle(ChatFormatting.RED);
        }
        if (network.tier() == null) return Component.translatable(ElectricStatus.NO_SOURCE.key());
        MutableComponent line;
        if (level.getBlockEntity(pos) instanceof ElectricDevice device && !(device instanceof ElectricConductor)) {
            line = deviceLine(network, pos, device, prefix);
        } else {
            line = Component.translatable(prefix + "network", network.tier().label(), power(network.delivered()), power(network.supply()),
                    percent(network.worstLoss()), joules(network.stored()), joules(network.storageCapacity()));
        }
        if (network.capped() && network.weakestCable() != null) {
            line.append(Component.literal(" · ")).append(Component.translatable(prefix + "limited_by", network.weakestCable().label(),
                    network.weakestCable().cableCapacity()).withStyle(ChatFormatting.GOLD));
        }
        return line;
    }

    private static MutableComponent deviceLine(ElectricNetwork network, BlockPos pos, ElectricDevice device, String prefix) {
        ElectricNetwork.Report report = network.report(pos);
        return switch (report.status()) {
            case OVERVOLTAGE -> Component.translatable(ElectricStatus.OVERVOLTAGE.key(), device.tier().label(), network.tier().label())
                    .withStyle(ChatFormatting.RED);
            case TOO_FAR -> Component.translatable(ElectricStatus.TOO_FAR.key(), percent(report.loss())).withStyle(ChatFormatting.RED);
            default -> {
                if (device instanceof ElectricStorage storage) {
                    yield Component.translatable(prefix + "storage", joules(storage.stored()), joules(storage.capacity()),
                            power(Math.abs(report.drawn())), Component.translatable(report.drawn() > 0 ? prefix + "charging"
                                    : report.drawn() < 0 ? prefix + "discharging" : prefix + "holding"));
                }
                if (device instanceof ElectricConsumer) {
                    yield Component.translatable(prefix + "consumer", power(report.requested()), power(report.drawn()), percent(report.loss()),
                            Component.translatable(report.status().key(), percent(report.fraction())));
                }
                yield Component.translatable(prefix + "source", power(report.drawn()), Component.translatable(report.status() == ElectricStatus.RUNNING
                        ? prefix + "status.running" : prefix + "no_demand"));
            }
        };
    }

    /** "16.8" for small numbers, "128" from 100 up. */
    public static String power(double perTick) {
        return perTick >= 100 || Math.abs(perTick - Math.round(perTick)) < 0.05 ? String.valueOf(Math.round(perTick))
                : String.format(Locale.ROOT, "%.1f", perTick);
    }

    /** Whole joules with thin groups, "41 000". */
    public static String joules(double amount) {
        return String.format(Locale.ROOT, "%,d", Math.round(amount)).replace(',', ' ');
    }

    public static String percent(double fraction) {
        return String.valueOf(Math.round(fraction * 100));
    }
}
