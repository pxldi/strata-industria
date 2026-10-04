package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

final class ModItemTagsProvider extends ItemTagsProvider {
    ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, StrataIndustria.MOD_ID);
    }

    private static net.minecraft.resources.ResourceKey<Item> key(Item item) {
        return item.builtInRegistryHolder().key();
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        for (RockCategory category : RockCategory.values()) {
            var categoryTag = tag(ModTags.Items.rocks(category));
            for (Rock rock : Rock.values()) {
                if (rock.category() == category) categoryTag.add(ModItems.RAW_ROCK.get(rock).getKey());
            }
            tag(ModTags.Items.ROCKS).addTag(ModTags.Items.rocks(category));
            if (category.isIgneous()) tag(ModTags.Items.IGNEOUS_ROCKS).addTag(ModTags.Items.rocks(category));
        }
        tag(Tags.Items.STONES).addTag(ModTags.Items.ROCKS);

        var looseRocks = tag(ModTags.Items.LOOSE_ROCKS);
        var cobbled = tag(Tags.Items.COBBLESTONES);
        var ores = tag(Tags.Items.ORES);
        for (Rock rock : Rock.values()) {
            looseRocks.add(ModItems.LOOSE_ROCK.get(rock).getKey());
            cobbled.add(ModItems.COBBLED_ROCK.get(rock).getKey());
            for (OreMineral mineral : OreMineral.values()) {
                ores.add(ModItems.ORE_BLOCKS.get(rock).get(mineral).getKey());
            }
        }

        var smallOres = tag(ModTags.Items.SMALL_ORES);
        for (OreMineral mineral : OreMineral.values()) {
            smallOres.add(ModItems.SMALL_ORES.get(mineral).getKey());
            var mineralTag = tag(ModTags.Items.ores(mineral.id()));
            for (OreGrade grade : OreGrade.values()) {
                mineralTag.add(ModItems.ORE_PIECES.get(mineral).get(grade).getKey());
            }
        }

        tag(ModTags.Items.KNAPPABLE).addTag(ModTags.Items.LOOSE_ROCKS).add(key(Items.FLINT));
        tag(Tags.Items.STRINGS).add(ModItems.TWINE.getKey());

        var large = tag(ModTags.Items.PIT_KILN_LARGE);
        for (var item : java.util.List.of(ModItems.UNFIRED_SMALL_VESSEL, ModItems.UNFIRED_LARGE_VESSEL, ModItems.UNFIRED_CRUCIBLE)) {
            large.add(item.getKey());
        }
        var fireable = tag(ModTags.Items.PIT_KILN_FIREABLE).addTag(ModTags.Items.PIT_KILN_LARGE)
                .add(ModItems.UNFIRED_INGOT_MOLD.getKey()).add(ModItems.UNFIRED_BRICK.getKey());
        for (MoldType type : MoldType.values()) fireable.add(ModItems.UNFIRED_MOLDS.get(type).getKey());

        // Our tools join the vanilla tool tags, which also makes them enchantable like vanilla tools.
        tag(ItemTags.AXES).add(ModItems.STONE_AXE.getKey());
        tag(ItemTags.PICKAXES).add(ModItems.STONE_PICKAXE.getKey());
        tag(ItemTags.SHOVELS).add(ModItems.STONE_SHOVEL.getKey());
        tag(ItemTags.HOES).add(ModItems.STONE_HOE.getKey());
        tag(ModTags.Items.AXES).addTag(ItemTags.AXES);
        tag(ModTags.Items.KNIVES).add(ModItems.STONE_KNIFE.getKey());
        tag(ModTags.Items.HAMMERS).add(ModItems.STONE_HAMMER.getKey());
        tag(ModTags.Items.SAWS);
        tag(ItemTags.DURABILITY_ENCHANTABLE).addTag(ModTags.Items.KNIVES).addTag(ModTags.Items.HAMMERS);
        tag(ItemTags.MELEE_WEAPON_ENCHANTABLE).addTag(ModTags.Items.KNIVES).addTag(ModTags.Items.HAMMERS);
        tag(ItemTags.SHARP_WEAPON_ENCHANTABLE).addTag(ModTags.Items.KNIVES);
    }
}
