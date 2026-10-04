package dev.strataindustria.cord;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A strip of bark (redesign R3). Hold use with two in the stack and they twist into one cord in your hands: a creak
 * that climbs with the twist, bark crumbs, a snap as the cord pulls tight. Let go early and nothing is lost; hold on
 * and the next pair starts at once.
 */
public class BarkStripItem extends Item {
    /** Strips that go into one cord. */
    public static final int STRIPS = 2;
    /** Ticks of twisting for one cord. */
    public static final int TWIST_TICKS = 32;
    /** Ticks between creaks while twisting. */
    static final int CREAK_EVERY = 6;

    public BarkStripItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack strips = player.getItemInHand(hand);
        if (strips.getCount() < STRIPS) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message." + StrataIndustria.MOD_ID + ".cord.need_strips"));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BAMBOO_HIT, SoundSource.PLAYERS, 0.4f, 1.3f);
            }
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
        return TWIST_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int used = TWIST_TICKS - remaining;
        if (!(level instanceof ServerLevel server) || used <= 0) return;
        if (stack.getCount() < STRIPS) {
            user.releaseUsingItem();
            return;
        }
        Vec3 hands = user.getEyePosition().add(user.getLookAngle().scale(0.55)).add(0, -0.35, 0);
        if (used % CREAK_EVERY == 1) {
            // The creak climbs about two thirds of an octave over the twist.
            float pitch = 0.8f + 0.9f * used / TWIST_TICKS;
            server.playSound(null, hands.x, hands.y, hands.z, CordSounds.TWIST.get(), SoundSource.PLAYERS, 0.55f, pitch);
        }
        if (used % 3 == 0) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()), hands.x, hands.y, hands.z, 1 + used / 12, 0.06, 0.05, 0.06, 0.02);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player) || stack.getCount() < STRIPS) return stack;
        twist(server, player, stack);
        return stack;
    }

    /** Twists {@link #STRIPS} strips from {@code stack} into one cord, with the snap. */
    public static void twist(ServerLevel level, Player player, ItemStack stack) {
        stack.shrink(STRIPS);
        Vec3 hands = player.getEyePosition().add(player.getLookAngle().scale(0.55)).add(0, -0.35, 0);
        level.playSound(null, hands.x, hands.y, hands.z, CordSounds.TIGHT.get(), SoundSource.PLAYERS, 0.8f, 0.95f + level.getRandom().nextFloat() * 0.15f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.BARK.get()), hands.x, hands.y, hands.z, 14, 0.12, 0.08, 0.12, 0.12);
        level.sendParticles(ParticleTypes.CRIT, hands.x, hands.y, hands.z, 5, 0.1, 0.05, 0.1, 0.15);
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CORD.get()), Prediction.SERVER_ONLY);
        if (player instanceof ServerPlayer) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3f, 1.4f);
        }
    }
}
