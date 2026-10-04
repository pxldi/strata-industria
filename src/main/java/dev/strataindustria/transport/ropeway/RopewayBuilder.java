package dev.strataindustria.transport.ropeway;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jspecify.annotations.Nullable;

/**
 * Stringing a line (outposts spec 8.2): wire rope on the terminal, then on each tower, then on the return, the way
 * overhead spans are made. Each span is checked as it is made fast and costs one wire rope per 8 blocks, rounded up;
 * the line exists only when the return is reached, so a half-built one costs nothing to walk away from.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class RopewayBuilder {
    /** Most spans between the terminal and the return. */
    public static final int MAX_SPANS = 16;
    /** Blocks of line one wire rope pays for. */
    public static final int ROPE_BLOCKS = 8;
    /** Blocks of rise or fall allowed for each 2 across. */
    private static final double STEEP = 0.5;

    private static final class Pending {
        final ResourceKey<Level> dimension;
        final List<BlockPos> nodes = new ArrayList<>();
        double length;

        Pending(ResourceKey<Level> dimension, BlockPos terminal) {
            this.dimension = dimension;
            nodes.add(terminal);
        }
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private RopewayBuilder() {}

    @SubscribeEvent
    static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    /** Whether the player has a line they are stringing, for the tooltip and tests. */
    public static boolean stringing(Player player) {
        return PENDING.containsKey(player.getUUID());
    }

    /** Wire rope used on a block. */
    public static InteractionResult rope(Player player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean terminal = state.is(RopewayRegistry.TERMINAL.get()), tower = state.getBlock() instanceof RopewayTowerBlock, far = state.is(RopewayRegistry.RETURN.get());
        if (!terminal && !tower && !far) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)) return InteractionResult.SUCCESS;
        if (terminal) {
            start(server, player, pos);
        } else {
            Pending pending = PENDING.get(player.getUUID());
            if (pending == null || pending.dimension != server.dimension()) {
                say(player, "need_terminal");
            } else {
                add(server, player, pending, pos, far);
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void start(ServerLevel level, Player player, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RopewayTerminalBlockEntity terminal)) return;
        if (terminal.hasLine()) {
            if (!player.isShiftKeyDown()) {
                say(player, "exists");
                return;
            }
            terminal.cutLine(level, null, true);
        }
        PENDING.put(player.getUUID(), new Pending(level.dimension(), pos));
        level.playSound(null, pos, RopewayRegistry.ROPE_TIE.get(), SoundSource.BLOCKS, 0.9f, 0.8f);
        say(player, "started");
    }

    private static void add(ServerLevel level, Player player, Pending pending, BlockPos pos, boolean far) {
        BlockPos from = pending.nodes.getLast();
        if (pending.nodes.contains(pos)) {
            say(player, "twice");
            return;
        }
        if (!(level.getBlockEntity(pending.nodes.getFirst()) instanceof RopewayTerminalBlockEntity terminal)) {
            PENDING.remove(player.getUUID());
            say(player, "lost");
            return;
        }
        if (pending.nodes.size() > MAX_SPANS - (far ? 0 : 1)) {
            say(player, "too_many", MAX_SPANS);
            return;
        }
        if (far ? level.getBlockEntity(pos) instanceof RopewayReturnBlockEntity station && station.attached()
                : level.getBlockEntity(pos) instanceof RopewayTowerBlockEntity tower && tower.attached()) {
            say(player, "taken");
            return;
        }
        Vec3 a = RopewayPath.anchor(from), b = RopewayPath.anchor(pos);
        double length = a.distanceTo(b), flat = Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z));
        boolean steel = steelEnd(level, from) && steelEnd(level, pos);
        int limit = steel ? Config.TRANSPORT_ROPEWAY_SPAN_STEEL.getAsInt() : Config.TRANSPORT_ROPEWAY_SPAN_WOOD.getAsInt();
        if (length > limit) {
            say(player, "too_long", limit);
            return;
        }
        if (Math.abs(b.y - a.y) > flat * STEEP) {
            say(player, "too_steep");
            return;
        }
        if (pending.length + length > Config.TRANSPORT_ROPEWAY_MAX_LENGTH.getAsInt()) {
            say(player, "too_far", Config.TRANSPORT_ROPEWAY_MAX_LENGTH.getAsInt());
            return;
        }
        BlockPos blocked = blockedAt(level, from, pos);
        if (blocked != null) {
            say(player, "blocked", blocked.getX() + " " + blocked.getY() + " " + blocked.getZ());
            return;
        }
        int cost = (int) Math.ceil(length / ROPE_BLOCKS);
        if (!player.hasInfiniteMaterials() && count(player) < cost) {
            say(player, "need_rope", cost);
            return;
        }
        if (!player.hasInfiniteMaterials()) spend(player, cost);
        pending.nodes.add(pos);
        pending.length += length;
        level.playSound(null, pos, RopewayRegistry.ROPE_TIE.get(), SoundSource.BLOCKS, 0.9f, 0.9f + 0.05f * pending.nodes.size());
        spark(level, a, b);
        if (!far) {
            say(player, "span", Math.round(length), cost);
            return;
        }
        terminal.setLine(level, pending.nodes);
        PENDING.remove(player.getUUID());
        level.playSound(null, pos, RopewayRegistry.LINE_STRUNG.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        level.playSound(null, pending.nodes.getFirst(), RopewayRegistry.LINE_STRUNG.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        say(player, "done", Math.round(pending.length), terminal.capacity());
    }

    /** Stations and steel towers take the long spans. */
    private static boolean steelEnd(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !(state.getBlock() instanceof RopewayTowerBlock tower) || tower.steel();
    }

    /** A little dust along a new span: it is made fast. */
    private static void spark(ServerLevel level, Vec3 a, Vec3 b) {
        int steps = (int) Math.min(12, Math.max(3, a.distanceTo(b) / 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** The first solid block in the way of either rope or the line between them, ignoring the two posts. */
    public static @Nullable BlockPos blockedAt(Level level, BlockPos fromNode, BlockPos toNode) {
        Vec3 a = RopewayPath.anchor(fromNode), b = RopewayPath.anchor(toNode);
        Vec3 along = b.subtract(a);
        double length = along.length();
        if (length < 2.0) return null;
        Vec3 unit = along.scale(1.0 / length);
        double flat = Math.sqrt(along.x * along.x + along.z * along.z);
        Vec3 side = flat < 1.0E-6 ? new Vec3(1, 0, 0) : new Vec3(-along.z / flat, 0, along.x / flat);
        for (double offset : new double[] {0, RopewayPath.LATERAL, -RopewayPath.LATERAL}) {
            Vec3 shift = side.scale(offset);
            Vec3 start = a.add(unit.scale(0.8)).add(shift), end = b.subtract(unit.scale(0.8)).add(shift);
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(fromNode) && !hit.getBlockPos().equals(toNode)) return hit.getBlockPos();
        }
        return null;
    }

    private static int count(Player player) {
        int total = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(RopewayRegistry.WIRE_ROPE.get())) total += stack.getCount();
        }
        return total;
    }

    private static void spend(Player player, int count) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && count > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(RopewayRegistry.WIRE_ROPE.get())) continue;
            int take = Math.min(count, stack.getCount());
            stack.shrink(take);
            count -= take;
        }
    }

    private static void say(Player player, String key, Object... args) {
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ropeway.line." + key, args));
    }
}
