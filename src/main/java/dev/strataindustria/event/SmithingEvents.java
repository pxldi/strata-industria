package dev.strataindustria.event;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.smithing.AnvilBlock;
import dev.strataindustria.smithing.AnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Spec 9.1: sneak + right-click the top of raw igneous rock with a hammer to dress it into a stone anvil. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class SmithingEvents {
    private SmithingEvents() {}

    /** A hammer on an anvil strikes, sneaking with it cycles the shape; sneaking with a blank pattern sets its shape. */
    private static boolean handleAnvil(PlayerInteractEvent.RightClickBlock event, Player player, ItemStack held) {
        boolean hammer = held.is(ModTags.Items.HAMMERS);
        boolean pattern = AnvilBlockEntity.isBlankPattern(held) && player.isSecondaryUseActive();
        if (!hammer && !pattern) return false;
        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof AnvilBlockEntity anvil)
                || !(level.getBlockState(event.getPos()).getBlock() instanceof AnvilBlock)) return false;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(player instanceof ServerPlayer server)) return true;
        if (pattern) anvil.record(server, held);
        else if (player.isSecondaryUseActive()) anvil.cycleShape(server);
        else anvil.strikeBy(server);
        return true;
    }

    /** Hot metal into a water cauldron: a hiss, a burst of steam, and the piece is dark and safe. */
    private static boolean quench(PlayerInteractEvent.RightClickBlock event, Player player, ItemStack held) {
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        if (!state.is(Blocks.WATER_CAULDRON) || held.isEmpty() || !Heat.isHot(held, level)) return false;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (level instanceof ServerLevel server) quenchAt(server, event.getPos(), held);
        return true;
    }

    /** Plunges {@code piece} into the cauldron at {@code pos}: it goes cold, with a hiss and steam. */
    public static void quenchAt(ServerLevel server, BlockPos pos, ItemStack piece) {
        Heat.set(piece, Heat.AMBIENT, server.getGameTime());
        server.playSound(null, pos, ModSounds.QUENCH.get(), SoundSource.BLOCKS, 1.0f, 0.95f + server.getRandom().nextFloat() * 0.1f);
        server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 14, 0.2, 0.1, 0.2, 0.04);
        server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.2, 0.05, 0.2, 0.1);
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (handleAnvil(event, player, held)) return;
        if (quench(event, player, held)) return;
        if (!player.isSecondaryUseActive() || !held.is(ModTags.Items.HAMMERS) || event.getFace() != Direction.UP) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Rock rock = null;
        for (Rock r : Rock.values()) {
            if (r.category().isIgneous() && state.is(ModBlocks.RAW_ROCK.get(r).get())) rock = r;
        }
        if (rock == null) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(level instanceof ServerLevel server)) return;
        Block anvil = ModBlocks.STONE_ANVILS.get(rock).get();
        server.setBlock(pos, anvil.defaultBlockState().setValue(AnvilBlock.FACING, player.getDirection().getClockWise()), Block.UPDATE_ALL);
        server.playSound(null, pos, ModSounds.ANVIL_DRESS.get(), SoundSource.BLOCKS, 1.0f, 0.9f + server.getRandom().nextFloat() * 0.2f);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                16, 0.3, 0.05, 0.3, 0.1);
        held.hurtAndBreak(1, player, event.getHand());
        if (player instanceof ServerPlayer serverPlayer) Journal.award(serverPlayer, Journal.STONE_ANVIL);
    }
}
