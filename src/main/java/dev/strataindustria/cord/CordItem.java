package dev.strataindustria.cord;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.RockLookup;
import dev.strataindustria.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Bark cord (redesign R3). Four cord held against stone are beaten flat: use on stone, three blows with a thump
 * that climbs, and the last one pops a square of bark cloth off the stone. Held use repeats; a blow every few ticks
 * counts, a faster click does not. Nothing can fail and nothing is lost.
 */
public class CordItem extends Item {
    /** Cord that goes into one cloth. */
    public static final int CORD_PER_CLOTH = 4;
    /** Blows it takes. */
    public static final int BLOWS = 3;
    /** Ticks without a blow before the count starts over. */
    static final long SETTLE_TICKS = 30;
    /** Held use repeats about every four ticks; a faster click is ignored. */
    static final long MIN_GAP = 3;

    private static final Map<UUID, Beat> BEATING = new HashMap<>();

    private record Beat(BlockPos pos, int count, long tick) {}

    public CordItem(Properties properties) {
        super(properties);
    }

    /** Stone to beat cord on: bare rock, cobble and the stone of walls and paving. */
    public static boolean isBeatingStone(BlockState state) {
        return RockLookup.rawRock(state) != null || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.STONES)
                || state.is(Tags.Blocks.COBBLESTONES) || state.is(BlockTags.STONE_BRICKS);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        if (player == null || !isBeatingStone(state)) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack cord = context.getItemInHand();
        if (cord.getCount() < CORD_PER_CLOTH) {
            player.sendOverlayMessage(Component.translatable("message." + StrataIndustria.MOD_ID + ".cord.need_cord"));
            return InteractionResult.FAIL;
        }
        beat(level, player, cord, pos, context.getClickedFace());
        return InteractionResult.CONSUME;
    }

    /** One blow on the stone at {@code pos}; returns true when the cloth comes free. */
    public static boolean beat(ServerLevel level, Player player, ItemStack cord, BlockPos pos, Direction face) {
        long now = level.getGameTime();
        Beat last = BEATING.get(player.getUUID());
        if (last != null && now - last.tick < MIN_GAP) return false;
        int count = last != null && now - last.tick <= SETTLE_TICKS && last.pos.distSqr(pos) <= 4 ? last.count + 1 : 1;
        BlockState stone = level.getBlockState(pos);
        Vec3 at = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        if (count < BLOWS) {
            BEATING.put(player.getUUID(), new Beat(pos, count, now));
            // Each thump sits a little higher and a little harder than the last.
            level.playSound(null, pos, CordSounds.BEAT.get(), SoundSource.PLAYERS, 0.7f + 0.15f * count, 0.85f + 0.2f * count);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone), at.x, at.y, at.z, 5 + 3 * count, 0.2, 0.1, 0.2, 0.02);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.CORD.get()), at.x, at.y, at.z, 3 + 2 * count, 0.15, 0.05, 0.15, 0.03);
            return false;
        }
        BEATING.remove(player.getUUID());
        cord.shrink(CORD_PER_CLOTH);
        level.playSound(null, pos, CordSounds.BEAT.get(), SoundSource.PLAYERS, 1.0f, 1.15f);
        level.playSound(null, pos, CordSounds.CLOTH.get(), SoundSource.PLAYERS, 0.9f, 0.95f + level.getRandom().nextFloat() * 0.15f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone), at.x, at.y, at.z, 16, 0.25, 0.1, 0.25, 0.05);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.BARK_CLOTH.get()), at.x, at.y, at.z, 10, 0.2, 0.1, 0.2, 0.08);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.1, at.z, 6, 0.15, 0.1, 0.15, 0.2);
        Block.popResourceFromFace(level, pos, face, new ItemStack(ModItems.BARK_CLOTH.get()));
        return true;
    }

    /** Forgets a player's beating, on logout. */
    public static void forget(UUID player) {
        BEATING.remove(player);
    }
}
