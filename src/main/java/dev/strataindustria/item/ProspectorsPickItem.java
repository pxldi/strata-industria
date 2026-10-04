package dev.strataindustria.item;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Prospector's pick (spec 10.2): tap a block to sound out the ore in the 25 x 25 x 25 cube around it.
 * Reports up to three ores, most first, by rough amount.
 */
public class ProspectorsPickItem extends Item {
    public static final int RADIUS = 12;
    public static final int COOLDOWN = 10;

    public ProspectorsPickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos centre = context.getClickedPos();
        Map<OreMineral, Integer> counts = scan(level, centre);
        player.sendOverlayMessage(report(counts));
        level.playSound(null, centre, ModSounds.PROSPECT.get(), SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        ItemStack stack = context.getItemInHand();
        stack.hurtAndBreak(1, player, context.getHand());
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (player instanceof ServerPlayer serverPlayer) Journal.award(serverPlayer, Journal.PROSPECT);
        return InteractionResult.SUCCESS;
    }

    /** Counts ore blocks by mineral, skipping chunk sections whose palette holds no ore at all. */
    static Map<OreMineral, Integer> scan(ServerLevel level, BlockPos centre) {
        Map<OreMineral, Integer> counts = new EnumMap<>(OreMineral.class);
        int minY = Math.max(level.getMinY(), centre.getY() - RADIUS);
        int maxY = Math.min(level.getMaxY(), centre.getY() + RADIUS);
        for (int cx = (centre.getX() - RADIUS) >> 4; cx <= (centre.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (centre.getZ() - RADIUS) >> 4; cz <= (centre.getZ() + RADIUS) >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir() || !section.maybeHas(state -> state.getBlock() instanceof OreBlock)) continue;
                    for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++) {
                        for (int x = Math.max(centre.getX() - RADIUS, cx << 4); x <= Math.min(centre.getX() + RADIUS, (cx << 4) + 15); x++) {
                            for (int z = Math.max(centre.getZ() - RADIUS, cz << 4); z <= Math.min(centre.getZ() + RADIUS, (cz << 4) + 15); z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (state.getBlock() instanceof OreBlock ore) counts.merge(ore.mineral(), 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }
        return counts;
    }

    /** "Large cassiterite, traces of native copper", or "No ore nearby". */
    static Component report(Map<OreMineral, Integer> counts) {
        if (counts.isEmpty()) return Component.translatable(StrataIndustria.MOD_ID + ".prospect.nothing");
        List<Map.Entry<OreMineral, Integer>> found = new ArrayList<>(counts.entrySet());
        found.sort(Map.Entry.<OreMineral, Integer>comparingByValue().reversed());
        MutableComponent line = Component.empty();
        for (int i = 0; i < Math.min(3, found.size()); i++) {
            if (i > 0) line.append(", ");
            Component ore = Component.translatable(StrataIndustria.MOD_ID + ".ore." + found.get(i).getKey().id());
            // The first entry opens the sentence; the rest continue it in lower case.
            String key = StrataIndustria.MOD_ID + ".prospect." + size(found.get(i).getValue()) + (i > 0 ? ".more" : "");
            line.append(Component.translatable(key, ore));
        }
        return line;
    }

    static String size(int count) {
        if (count >= 150) return "very_large";
        if (count >= 80) return "large";
        if (count >= 30) return "medium";
        if (count >= 10) return "small";
        return "traces";
    }
}
