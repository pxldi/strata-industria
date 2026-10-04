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
        public static final TagKey<Block> NEEDS_STEEL_TOOL = tag("needs_steel_tool");
        public static final TagKey<Block> INCORRECT_FOR_WROUGHT_IRON_TOOL = tag("incorrect_for_wrought_iron_tool");
        /** Blocks every heat structure accepts as its walls (tier 3 spec 3.3). */
        public static final TagKey<Block> REFRACTORY = tag("refractory");
        /** Non-ore deposits the prospector's pick reports: lignite, fire clay and bog iron. */
        public static final TagKey<Block> PROSPECTABLE = tag("prospectable");
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
        /** Unfired clay pieces that can be set out in a pit kiln. */
        public static final TagKey<Item> PIT_KILN_FIREABLE = tag("pit_kiln/fireable");
        /** Pieces that fill a whole pit kiln on their own: vessels and the crucible. */
        public static final TagKey<Item> PIT_KILN_LARGE = tag("pit_kiln/large");
        public static final TagKey<Item> AXES = tag("tools/axes");
        public static final TagKey<Item> KNIVES = tag("tools/knives");
        public static final TagKey<Item> HAMMERS = tag("tools/hammers");
        public static final TagKey<Item> SAWS = tag("tools/saws");
        public static final TagKey<Item> ANY_BRONZE_INGOTS = tag("ingots/any_bronze");
        public static final TagKey<Item> ANY_BRONZE_PLATES = tag("plates/any_bronze");
        /** Copper or any bronze plate: the bloomery door and the washing pan. */
        public static final TagKey<Item> SOFT_METAL_PLATES = tag("plates/copper_or_bronze");
        /** Every iron ore piece, crushed or not (tier 3 spec 14.7). */
        public static final TagKey<Item> IRON_ORES = tag("ores/iron_any");
        /** What a bloomery burns: charcoal only (tier 3 spec 5.2). */
        public static final TagKey<Item> BLOOMERY_FUEL = tag("bloomery_fuel");

        public static TagKey<Item> rocks(RockCategory category) {
            return tag("rocks/" + category.getSerializedName());
        }

        /** {@code c:ingots/<metal>}, also each tool metal's repair material. */
        public static TagKey<Item> ingots(dev.strataindustria.material.Metal metal) {
            return TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ingots/" + metal.id()));
        }

        public static TagKey<Item> nuggets(dev.strataindustria.material.Metal metal) {
            return TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "nuggets/" + metal.id()));
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
