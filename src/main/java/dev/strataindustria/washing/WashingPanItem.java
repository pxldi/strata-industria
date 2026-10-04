package dev.strataindustria.washing;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModSounds;
import java.util.List;
import net.minecraft.util.Prediction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The washing pan (tier 3 spec 11.1). Scoop up a placer block, or hold crushed ore in the other hand
 * and use the pan; then stand in water and hold use to swirl it clean, one load at a time.
 */
public class WashingPanItem extends Item {
    public static final int WASH_TICKS = 40;
    private static final int USE_DURATION = 72000;

    public WashingPanItem(Properties properties) {
        super(properties);
    }

    static boolean feetInWater(LivingEntity user) {
        return user.level().getFluidState(user.blockPosition()).is(FluidTags.WATER);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack pan = context.getItemInHand();
        if (player == null || pan.has(ModDataComponents.PAN_CONTENTS.get())) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.PLACER_GRAVEL.get()) && !state.is(ModBlocks.PLACER_SAND.get()) && !state.is(ModBlocks.BLACK_SAND.get())) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.removeBlock(pos, false);
            pan.set(ModDataComponents.PAN_CONTENTS.get(), new PanContents(state.getBlock().asItem()));
            level.playSound(null, pos, state.getSoundType(level, pos, player).getBreakSound(), SoundSource.PLAYERS, 0.8f, 1.1f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack pan = player.getItemInHand(hand);
        if (!pan.has(ModDataComponents.PAN_CONTENTS.get())) {
            InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack other = player.getItemInHand(otherHand);
            if (other.isEmpty()) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".washing_pan.how"));
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (WashingRecipe.recipeFor(level, other).isEmpty()) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".washing_pan.cannot", other.getHoverName()));
                return InteractionResult.FAIL;
            }
            pan.set(ModDataComponents.PAN_CONTENTS.get(), new PanContents(other.getItem()));
            other.consume(1, player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRAVEL_PLACE, SoundSource.PLAYERS, 0.6f, 1.2f);
            return InteractionResult.SUCCESS;
        }
        if (!feetInWater(player)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".washing_pan.no_water"));
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        PanContents contents = stack.get(ModDataComponents.PAN_CONTENTS.get());
        if (!(user instanceof Player player) || contents == null || !feetInWater(user)) {
            user.releaseUsingItem();
            return;
        }
        int used = USE_DURATION - remaining;
        if (level.isClientSide()) {
            if (used % 3 == 0) {
                double angle = used * 0.6;
                level.addParticle(ParticleTypes.SPLASH, user.getX() + Math.cos(angle) * 0.4 + user.getLookAngle().x * 0.5,
                        user.getY() + 0.2, user.getZ() + Math.sin(angle) * 0.4 + user.getLookAngle().z * 0.5, 0, 0.05, 0);
            }
            return;
        }
        if (used % 10 == 0) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.WASHING_PAN_SWIRL.get(), SoundSource.PLAYERS,
                    0.5f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        if (used < WASH_TICKS) return;
        InteractionHand hand = player.getUsedItemHand();
        user.releaseUsingItem();
        stack.remove(ModDataComponents.PAN_CONTENTS.get());
        ItemStack load = contents.stack();
        List<ItemStack> out = WashingRecipe.recipeFor(level, load).map(r -> r.value().roll(level.getRandom())).orElse(List.of(load));
        for (ItemStack result : out) {
            player.getInventory().placeItemBackInInventory(result, Prediction.SERVER_ONLY);
        }
        if (out.size() > 1) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.WASHING_PAN_FIND.get(), SoundSource.PLAYERS, 0.5f, 1.5f);
        }
        if (player instanceof ServerPlayer server) Journal.award(server, Journal.WASH);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, user.getX(), user.getY() + 0.3, user.getZ(), 12, 0.4, 0.1, 0.4, 0.1);
        }
        stack.hurtAndBreak(1, player, hand);
    }
}
