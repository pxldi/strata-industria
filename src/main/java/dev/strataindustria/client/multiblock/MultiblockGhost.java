package dev.strataindustria.client.multiblock;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.Multiblocks;
import dev.strataindustria.registry.ModItems;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * The ghost preview (progression design 5.6): holding a multiblock's controller shows, where it would go, every
 * block it still needs; holding the field journal does the same for the controller you look at, or the nearest
 * unfinished one. Blocks to place are drawn blue, wrong ones red, and the nearest one is named.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class MultiblockGhost {
    /** Ticks between looking at the world again; the boxes are redrawn every tick. */
    private static final int REFRESH = 10;
    private static final int SCAN_REFRESH = 40;
    private static final int SCAN_RADIUS = 12;
    private static final int MISSING_STROKE = 0xE04AA3FF, MISSING_FILL = 0x484AA3FF;
    private static final int WRONG_STROKE = 0xE0FF5040, WRONG_FILL = 0x58FF5040;
    private static final float INSET = -0.1f;

    private record Target(Multiblock multiblock, BlockPos controller, Direction facing) {}

    private static List<Multiblock.Mismatch> shown = List.of();
    private static @Nullable Target last;
    private static int age;
    private static int scanAge = SCAN_REFRESH;
    private static @Nullable Target scanned;

    private MultiblockGhost() {}

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        shown = List.of();
        last = null;
        scanned = null;
        scanAge = SCAN_REFRESH;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            clear();
            return;
        }
        Target target = target(minecraft, player, level);
        if (target == null) {
            shown = List.of();
            last = null;
            return;
        }
        if (!target.equals(last) || ++age >= REFRESH) {
            age = 0;
            last = target;
            shown = target.multiblock().mismatches(level, target.controller(), target.facing());
        }
        draw(player);
    }

    private static @Nullable Target target(Minecraft minecraft, LocalPlayer player, ClientLevel level) {
        ItemStack main = player.getMainHandItem(), off = player.getOffhandItem();
        boolean journal = main.is(ModItems.FIELD_JOURNAL.get()) || off.is(ModItems.FIELD_JOURNAL.get());
        Block held = controllerBlock(main);
        if (held == null) held = controllerBlock(off);
        if (!journal && held == null) {
            scanAge = SCAN_REFRESH;
            return null;
        }
        Map<Block, Multiblock> controllers = Multiblocks.controllers(level);
        if (controllers.isEmpty()) return null;
        if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockState looked = level.getBlockState(hit.getBlockPos());
            Multiblock pattern = controllers.get(looked.getBlock());
            if (pattern != null && (journal || looked.is(held))) {
                return new Target(pattern, hit.getBlockPos(), Multiblocks.facing(looked));
            }
            Multiblock placing = held == null ? null : controllers.get(held);
            if (placing != null) {
                BlockPos at = looked.canBeReplaced() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
                return new Target(placing, at, player.getDirection().getOpposite());
            }
        }
        return journal ? nearest(level, player, controllers) : null;
    }

    private static @Nullable Block controllerBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? item.getBlock() : null;
    }

    /** The closest controller within reach whose structure is not whole. */
    private static @Nullable Target nearest(ClientLevel level, LocalPlayer player, Map<Block, Multiblock> controllers) {
        if (++scanAge >= SCAN_REFRESH) {
            scanAge = 0;
            scanned = null;
            double best = Double.MAX_VALUE;
            BlockPos origin = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS), origin.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {
                BlockState state = level.getBlockState(pos);
                Multiblock pattern = controllers.get(state.getBlock());
                if (pattern == null) continue;
                double distance = pos.distSqr(origin);
                if (distance >= best) continue;
                Direction facing = Multiblocks.facing(state);
                if (pattern.check(level, pos, facing).complete()) continue;
                best = distance;
                scanned = new Target(pattern, pos.immutable(), facing);
            }
        }
        return scanned;
    }

    private static void draw(LocalPlayer player) {
        Multiblock.Mismatch closest = null;
        double best = Double.MAX_VALUE;
        for (Multiblock.Mismatch mismatch : shown) {
            boolean missing = mismatch.fault() == Multiblock.Fault.MISSING;
            Gizmos.cuboid(mismatch.pos(), INSET, GizmoStyle.strokeAndFill(missing ? MISSING_STROKE : WRONG_STROKE, 2.0f, missing ? MISSING_FILL : WRONG_FILL));
            double distance = mismatch.pos().distToCenterSqr(player.position());
            if (mismatch.expected() != null && distance < best) {
                best = distance;
                closest = mismatch;
            }
        }
        if (closest != null) {
            Gizmos.billboardText(closest.expected().getBlock().getName().getString(), Vec3.atLowerCornerWithOffset(closest.pos(), 0.5, 1.1, 0.5),
                    TextGizmo.Style.whiteAndCentered().withScale(0.2f)).setAlwaysOnTop();
        }
    }
}
