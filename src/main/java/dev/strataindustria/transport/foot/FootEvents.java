package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/** Building cairns and cutting blazes: the two ways a player leaves a mark (spec 4.4). */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class FootEvents {
    /** Marks in one trail that satisfy the trail goal. */
    public static final int TRAIL_GOAL = 4;

    private FootEvents() {}

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        boolean handled = false;
        if (held.is(ModTags.Items.KNIVES)) {
            handled = cutBlaze(player, held, level, pos, event.getFace());
        } else {
            Rock rock = rockOf(held);
            if (rock != null) handled = stack(player, held, rock, level, pos);
        }
        if (handled) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Which rock a loose rock item is, or null for any other item. */
    static @Nullable Rock rockOf(ItemStack stack) {
        for (Rock rock : Rock.values()) {
            if (stack.is(ModItems.LOOSE_ROCK.get(rock).get())) return rock;
        }
        return null;
    }

    private static boolean isLooseRock(BlockState state) {
        for (Rock rock : Rock.values()) {
            if (state.is(ModBlocks.LOOSE_ROCK.get(rock).get())) return true;
        }
        return false;
    }

    /** Four loose rocks onto a loose rock start a cairn; four more on a cairn raise it, up to three courses. */
    public static boolean stack(Player player, ItemStack held, Rock rock, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean onCairn = state.is(FootRegistry.CAIRN.get());
        if (!onCairn && !isLooseRock(state)) return false;
        if (onCairn && state.getValue(CairnBlock.HEIGHT) >= 3) return false;
        if (!player.hasInfiniteMaterials() && held.getCount() < CairnBlock.ROCKS_PER_COURSE) return false;
        if (level instanceof ServerLevel server) {
            int height = onCairn ? state.getValue(CairnBlock.HEIGHT) + 1 : 1;
            level.setBlock(pos, FootRegistry.CAIRN.get().defaultBlockState()
                    .setValue(CairnBlock.ROCK, rock).setValue(CairnBlock.HEIGHT, height), Block.UPDATE_ALL);
            if (!player.hasInfiniteMaterials()) held.shrink(CairnBlock.ROCKS_PER_COURSE);
            level.playSound(null, pos, FootRegistry.CAIRN_STACK.get(), SoundSource.BLOCKS, 0.9f, 0.9f + 0.1f * height);
            if (!onCairn && player instanceof ServerPlayer serverPlayer) record(serverPlayer, server, pos);
        }
        return true;
    }

    /** A knife on the side of a log cuts a blaze into the bark. */
    public static boolean cutBlaze(Player player, ItemStack knife, Level level, BlockPos pos, Direction face) {
        if (face.getAxis() == Direction.Axis.Y || !level.getBlockState(pos).is(BlockTags.LOGS)) return false;
        BlockPos at = pos.relative(face);
        if (!level.getBlockState(at).canBeReplaced()) return false;
        BlockState blaze = FootRegistry.BLAZE_MARK.get().defaultBlockState().setValue(BlazeMarkBlock.FACING, face);
        if (!blaze.canSurvive(level, at)) return false;
        if (level instanceof ServerLevel server) {
            level.setBlock(at, blaze, Block.UPDATE_ALL);
            knife.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
            level.playSound(null, at, FootRegistry.BLAZE_CUT.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            if (player instanceof ServerPlayer serverPlayer) record(serverPlayer, server, at);
        }
        return true;
    }

    /** Enters a new mark on the player's trail and awards the trail goal at four. */
    public static int record(ServerPlayer player, ServerLevel level, BlockPos pos) {
        int length = TrailMarks.get(level.getServer()).add(player.getUUID(), level, pos);
        if (length >= TRAIL_GOAL) Journal.award(player, Journal.TRAIL_MARKED);
        return length;
    }
}
