package dev.strataindustria.fire;

import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Lights a fire by striking flint on a rock (redesign L9). Flint in one hand, a loose rock in the other,
 * use on anything {@link Ignitable}. Each strike throws sparks, rings a little higher than the last and
 * leaves a thread of smoke; the third catches. Nothing is spent and a strike never fails: the tinder only
 * forgets the sparks after a long pause.
 */
@EventBusSubscriber(modid = "strataindustria")
public final class FlintStrike {
    public static final int STRIKES = 3;
    /** Held use repeats every four ticks; a click inside this gap is the same strike. */
    static final int MIN_GAP = 3;
    /** Ticks without a strike after which the sparks have gone cold. */
    static final int COLD_AFTER = 100;

    private record Progress(BlockPos pos, int strikes, long last) {}

    private static final Map<UUID, Progress> PROGRESS = new HashMap<>();

    public enum Result { NOT_A_TARGET, NO_ROCK, WAIT, STRUCK, LIT }

    private FlintStrike() {}

    /** Whether the stack can be struck on a rock: flint. */
    public static boolean isStriker(ItemStack stack) {
        return Knapping.isFlint(stack);
    }

    public static boolean isRock(ItemStack stack) {
        return stack.is(ModTags.Items.LOOSE_ROCKS);
    }

    /** How many strikes the player has put into the block at {@code pos} so far. */
    public static int strikes(Player player, BlockPos pos, long now) {
        Progress p = PROGRESS.get(player.getUUID());
        return p != null && p.pos.equals(pos) && now - p.last <= COLD_AFTER ? p.strikes : 0;
    }

    /** One strike of the flint in {@code flintHand} on a rock in the other hand, aimed at {@code pos}. Server only. */
    public static Result strike(ServerPlayer player, InteractionHand flintHand, BlockPos pos, Vec3 at, long now) {
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!Ignitable.mayIgnite(level, pos, state)) return Result.NOT_A_TARGET;
        InteractionHand rockHand = flintHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!isStriker(player.getItemInHand(flintHand)) || !isRock(player.getItemInHand(rockHand))) {
            player.sendOverlayMessage(Component.translatable("strataindustria.flint_strike.need_rock"));
            return Result.NO_ROCK;
        }
        Progress before = PROGRESS.get(player.getUUID());
        int done = strikes(player, pos, now);
        if (before != null && before.pos.equals(pos) && now - before.last < MIN_GAP) return Result.WAIT;

        int n = done + 1;
        float pitch = 1.0f + 0.14f * done + (level.getRandom().nextFloat() - 0.5f) * 0.06f;
        level.playSound(null, at.x, at.y, at.z, ModSounds.FLINT_STRIKE.get(), SoundSource.PLAYERS, 0.7f, pitch);
        sparks(level, at, n);

        if (n >= STRIKES) {
            PROGRESS.remove(player.getUUID());
            if (!(state.getBlock() instanceof Ignitable target) || !target.ignite(level, pos, state)) return Result.STRUCK;
            catchFire(level, pos, at);
            return Result.LIT;
        }
        PROGRESS.put(player.getUUID(), new Progress(pos.immutable(), n, now));
        return Result.STRUCK;
    }

    /** More and brighter sparks with every strike; from the second the tinder smoulders. */
    private static void sparks(ServerLevel level, Vec3 at, int n) {
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.05, at.z, 3 + n * 3, 0.06, 0.04, 0.06, 0.25);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.05, at.z, n * 2, 0.05, 0.02, 0.05, 0.0);
        if (n >= 2) {
            level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y + 0.02, at.z, n, 0.04, 0.0, 0.04, 0.005);
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.1, at.z, n, 0.03, 0.02, 0.03, 0.01);
        }
    }

    private static void catchFire(ServerLevel level, BlockPos pos, Vec3 at) {
        level.playSound(null, pos, ModSounds.FLINT_CATCH.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 12, 0.15, 0.1, 0.15, 0.04);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.1, at.z, 5, 0.12, 0.05, 0.12, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.1, 0.05, 0.1, 0.03);
    }

    /** Flint in one hand and a rock in the other, used on something that can burn. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        if (hand != InteractionHand.MAIN_HAND) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        InteractionHand flintHand;
        if (isStriker(main)) flintHand = InteractionHand.MAIN_HAND;
        else if (isStriker(off) && isRock(main)) flintHand = InteractionHand.OFF_HAND;
        else return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!Ignitable.mayIgnite(level, pos, level.getBlockState(pos))) return;
        if (player instanceof ServerPlayer server) {
            Result result = strike(server, flintHand, pos, event.getHitVec().getLocation(), level.getGameTime());
            if (result == Result.NOT_A_TARGET) return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
