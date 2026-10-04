package dev.strataindustria.knapping;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.BoulderBlock;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.StrikeNudge;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Splitting boulders by hand (redesign round 3, R1). Each blow opens a crack and plays the next note of the
 * rock's own voice; the blow after the last crack splits the boulder with a bang and a burst of shards that
 * hop out across the ground. Nothing can go wrong and nothing is spent: a held button keeps striking at a
 * steady pace. A cobbled rock struck on bare stone gives two shards the same way.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class Boulders {
    /** Cracks a boulder shows before the next blow splits it. */
    public static final int MAX_CRACKS = 2;
    /** Blows closer together than this are a held button repeating faster than the rock can answer. */
    public static final int BLOW_GAP_TICKS = 7;
    /** Shards from one split of a big or medium boulder, and from the last one. */
    public static final int SPLIT_MIN = 2;
    public static final int BURST_MIN = 3;
    public static final int BURST_MAX = 5;
    /** The how-to line is shown again after this long without a blow. */
    private static final long HINT_AGAIN_TICKS = 6000;

    private static final Map<UUID, Long> LAST_BLOW = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_HINT = new ConcurrentHashMap<>();

    private Boulders() {}

    /** One blow on the boulder at {@code pos}. {@code now} is explicit so tests can space their blows. */
    public static void strike(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, Vec3 at, long now) {
        if (!(state.getBlock() instanceof BoulderBlock block)) return;
        Long previous = LAST_BLOW.put(player.getUUID(), now);
        if (previous != null && now - previous < BLOW_GAP_TICKS) {
            LAST_BLOW.put(player.getUUID(), previous);
            return;
        }
        RandomSource random = level.getRandom();
        Rock rock = block.rock();
        int cracks = state.getValue(BoulderBlock.CRACKS);
        int size = state.getValue(BoulderBlock.SIZE);
        boolean flinty = state.getValue(BoulderBlock.FLINTY);
        BoulderBlock.Coat coat = state.getValue(BoulderBlock.COAT);
        float voice = rock.grain().strikePitch();
        float vary = 0.97f + random.nextFloat() * 0.06f;
        Item shard = block.shard();

        if (cracks < MAX_CRACKS) {
            float note = Smithing.notePitch(cracks, MAX_CRACKS + 1, false, true);
            level.setBlock(pos, state.setValue(BoulderBlock.CRACKS, cracks + 1), Block.UPDATE_ALL);
            level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_STRIKE.get(), SoundSource.BLOCKS, 1.0f, pitch(voice * note * vary));
            level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_CRACK.get(), SoundSource.BLOCKS, 0.5f + 0.2f * cracks, pitch(voice * note * 0.9f * vary));
            if (flinty) level.playSound(null, at.x, at.y, at.z, ModSounds.SHAPING_CHIME.get(), SoundSource.BLOCKS, 0.35f, pitch(note * 1.3f));
            int chips = (5 + 3 * cracks) * rock.grain().chipFactor();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, chips, 0.22, 0.12, 0.22, 0.08);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shard), at.x, at.y, at.z, 3 + cracks * 2, 0.15, 0.1, 0.15, 0.12);
            level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.1, at.z, 1 + cracks, 0.15, 0.05, 0.15, 0.01);
            if (flinty || rock.grain() == Grain.CLEAN) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2 + cracks * 2, 0.12, 0.08, 0.12, 0.2);
            }
            if (coat != BoulderBlock.Coat.NONE) {
                level.sendParticles(new DustParticleOptions(coat.stain().color(), 1.0f), at.x, at.y, at.z, 6 + 3 * cracks, 0.2, 0.1, 0.2, 0.03);
            }
            hint(player, now);
            return;
        }

        // The split: a crack and a burst. A bigger boulder leaves a smaller one standing.
        boolean last = size == 1;
        int shards = last ? BURST_MIN + random.nextInt(BURST_MAX - BURST_MIN + 1) : SPLIT_MIN + random.nextInt(2);
        int flint = !flinty ? 0 : last ? 1 + random.nextInt(2) : random.nextInt(2);
        int chunks = coat == BoulderBlock.Coat.NONE ? 0 : last ? 1 + random.nextInt(2) : random.nextInt(2);
        Vec3 top = Vec3.atCenterOf(pos).add(0.0, 0.1 + 0.15 * size, 0.0);
        if (last) level.removeBlock(pos, false);
        else level.setBlock(pos, state.setValue(BoulderBlock.SIZE, size - 1).setValue(BoulderBlock.CRACKS, 0), Block.UPDATE_ALL);

        level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_SPLIT.get(), SoundSource.BLOCKS, 1.2f, pitch(voice * Smithing.notePitch(0, 1, true, true) * 0.75f * vary));
        level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_SHARDS.get(), SoundSource.BLOCKS, 0.9f, 0.9f + random.nextFloat() * 0.2f);
        if (flint > 0) level.playSound(null, at.x, at.y, at.z, ModSounds.SHAPING_CHIME.get(), SoundSource.BLOCKS, 0.6f, pitch(Smithing.notePitch(0, 1, true, true)));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), top.x, top.y, top.z, 24 + 8 * size, 0.35, 0.2, 0.35, 0.12);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shard), top.x, top.y, top.z, 10, 0.3, 0.15, 0.3, 0.18);
        level.sendParticles(ParticleTypes.POOF, top.x, top.y, top.z, 6 + 2 * size, 0.3, 0.15, 0.3, 0.03);
        if (flinty || rock.grain() == Grain.CLEAN) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.x, top.y, top.z, 10, 0.25, 0.1, 0.25, 0.25);
        }
        if (coat != BoulderBlock.Coat.NONE) {
            level.sendParticles(new DustParticleOptions(coat.stain().color(), 1.4f), top.x, top.y, top.z, 20, 0.3, 0.15, 0.3, 0.05);
        }
        hop(level, top, new ItemStack(shard), shards);
        if (chunks > 0) {
            level.playSound(null, at.x, at.y, at.z, ModSounds.SHAPING_CHIME.get(), SoundSource.BLOCKS, 0.5f, pitch(Smithing.notePitch(0, 1, true, true) * 1.2f));
            hop(level, top, new ItemStack(chunk(coat, random)), chunks);
        }
        if (flint > 0) hop(level, top, new ItemStack(Items.FLINT), flint);
        if (!(player instanceof FakePlayer) && Config.SMITHING_SCREEN_NUDGE.get()) StrikeNudge.send(player, last ? 1.0f : 0.7f);
    }

    /** The ore chunk inside a stained boulder: rust over iron, green over copper. */
    public static Item chunk(BoulderBlock.Coat coat, RandomSource random) {
        OreMineral mineral = coat == BoulderBlock.Coat.GOSSAN
                ? (random.nextInt(3) == 0 ? OreMineral.HEMATITE : OreMineral.LIMONITE)
                : (random.nextBoolean() ? OreMineral.MALACHITE : OreMineral.NATIVE_COPPER);
        return ModItems.SMALL_ORES.get(mineral).get();
    }

    /** Throws {@code count} of {@code stack} out of the break, one entity each, so they hop apart across the ground. */
    public static void hop(ServerLevel level, Vec3 at, ItemStack stack, int count) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.06 + random.nextDouble() * 0.09;
            ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, stack.copyWithCount(1));
            entity.setDeltaMovement(Math.cos(angle) * speed, 0.26 + random.nextDouble() * 0.12, Math.sin(angle) * speed);
            entity.setPickUpDelay(12);
            level.addFreshEntity(entity);
        }
    }

    /** One how-to line the first time, and again after a long break. */
    private static void hint(ServerPlayer player, long now) {
        Long shown = LAST_HINT.get(player.getUUID());
        if (shown != null && now - shown < HINT_AGAIN_TICKS) return;
        LAST_HINT.put(player.getUUID(), now);
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".boulder.hint"));
    }

    private static float pitch(float value) {
        return Mth.clamp(value, 0.5f, 2.0f);
    }

    /**
     * A cobbled rock struck on bare stone: sneak and use on a rock face. It gives two shards of its rock and
     * costs the cobble. Returns true if it struck.
     */
    public static boolean strikeCobble(ServerLevel level, ServerPlayer player, ItemStack held, Vec3 at, long now) {
        Rock rock = cobbledRock(held);
        if (rock == null) return false;
        Long previous = LAST_BLOW.put(player.getUUID(), now);
        if (previous != null && now - previous < BLOW_GAP_TICKS) {
            LAST_BLOW.put(player.getUUID(), previous);
            return true;
        }
        RandomSource random = level.getRandom();
        if (!player.hasInfiniteMaterials()) held.shrink(1);
        BlockState cobbled = ModBlocks.COBBLED_ROCK.get(rock).get().defaultBlockState();
        float voice = rock.grain().strikePitch();
        level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_STRIKE.get(), SoundSource.BLOCKS, 1.0f, pitch(voice * Smithing.notePitch(1, 3, false, true)));
        level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_SPLIT.get(), SoundSource.BLOCKS, 0.7f, pitch(voice * 1.4f));
        level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_SHARDS.get(), SoundSource.BLOCKS, 0.7f, 1.1f + random.nextFloat() * 0.2f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, cobbled), at.x, at.y + 0.1, at.z, 18, 0.2, 0.1, 0.2, 0.1);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.1, at.z, 3, 0.1, 0.05, 0.1, 0.01);
        hop(level, at.add(0.0, 0.15, 0.0), new ItemStack(ModItems.ROCK_SHARD.get(rock).get()), 2);
        return true;
    }

    /** Which rock a cobbled rock item is, or null. */
    public static Rock cobbledRock(ItemStack stack) {
        for (Rock rock : Rock.values()) {
            if (stack.is(ModItems.COBBLED_ROCK.get(rock).get())) return rock;
        }
        return null;
    }

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !player.isSecondaryUseActive()) return;
        ItemStack held = event.getItemStack();
        if (cobbledRock(held) == null) return;
        BlockState target = level.getBlockState(event.getPos());
        if (!target.is(ModTags.Blocks.ROCKS) && !target.is(ModTags.Blocks.BOULDERS)) return;
        if (strikeCobble(level, player, held, event.getHitVec().getLocation(), level.getGameTime())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_BLOW.remove(event.getEntity().getUUID());
        LAST_HINT.remove(event.getEntity().getUUID());
    }
}
