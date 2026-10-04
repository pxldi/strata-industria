package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
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
            for (OreMineral mineral : OreMineral.inRockValues()) {
                ores.add(ModItems.ORE_BLOCKS.get(rock).get(mineral).getKey());
                if (mineral.isIron()) tag(Tags.Items.ORES_IRON).add(ModItems.ORE_BLOCKS.get(rock).get(mineral).getKey());
                if (mineral == OreMineral.NATIVE_GOLD) tag(Tags.Items.ORES_GOLD).add(ModItems.ORE_BLOCKS.get(rock).get(mineral).getKey());
                if (mineral == OreMineral.BITUMINOUS_COAL) tag(Tags.Items.ORES_COAL).add(ModItems.ORE_BLOCKS.get(rock).get(mineral).getKey());
            }
        }

        var smallOres = tag(ModTags.Items.SMALL_ORES);
        for (OreMineral mineral : OreMineral.withPieces()) {
            smallOres.add(ModItems.SMALL_ORES.get(mineral).getKey());
            var mineralTag = tag(ModTags.Items.ores(mineral.id()));
            for (OreGrade grade : OreGrade.values()) {
                mineralTag.add(ModItems.ORE_PIECES.get(mineral).get(grade).getKey());
            }
            if (mineral.isIron()) {
                var iron = tag(ModTags.Items.IRON_ORES).add(ModItems.SMALL_ORES.get(mineral).getKey());
                for (OreGrade grade : OreGrade.values()) {
                    iron.add(ModItems.ORE_PIECES.get(mineral).get(grade).getKey()).add(ModItems.CRUSHED_ORES.get(mineral).get(grade).getKey())
                            .add(ModItems.WASHED_ORES.get(mineral).get(grade).getKey());
                }
            }
        }
        ores.add(ModItems.BOG_IRON.getKey());
        tag(Tags.Items.ORES_IRON).add(ModItems.BOG_IRON.getKey());

        // Tier 3.
        tag(ModTags.Items.BLOOMERY_FUEL).add(key(Items.CHARCOAL));
        tag(ItemTags.WALLS).add(ModItems.FIRE_BRICK_WALL.getKey());
        // Tier 4 spec 5.
        tag(ItemTags.WOODEN_SLABS).add(Tier4Items.TREATED_SLAB.getKey());
        tag(ItemTags.WOODEN_STAIRS).add(Tier4Items.TREATED_STAIRS.getKey());
        tag(ItemTags.WOODEN_FENCES).add(Tier4Items.TREATED_FENCE.getKey());
        tag(Tags.Items.STORAGE_BLOCKS).add(Tier4Items.COKE_BLOCK.getKey());
        // Tier 4 spec 12.1: blast furnace flux; bloomery slag still carries its limestone.
        tag(dev.strataindustria.registry.ModTags.Items.FLUX).add(ModItems.FLUX.getKey()).add(ModItems.BLOOMERY_SLAG.getKey());

        tag(ModTags.Items.KNAPPABLE).addTag(ModTags.Items.LOOSE_ROCKS).add(key(Items.FLINT));
        tag(Tags.Items.STRINGS).add(ModItems.TWINE.getKey());

        var large = tag(ModTags.Items.PIT_KILN_LARGE);
        for (var item : java.util.List.of(ModItems.UNFIRED_SMALL_VESSEL, ModItems.UNFIRED_LARGE_VESSEL, ModItems.UNFIRED_CRUCIBLE,
                Tier4Items.UNFIRED_REFRACTORY_CRUCIBLE)) {
            large.add(item.getKey());
        }
        var fireable = tag(ModTags.Items.PIT_KILN_FIREABLE).addTag(ModTags.Items.PIT_KILN_LARGE)
                .add(ModItems.UNFIRED_INGOT_MOLD.getKey()).add(ModItems.UNFIRED_BRICK.getKey())
                .add(ModItems.UNFIRED_FIRE_BRICK.getKey()).add(Tier4Items.UNFIRED_COKE_OVEN_BRICK.getKey())
                .add(Tier4Items.UNFIRED_REFRACTORY_INGOT_MOLD.getKey()).add(Tier4Items.UNFIRED_REFRACTORY_GEAR_MOLD.getKey())
                .add(Tier4Items.UNFIRED_GEAR_MOLD.getKey());
        for (MoldType type : MoldType.values()) fireable.add(ModItems.UNFIRED_MOLDS.get(type).getKey());

        // Our tools join the vanilla tool tags, which also makes them enchantable like vanilla tools.
        tag(ItemTags.AXES).add(ModItems.STONE_AXE.getKey());
        tag(ItemTags.PICKAXES).add(ModItems.STONE_PICKAXE.getKey());
        tag(ItemTags.SHOVELS).add(ModItems.STONE_SHOVEL.getKey());
        tag(ItemTags.HOES).add(ModItems.STONE_HOE.getKey());
        tag(ModTags.Items.AXES).addTag(ItemTags.AXES);
        tag(ModTags.Items.KNIVES).add(ModItems.STONE_KNIFE.getKey());
        tag(ModTags.Items.HAMMERS).add(ModItems.STONE_HAMMER.getKey());
        metals();
        tag(ItemTags.DURABILITY_ENCHANTABLE).addTag(ModTags.Items.KNIVES).addTag(ModTags.Items.HAMMERS);
        tag(ItemTags.MELEE_WEAPON_ENCHANTABLE).addTag(ModTags.Items.KNIVES).addTag(ModTags.Items.HAMMERS);
        tag(ItemTags.SHARP_WEAPON_ENCHANTABLE).addTag(ModTags.Items.KNIVES);
    }

    /** Spec 6.1 and 8.3: c:ingots and c:nuggets per metal, the any_bronze groups, and metal tools in the tool tags. */
    private void metals() {
        var saws = tag(ModTags.Items.SAWS);
        for (Metal metal : Metal.values()) {
            if (!metal.hasIngot()) continue;
            if (!metal.isVanilla()) {
                tag(ModTags.Items.ingots(metal)).add(key(ModItems.ingot(metal)));
                tag(Tags.Items.INGOTS).addTag(ModTags.Items.ingots(metal));
                if (metal.hasNugget()) {
                    tag(ModTags.Items.nuggets(metal)).add(key(ModItems.NUGGETS.get(metal).get()));
                    tag(Tags.Items.NUGGETS).addTag(ModTags.Items.nuggets(metal));
                }
            }
            if (metal.isBronze()) {
                tag(ModTags.Items.ANY_BRONZE_INGOTS).addTag(ModTags.Items.ingots(metal));
                tag(ModTags.Items.ANY_BRONZE_PLATES).add(ModItems.PLATES.get(metal).getKey());
                tag(ModTags.Items.SOFT_METAL_PLATES).add(ModItems.PLATES.get(metal).getKey());
            }
            if (metal == Metal.COPPER) tag(ModTags.Items.SOFT_METAL_PLATES).add(ModItems.PLATES.get(metal).getKey());
            if (!metal.isToolMetal()) continue;
            if (!metal.isVanilla()) {
                var armour = ModItems.ARMOUR.get(metal);
                tag(ItemTags.HEAD_ARMOR).add(key(armour.get(net.minecraft.world.item.equipment.ArmorType.HELMET).get()));
                tag(ItemTags.CHEST_ARMOR).add(key(armour.get(net.minecraft.world.item.equipment.ArmorType.CHESTPLATE).get()));
                tag(ItemTags.LEG_ARMOR).add(key(armour.get(net.minecraft.world.item.equipment.ArmorType.LEGGINGS).get()));
                tag(ItemTags.FOOT_ARMOR).add(key(armour.get(net.minecraft.world.item.equipment.ArmorType.BOOTS).get()));
            }
            if (ModItems.PROSPECTORS_PICKS.containsKey(metal)) {
                tag(ItemTags.PICKAXES).add(ModItems.PROSPECTORS_PICKS.get(metal).getKey());
            }
            for (MoldType type : metal.toolTypes()) {
                Item tool = ModItems.tool(metal, type);
                if (metal.isVanilla() && tool.builtInRegistryHolder().key().identifier().getNamespace().equals("minecraft")) continue;
                var toolKey = key(tool);
                switch (type) {
                    case PICKAXE_HEAD -> tag(ItemTags.PICKAXES).add(toolKey);
                    case AXE_HEAD -> tag(ItemTags.AXES).add(toolKey);
                    case SHOVEL_HEAD -> tag(ItemTags.SHOVELS).add(toolKey);
                    case HOE_HEAD -> tag(ItemTags.HOES).add(toolKey);
                    case KNIFE_BLADE -> tag(ModTags.Items.KNIVES).add(toolKey);
                    case HAMMER_HEAD -> tag(ModTags.Items.HAMMERS).add(toolKey);
                    case SAW_BLADE -> saws.add(toolKey);
                    case SWORD_BLADE -> tag(ItemTags.SWORDS).add(toolKey);
                }
            }
        }
        // Saws mine like axes, so they join the axe enchantments but not the axe tag (that would make them strip logs).
        tag(ItemTags.MINING_ENCHANTABLE).addTag(ModTags.Items.SAWS);
        tag(ItemTags.MINING_LOOT_ENCHANTABLE).addTag(ModTags.Items.SAWS);
        tag(ItemTags.DURABILITY_ENCHANTABLE).addTag(ModTags.Items.SAWS);
    }
}
