package dev.strataindustria.journal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockLookup;
import dev.strataindustria.geology.VeinType;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.steam.BoilerBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Studying a block with the journal (journal leads spec, "Study"): the character looks it over and writes down
 * what they can tell, and anything that belongs to a lead brings that lead's hint to mind. Text lives under
 * {@code journal.strataindustria.study.*}.
 */
public final class Study {
    static final String KEY = "journal." + StrataIndustria.MOD_ID + ".study.";
    /** Minerals named on a rock's study note. */
    static final int MINERALS_NAMED = 4;

    /** Blocks that belong to a lead besides the block of the lead's own icon. */
    static final Map<String, Predicate<BlockState>> SUBJECTS = new LinkedHashMap<>();

    static {
        SUBJECTS.put("t0/loose_rock", state -> state.is(ModTags.Blocks.LOOSE_ROCKS));
        SUBJECTS.put("t0/knap", state -> state.is(ModTags.Blocks.LOOSE_ROCKS));
        SUBJECTS.put("t0/twine", state -> state.is(ModTags.Blocks.FIBRE_PLANTS));
        SUBJECTS.put("t0/log", state -> state.is(BlockTags.LOGS));
        SUBJECTS.put("t0/stone_axe", state -> state.is(BlockTags.LOGS));
        SUBJECTS.put("t0/clay", state -> state.is(Blocks.CLAY));
        SUBJECTS.put("t1/nugget", state -> state.is(ModTags.Blocks.SMALL_ORES));
        SUBJECTS.put("t2/alloy_metal", ore(OreMineral.CASSITERITE, OreMineral.BISMUTHINITE, OreMineral.TENNANTITE));
        SUBJECTS.put("t2/stone_anvil", state -> RockLookup.rawRock(state) != null);
        SUBJECTS.put("t3/fire_clay", state -> state.is(ModBlocks.FIRE_CLAY.get()));
        SUBJECTS.put("t3/iron_ore", state -> state.getBlock() instanceof OreBlock ore && ore.mineral().isIron());
        SUBJECTS.put("t3/bellows", state -> state.getBlock() == ModBlocks.BLOOMERY.get());
        SUBJECTS.put("t4/coal", ore(OreMineral.BITUMINOUS_COAL));
        SUBJECTS.put("t4/sphalerite", ore(OreMineral.SPHALERITE));
    }

    private Study() {}

    private static Predicate<BlockState> ore(OreMineral... minerals) {
        List<OreMineral> list = List.of(minerals);
        return state -> state.getBlock() instanceof OreBlock ore && list.contains(ore.mineral());
    }

    /** Studies the block at {@code pos}. Returns true if a new note was written or a lead was hinted. */
    public static boolean study(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) return false;
        List<String> hinted = new ArrayList<>();
        for (String path : leadsFor(player, state)) {
            if (Leads.hint(player, path)) hinted.add(path);
        }

        String blockKey = state.getBlock().getDescriptionId();
        String key;
        List<String> args = new ArrayList<>();
        args.add(blockKey);
        BlockEntity be = player.level().getBlockEntity(pos);
        Rock rock = RockLookup.rawRock(state);
        if (state.getBlock() instanceof OreBlock ore) {
            key = KEY + "ore";
            args.add(toolKey(ore.mineral(), state));
        } else if (rock != null) {
            key = KEY + "rock";
            args.add(KEY + "category." + rock.category().getSerializedName());
            List<String> minerals = minerals(player, rock);
            if (minerals.isEmpty()) {
                key = KEY + "rock.barren";
            } else {
                args.add(String.join(Journal.ARG_LIST, minerals));
            }
        } else if (state.getBlock() instanceof dev.strataindustria.flora.IndicatorPlantBlock plant) {
            key = KEY + "plant." + plant.plant().id();
        } else if (be instanceof BloomeryBlockEntity bloomery) {
            key = KEY + "bloomery." + bloomery.status().name().toLowerCase(Locale.ROOT);
        } else if (be instanceof BoilerBlockEntity boiler) {
            key = KEY + "boiler." + boiler.status().name().toLowerCase(Locale.ROOT);
        } else if (!hinted.isEmpty()) {
            key = KEY + "lead";
        } else {
            key = KEY + "plain";
        }

        JournalState journal = JournalContent.state(player);
        // One note per block; a machine gets another whenever its state has changed since.
        boolean machine = be instanceof BloomeryBlockEntity || be instanceof BoilerBlockEntity;
        String id = "study/" + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + (machine ? "/" + key : "");
        boolean wrote = journal.see(id);
        if (wrote) Leads.note(player, key, args, Observations.id(icon(state)), !hinted.isEmpty());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JournalContent.STUDY.get(),
                SoundSource.PLAYERS, 0.7f, 0.95f + player.getRandom().nextFloat() * 0.1f);
        if (!wrote && hinted.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(KEY + "nothing_new", Component.translatable(blockKey)));
        }
        return wrote || !hinted.isEmpty();
    }

    /** Leads not yet closed that this block belongs to. */
    static List<String> leadsFor(ServerPlayer player, BlockState state) {
        List<String> paths = new ArrayList<>();
        Item item = state.getBlock().asItem();
        for (JournalState.Lead lead : JournalContent.state(player).leads().values()) {
            if (lead.closed()) continue;
            Predicate<BlockState> subject = SUBJECTS.get(lead.path());
            boolean ownIcon = item != Items.AIR && lead.icon().equals(BuiltInRegistries.ITEM.getKey(item));
            boolean plant = state.getBlock() instanceof dev.strataindustria.flora.IndicatorPlantBlock p && lead.path().equals(p.plant().lead());
            if (ownIcon || plant || (subject != null && subject.test(state))) paths.add(lead.path());
        }
        return paths;
    }

    private static String toolKey(OreMineral mineral, BlockState state) {
        if (state.is(ModTags.Blocks.NEEDS_STEEL_TOOL)) return Observations.KEY + "too_hard.steel";
        if (mineral.needsWroughtIronTool()) return Observations.KEY + "too_hard.wrought_iron";
        if (mineral.needsBronzeTool()) return Observations.KEY + "too_hard.bronze";
        if (mineral.needsCopperTool()) return Observations.KEY + "too_hard.copper";
        return KEY + "tool.any";
    }

    /** The minerals this rock hosts most often, by summed vein weight. */
    static List<String> minerals(ServerPlayer player, Rock rock) {
        Map<OreMineral, Integer> weights = new LinkedHashMap<>();
        var veins = player.level().registryAccess().lookupOrThrow(VeinType.REGISTRY);
        veins.listElements().forEach(holder -> {
            VeinType vein = holder.value();
            if (!vein.hosts().contains(rock)) return;
            for (VeinType.MineralWeight weight : vein.minerals()) {
                weights.merge(weight.mineral(), weight.weight() * vein.weight(), Integer::sum);
            }
        });
        List<String> names = new ArrayList<>();
        weights.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(MINERALS_NAMED)
                .forEach(entry -> names.add(StrataIndustria.MOD_ID + ".ore." + entry.getKey().id()));
        return names;
    }

    private static Item icon(BlockState state) {
        Item item = state.getBlock().asItem();
        return item == Items.AIR ? Items.PAPER : item;
    }
}
