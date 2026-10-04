package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tier 5 tags (spec 16.5). */
public final class Tier5Tags {
    /** Creosote-treated wood: it never burns. */
    public static final TagKey<Block> TREATED_WOOD_BLOCKS = TagKey.create(Registries.BLOCK, StrataIndustria.id("treated_wood"));
    public static final TagKey<Item> TREATED_WOOD = TagKey.create(Registries.ITEM, StrataIndustria.id("treated_wood"));

    private Tier5Tags() {}
}
