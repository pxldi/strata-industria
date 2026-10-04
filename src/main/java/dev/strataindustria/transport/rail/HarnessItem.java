package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The harness (outposts spec 5.3): used on a tamed adult horse, donkey or mule standing next to wooden track, it puts
 * the animal on the rail as a pony.
 */
public class HarnessItem extends Item {
    public HarnessItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof AbstractHorse animal)) return InteractionResult.PASS;
        if (!(player.level() instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!PonyEntity.canHarness(animal)) {
            say(player, animal.isTamed() ? "pony.cannot" : "pony.untamed");
            return InteractionResult.SUCCESS_SERVER;
        }
        BlockPos rail = trackNear(server, animal);
        if (rail == null) {
            say(player, "pony.no_track");
            return InteractionResult.SUCCESS_SERVER;
        }
        PonyEntity pony = PonyEntity.harness(server, animal, rail);
        if (pony == null) return InteractionResult.PASS;
        server.playSound(null, pony.getX(), pony.getY(), pony.getZ(), RailRegistry.PONY_HARNESS.get(), SoundSource.NEUTRAL, 0.9f, 1.0f);
        if (!player.isCreative()) stack.shrink(1);
        if (player instanceof ServerPlayer serverPlayer) PonyEntity.award(serverPlayer);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** The wooden track block the animal stands on or beside, nearest first. */
    static BlockPos trackNear(ServerLevel level, AbstractHorse animal) {
        BlockPos here = animal.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(here.offset(-1, -1, -1), here.offset(1, 0, 1))) {
            if (!level.getBlockState(pos).is(RailRegistry.TRACK)) continue;
            double dx = pos.getX() + 0.5 - animal.getX(), dz = pos.getZ() + 0.5 - animal.getZ();
            double distance = dx * dx + dz * dz + Math.abs(pos.getY() - animal.getY()) * 0.25;
            if (distance < bestDistance) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void say(Player player, String key) {
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + "." + key));
    }
}
