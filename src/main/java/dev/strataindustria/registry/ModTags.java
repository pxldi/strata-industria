package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.RockCategory;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Blocks {
        public static final TagKey<Block> ROCKS = tag("rocks");
        public static final TagKey<Block> IGNEOUS_ROCKS = tag("rocks/igneous");
        public static final TagKey<Block> LOOSE_ROCKS = tag("loose_rocks");
        public static final TagKey<Block> SMALL_ORES = tag("small_ores");
        public static final TagKey<Block> NEEDS_COPPER_TOOL = tag("needs_copper_tool");
        public static final TagKey<Block> NEEDS_BRONZE_TOOL = tag("needs_bronze_tool");
        public static final TagKey<Block> NEEDS_WROUGHT_IRON_TOOL = tag("needs_wrought_iron_tool");
        public static final TagKey<Block> INCORRECT_FOR_STONE_TOOL = tag("incorrect_for_stone_tool");
        public static final TagKey<Block> INCORRECT_FOR_COPPER_TOOL = tag("incorrect_for_copper_tool");
        public static final TagKey<Block> INCORRECT_FOR_BRONZE_TOOL = tag("incorrect_for_bronze_tool");
        /** Plants and leaves: what a knife cuts quickly. */
        public static final TagKey<Block> MINEABLE_WITH_KNIFE = tag("mineable/knife");
        /** Nothing yet: the hammer is a smithing tool and a weapon. */
        public static final TagKey<Block> MINEABLE_WITH_HAMMER = tag("mineable/hammer");
        /** Grass and ferns that give plant fibre and straw when cut with a knife. */
        public static final TagKey<Block> FIBRE_PLANTS = tag("fibre_plants");

        public static TagKey<Block> rocks(RockCategory category) {
            return tag("rocks/" + category.getSerializedName());
        }

        private static TagKey<Block> tag(String path) {
            return TagKey.create(Registries.BLOCK, StrataIndustria.id(path));
        }

        private Blocks() {}
    }

    public static final class Items {
        public static final TagKey<Item> ROCKS = tag("rocks");
        public static final TagKey<Item> IGNEOUS_ROCKS = tag("rocks/igneous");
        public static final TagKey<Item> LOOSE_ROCKS = tag("loose_rocks");
        public static final TagKey<Item> SMALL_ORES = tag("small_ores");
        /** Anything that opens the knapping grid: loose rocks and flint. */
        public static final TagKey<Item> KNAPPABLE = tag("knappable");
        public static final TagKey<Item> AXES = tag("tools/axes");
        public static final TagKey<Item> KNIVES = tag("tools/knives");
        public static final TagKey<Item> HAMMERS = tag("tools/hammers");
        public static final TagKey<Item> SAWS = tag("tools/saws");

        public static TagKey<Item> rocks(RockCategory category) {
            return tag("rocks/" + category.getSerializedName());
        }

        /** Every ore piece and crushed piece of one mineral, all grades. */
        public static TagKey<Item> ores(String mineral) {
            return tag("ores/" + mineral);
        }

        private static TagKey<Item> tag(String path) {
            return TagKey.create(Registries.ITEM, StrataIndustria.id(path));
        }

        private Items() {}
    }

    private ModTags() {}
}
