package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreMineral;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Which loot table each barrel and each block of suspicious gravel gets (structures spec 6). Tables that
 * hand out pieces of the camp's own ore come in one copy per mineral, so a barrel is filled from the
 * table of the vein the camp was dug beside.
 */
public final class CampLoot {
    private CampLoot() {}

    public static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, StrataIndustria.id(path));
    }

    public static ResourceKey<LootTable> key(String path, OreMineral mineral) {
        return key(path + "/" + mineral.id());
    }

    public static final String CLEARING_HUT = "chests/charcoal_burners_clearing/hut";
    /** The hidden cache of each place holds its best table (structures v2, L12); the rebuilds place it. */
    public static final String CLEARING_CACHE = "chests/charcoal_burners_clearing/cache";
    public static final String PROSPECTOR_PACK = "chests/prospector_camp/pack";
    public static final String PROSPECTOR_CACHE = "chests/prospector_camp/cache";
    public static final String MINING_CACHE = "chests/mining_camp/cache";
    public static final String MINING_TENT = "chests/mining_camp/tent";
    public static final String MINING_SMITHY = "chests/mining_camp/smithy";
    public static final String MINING_ORE_CART = "chests/mining_camp/ore_cart";
    public static final String ADIT_CACHE = "chests/collapsed_adit/cache";
    public static final String ADIT_HIDDEN = "chests/collapsed_adit/hidden";
    public static final String BLOOMERY_CACHE = "chests/ruined_bloomery/cache";
    public static final String PLACER_CACHE = "chests/placer_workings/cache";
    public static final String DIG_PROSPECTOR = "archaeology/prospector_camp";
    public static final String DIG_SPOIL = "archaeology/mining_camp_spoil";
    public static final String DIG_ADIT = "archaeology/collapsed_adit";

    /** Barrel loot of a plan, or null for plans without a barrel. */
    public static @Nullable ResourceKey<LootTable> barrel(String plan, OreMineral mineral) {
        return switch (plan) {
            case "charcoal_burners_clearing" -> key(CLEARING_HUT);
            case "prospector_camp" -> key(PROSPECTOR_PACK, mineral);
            case "mining_camp/tent_small", "mining_camp/tent_large", "mining_camp/bunkhouse" -> key(MINING_TENT);
            case "mining_camp/forge_shed" -> key(MINING_SMITHY);
            case "mining_camp/ore_sorting", "mining_camp/tramway" -> key(MINING_ORE_CART, mineral);
            case "adit/cache" -> key(ADIT_CACHE, mineral);
            case "ruined_bloomery/stump" -> key(BLOOMERY_CACHE);
            case "placer_workings/bank" -> key(PLACER_CACHE);
            default -> null;
        };
    }

    /** Archaeology loot of a plan's suspicious gravel. */
    public static ResourceKey<LootTable> dig(String plan, OreMineral mineral) {
        return switch (plan) {
            case "trial_pit" -> key(DIG_PROSPECTOR, mineral);
            case "collapsed_adit/portal", "adit/collapsed" -> key(DIG_ADIT, mineral);
            default -> key(DIG_SPOIL, mineral);
        };
    }
}
