package dev.strataindustria.grid;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Lightning harvest (uniqueness 7.3). Vanilla steers a storm's bolts to the nearest lightning rod. A mast of at least
 * three rods standing on a Leyden jar or on cable that leads to jars sends the bolt down into the bank: each rod of
 * the mast, up to eight, brings 8 000 J, shared among the jars that have room. A short mast, or one that stands on
 * nothing, only draws the bolt: sparks, thunder and no power.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class LightningHarvest {
    public static final int MIN_MAST = 3;
    public static final int MAX_MAST = 8;
    public static final double JOULES_PER_ROD = 8_000;

    private LightningHarvest() {}

    @SubscribeEvent
    static void onBolt(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LightningBolt bolt) || event.loadedFromDisk()) return;
        BlockPos top = findMast(level, bolt.blockPosition());
        if (top != null) strike(level, top);
    }

    /** The top rod of the mast a bolt came down on; vanilla puts the bolt on the block above the rod. */
    static BlockPos findMast(ServerLevel level, BlockPos bolt) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = bolt.above(dy);
            if (!level.getBlockState(pos).is(BlockTags.LIGHTNING_RODS)) continue;
            while (level.getBlockState(pos.above()).is(BlockTags.LIGHTNING_RODS)) pos = pos.above();
            return pos;
        }
        return null;
    }

    /** Height of the mast whose top rod is {@code top}, counting rods straight down. */
    public static int mastHeight(ServerLevel level, BlockPos top) {
        int height = 0;
        BlockPos pos = top;
        while (level.getBlockState(pos).is(BlockTags.LIGHTNING_RODS)) {
            height++;
            pos = pos.below();
        }
        return height;
    }

    /** A bolt coming down the mast whose top rod is {@code top}. Returns the joules that reached the jars. */
    public static double strike(ServerLevel level, BlockPos top) {
        int height = mastHeight(level, top);
        BlockPos foot = top.below(height);
        double delivered = 0;
        List<LeydenJarBlockEntity> jars = new ArrayList<>();
        if (height >= MIN_MAST) {
            ElectricNetwork network = ElectricNetworks.networkAt(level, foot);
            if (network != null) {
                for (ElectricStorage storage : network.storages()) {
                    if (storage instanceof LeydenJarBlockEntity jar && !jar.isRemoved()) jars.add(jar);
                }
            }
            delivered = share(jars, JOULES_PER_ROD * Math.min(height, MAX_MAST));
        }
        sparks(level, top, foot, jars, delivered > 0);
        if (delivered > 0) Journal.awardNear(level, foot, Journal.LIGHTNING_BANK);
        return delivered;
    }

    /** Splits a strike evenly among the jars with room; what a full jar cannot take goes to the others, and what none can is lost. */
    public static double share(List<LeydenJarBlockEntity> jars, double joules) {
        List<LeydenJarBlockEntity> open = new ArrayList<>(jars);
        open.sort((a, b) -> Double.compare(a.capacity() - a.stored(), b.capacity() - b.stored()));
        double left = joules, given = 0;
        for (int i = 0; i < open.size(); i++) {
            double taken = open.get(i).charge(left / (open.size() - i));
            left -= taken;
            given += taken;
        }
        return given;
    }

    private static void sparks(ServerLevel level, BlockPos top, BlockPos foot, List<LeydenJarBlockEntity> jars, boolean charged) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, 24, 0.3, 0.4, 0.3, 0.2);
        level.playSound(null, foot, GridSounds.LEYDEN_STRIKE.get(), SoundSource.BLOCKS, charged ? 1.0f : 0.6f, charged ? 1.0f : 0.7f);
        if (!charged) return;
        for (LeydenJarBlockEntity jar : jars) {
            BlockPos at = jar.getBlockPos();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.9, at.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.15);
        }
    }
}
