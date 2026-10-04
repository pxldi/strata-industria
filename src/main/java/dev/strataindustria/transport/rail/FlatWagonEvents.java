package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.outpost.OutpostCharterBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/**
 * Lifting a block onto a flat wagon (outposts spec 7.2): sneak-use a block with an empty hand while an empty flat
 * wagon stands within reach of it. Setting it down is the wagon's own use ({@link FlatWagonEntity}).
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class FlatWagonEvents {
    /** How far from the block a wagon may stand and still take it. */
    public static final double REACH = 3.5;

    private FlatWagonEvents() {}

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty()) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        FlatWagonEntity wagon = emptyWagonNear(level, pos);
        if (wagon == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level instanceof ServerLevel server) lift(server, player, pos, wagon);
    }

    /** Takes the block at {@code pos} onto {@code wagon}, or tells the player it will not come. True when it went. */
    public static boolean lift(ServerLevel server, Player player, BlockPos pos, FlatWagonEntity wagon) {
        BlockState state = server.getBlockState(pos);
        if (!liftable(server, pos, state)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".flat_wagon.too_fixed"));
            return false;
        }
        BlockEntity entity = server.getBlockEntity(pos);
        wagon.putLoad(state, entity == null ? null : entity.saveWithFullMetadata(server.registryAccess()));
        // The block entity goes first so a container does not spill what the wagon is now carrying.
        if (entity != null) server.removeBlockEntity(pos);
        server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        puff(server, pos, state, false);
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".flat_wagon.lifted"));
        return true;
    }

    /** The nearest flat wagon within reach of {@code pos} with nothing on its deck. */
    public static @Nullable FlatWagonEntity emptyWagonNear(Level level, BlockPos pos) {
        List<FlatWagonEntity> wagons = level.getEntitiesOfClass(FlatWagonEntity.class, new AABB(pos).inflate(REACH), wagon -> !wagon.loaded());
        FlatWagonEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (FlatWagonEntity wagon : wagons) {
            double distance = wagon.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance < bestDistance) {
                best = wagon;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Whether a block can ride: not bedrock, a rail, a charter or something that breaks when moved. */
    static boolean liftable(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        if (state.getBlock() instanceof BaseRailBlock || state.getBlock() instanceof OutpostCharterBlock) return false;
        PushReaction reaction = state.getPistonPushReaction();
        return reaction == PushReaction.PUSH_PULL || reaction == PushReaction.PUSH;
    }

    /** Dust and a thump as a block goes on or comes off the deck. */
    static void puff(ServerLevel level, BlockPos pos, BlockState state, boolean down) {
        level.playSound(null, pos, RailwayRegistry.FLAT_LOAD.get(), SoundSource.BLOCKS, 0.9f, down ? 0.85f : 1.1f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.05);
    }
}
