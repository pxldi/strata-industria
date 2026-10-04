package dev.strataindustria.branch;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Snapping branches off trees (redesign R2). Use leaves with an empty hand, or hold use: the leaves shake and a
 * creak climbs, and on the third shake the branch snaps with a crack, a burst of leaves, one or two sticks and a
 * strip of bark. The leaf block stays; the branch and the leaves round it are bare for a minute, and shaking a
 * bare branch only rustles. Nothing is spent, nothing can fail.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class Branches {
    /** Shakes it takes to snap a branch. */
    public static final int SHAKES = 3;
    /** Ticks a branch stays bare. */
    public static final long BARE_TICKS = 1200;
    /** Leaves round a snapped one that go bare with it. */
    static final int BARE_RADIUS = 2;
    /** Ticks without a shake before the branch settles and the count starts over. */
    static final long SETTLE_TICKS = 30;
    /** Held use repeats about every four ticks; a faster click is ignored so the creak cannot be spammed. */
    static final long MIN_GAP = 3;

    private static final Map<UUID, Shake> SHAKING = new HashMap<>();
    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Map<Long, Long>> BARE = new HashMap<>();

    private record Shake(BlockPos pos, int count, long tick) {}

    private Branches() {}

    @SubscribeEvent
    static void onUseLeaves(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!Config.BRANCH_SNAPPING.getAsBoolean() || !player.getMainHandItem().isEmpty() || player.isSecondaryUseActive()) return;
        ServerLevel level = player.level();
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(BlockTags.LEAVES)) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        shake(level, player, pos);
    }

    /** One shake of the branch at {@code pos}; returns true when it snaps. */
    public static boolean shake(ServerLevel level, ServerPlayer player, BlockPos pos) {
        long now = level.getGameTime();
        BlockState leaves = level.getBlockState(pos);
        Vec3 at = Vec3.atCenterOf(pos);
        if (isBare(level, pos, now)) {
            level.playSound(null, pos, BranchSounds.BARE.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.3f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, leaves), at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0.0);
            player.sendOverlayMessage(Component.translatable("message." + StrataIndustria.MOD_ID + ".branch.bare"));
            return false;
        }
        Shake last = SHAKING.get(player.getUUID());
        if (last != null && now - last.tick < MIN_GAP && last.pos.equals(pos)) return false;
        int count = last != null && now - last.tick <= SETTLE_TICKS && last.pos.distSqr(pos) <= 9 ? last.count + 1 : 1;
        RandomSource random = level.getRandom();
        if (count < SHAKES) {
            SHAKING.put(player.getUUID(), new Shake(pos, count, now));
            // The creak climbs a third with each shake and the leaves lean the way you pull.
            level.playSound(null, pos, BranchSounds.SHAKE.get(), SoundSource.BLOCKS, 0.7f, 0.85f + 0.25f * count);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, leaves), at.x, at.y, at.z, 4 + 3 * count, 0.45, 0.45, 0.45, 0.0);
            return false;
        }
        SHAKING.remove(player.getUUID());
        level.playSound(null, pos, BranchSounds.SNAP.get(), SoundSource.BLOCKS, 1.0f, 0.95f + random.nextFloat() * 0.15f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, leaves), at.x, at.y, at.z, 28, 0.6, 0.6, 0.6, 0.05);
        goBare(level, pos, now);
        Vec3 toward = player.getEyePosition().subtract(at).normalize().scale(0.45);
        drop(level, at.add(toward), new ItemStack(Items.STICK, 1 + random.nextInt(2)), toward);
        drop(level, at.add(toward), new ItemStack(ModItems.BARK.get()), toward);
        return true;
    }

    private static void drop(ServerLevel level, Vec3 at, ItemStack stack, Vec3 toward) {
        ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, stack);
        RandomSource random = level.getRandom();
        entity.setDeltaMovement(toward.x * 0.3 + (random.nextDouble() - 0.5) * 0.12, 0.18 + random.nextDouble() * 0.08,
                toward.z * 0.3 + (random.nextDouble() - 0.5) * 0.12);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    public static boolean isBare(ServerLevel level, BlockPos pos, long now) {
        Map<Long, Long> bare = BARE.get(level.dimension());
        if (bare == null) return false;
        Long until = bare.get(pos.asLong());
        if (until == null) return false;
        if (until <= now) {
            bare.remove(pos.asLong());
            return false;
        }
        return true;
    }

    private static void goBare(ServerLevel level, BlockPos pos, long now) {
        Map<Long, Long> bare = BARE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        if (bare.size() > 4096) bare.values().removeIf(until -> until <= now);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-BARE_RADIUS, -BARE_RADIUS, -BARE_RADIUS), pos.offset(BARE_RADIUS, BARE_RADIUS, BARE_RADIUS))) {
            if (level.getBlockState(p).is(BlockTags.LEAVES)) bare.put(p.asLong(), now + BARE_TICKS);
        }
    }

    /** Leaves no longer drop sticks when broken: the branches do. */
    @SubscribeEvent
    static void onLeafDrops(BlockDropsEvent event) {
        if (!event.getState().is(BlockTags.LEAVES)) return;
        event.getDrops().removeIf(drop -> drop.getItem().is(Items.STICK));
    }

    @SubscribeEvent
    static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SHAKING.remove(event.getEntity().getUUID());
    }
}
