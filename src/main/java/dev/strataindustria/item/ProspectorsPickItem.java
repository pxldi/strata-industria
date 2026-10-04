package dev.strataindustria.item;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModSounds;
import java.util.ArrayList;
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
 * Prospector's pick (spec 10.2): tap a block to sound out the ore in the 25 x 25 x 25 cube around it
 * (33 x 33 x 33 for wrought iron, tier 3 spec 10.3). Reports up to three ores, most first, by rough
 * amount. Lignite, fire clay and bog iron count as deposits too.
 */
public class ProspectorsPickItem extends Item {
    public static final int RADIUS = 12;
    public static final int WROUGHT_IRON_RADIUS = 16;
    public static final int COOLDOWN = 10;

    private final int radius;

    public ProspectorsPickItem(int radius, Properties properties) {
        super(properties);
        this.radius = radius;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos centre = context.getClickedPos();
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        boolean rich = scan(level, centre, radius, counts);
        Component report = report(counts);
        // The wrought iron pick also tells whether any of it is rich (spec 10.3).
        if (rich && radius > RADIUS) report = Component.empty().append(report)
                .append(Component.translatable(StrataIndustria.MOD_ID + ".prospect.rich"));
        player.sendOverlayMessage(report);
        level.playSound(null, centre, ModSounds.PROSPECT.get(), SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        ItemStack stack = context.getItemInHand();
        stack.hurtAndBreak(1, player, context.getHand());
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (player instanceof ServerPlayer serverPlayer) Journal.award(serverPlayer, Journal.PROSPECT);
        return InteractionResult.SUCCESS;
    }

    /** What a block counts as when sounded out: an ore mineral's id or a deposit's name, or null. */
    static String deposit(BlockState state) {
        if (state.getBlock() instanceof OreBlock ore) return ore.mineral().id();
        if (state.is(ModTags.Blocks.PROSPECTABLE)) return state.getBlock().builtInRegistryHolder().key().identifier().getPath();
        return null;
    }

    /**
     * Counts ore and deposit blocks, skipping chunk sections whose palette holds none at all. Returns
     * whether any of the ore is rich.
     */
    static boolean scan(ServerLevel level, BlockPos centre, int radius, Map<String, Integer> counts) {
        boolean rich = false;
        int minY = Math.max(level.getMinY(), centre.getY() - radius);
        int maxY = Math.min(level.getMaxY(), centre.getY() + radius);
        for (int cx = (centre.getX() - radius) >> 4; cx <= (centre.getX() + radius) >> 4; cx++) {
            for (int cz = (centre.getZ() - radius) >> 4; cz <= (centre.getZ() + radius) >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir() || !section.maybeHas(state -> deposit(state) != null)) continue;
                    for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++) {
                        for (int x = Math.max(centre.getX() - radius, cx << 4); x <= Math.min(centre.getX() + radius, (cx << 4) + 15); x++) {
                            for (int z = Math.max(centre.getZ() - radius, cz << 4); z <= Math.min(centre.getZ() + radius, (cz << 4) + 15); z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                String found = deposit(state);
                                if (found == null) continue;
                                counts.merge(found, 1, Integer::sum);
                                if (state.hasProperty(OreGrade.PROPERTY) && state.getValue(OreGrade.PROPERTY) == OreGrade.RICH) rich = true;
                            }
                        }
                    }
                }
            }
        }
        return rich;
    }

    /** "Large cassiterite, traces of native copper", or "No ore nearby". */
    static Component report(Map<String, Integer> counts) {
        if (counts.isEmpty()) return Component.translatable(StrataIndustria.MOD_ID + ".prospect.nothing");
        List<Map.Entry<String, Integer>> found = new ArrayList<>(counts.entrySet());
        found.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        MutableComponent line = Component.empty();
        for (int i = 0; i < Math.min(3, found.size()); i++) {
            if (i > 0) line.append(", ");
            Component ore = Component.translatable(StrataIndustria.MOD_ID + ".ore." + found.get(i).getKey());
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
