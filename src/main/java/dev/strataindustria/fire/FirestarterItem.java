package dev.strataindustria.fire;

import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bow drill (tier 0-2 spec 3.5). Hold use on something that can burn: smoke rises as the spindle
 * works, and after {@link #LIGHT_TICKS} the target catches. Looking away stops the drill.
 */
public class FirestarterItem extends Item {
    public static final int LIGHT_TICKS = 50;
    private static final int USE_DURATION = 72000;

    public FirestarterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        if (!Ignitable.mayIgnite(context.getLevel(), pos, state)) return InteractionResult.PASS;
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    /** How far through lighting the user is, 0 to 1. */
    public static float progress(LivingEntity user) {
        return Math.min(1.0f, user.getTicksUsingItem() / (float) LIGHT_TICKS);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof Player player)) {
            user.releaseUsingItem();
            return;
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        int used = USE_DURATION - remaining;
        // The client cannot tell an unfuelled pit from a fuelled one; if the server has not lit it by now, it never will.
        boolean overdue = level.isClientSide() && used > LIGHT_TICKS + 10;
        if (hit.getType() != HitResult.Type.BLOCK || !(state.getBlock() instanceof Ignitable target)
                || !Ignitable.mayIgnite(level, pos, state) || overdue) {
            user.releaseUsingItem();
            return;
        }

        Vec3 at = hit.getLocation();
        if (level.isClientSide()) {
            // More smoke the closer the ember is to catching.
            if (level.getRandom().nextInt(LIGHT_TICKS) < used + 5) {
                level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z,
                        (level.getRandom().nextDouble() - 0.5) * 0.02, 0.03, (level.getRandom().nextDouble() - 0.5) * 0.02);
            }
            return;
        }
        if (used % 5 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FIRESTARTER_DRILL.get(),
                    SoundSource.PLAYERS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.25f);
        }
        if (used >= LIGHT_TICKS) {
            if (target.ignite(level, pos, state)) {
                stack.hurtAndBreak(1, player, player.getUsedItemHand());
            }
            user.releaseUsingItem();
        }
    }
}
