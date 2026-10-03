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
