package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModCreativeTabs;
import dev.strataindustria.registry.ModItems;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ModLanguageProvider extends LanguageProvider {
    ModLanguageProvider(PackOutput output) {
        super(output, StrataIndustria.MOD_ID, "en_us");
    }

    static String title(String id) {
        return Arrays.stream(id.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    @Override
    protected void addTranslations() {
        add(ModCreativeTabs.MAIN_TAB_TITLE, "Strata Industria");

        addItem(ModItems.PLANT_FIBRE, "Plant Fibre");
        addItem(ModItems.STRAW, "Straw");
        addItem(ModItems.TWINE, "Twine");
        addItem(ModItems.FIBRE_CLOTH, "Fibre Cloth");
        addItem(ModItems.STONE_AXE_HEAD, "Stone Axe Head");
        addItem(ModItems.STONE_KNIFE_BLADE, "Stone Knife Blade");
        addItem(ModItems.STONE_SHOVEL_HEAD, "Stone Shovel Head");
        addItem(ModItems.STONE_HOE_HEAD, "Stone Hoe Head");
        addItem(ModItems.STONE_HAMMER_HEAD, "Stone Hammer Head");
        addItem(ModItems.STONE_SPEAR_HEAD, "Stone Spear Head");
        addItem(ModItems.STONE_PICKAXE_HEAD, "Stone Pickaxe Head");
        addItem(ModItems.STONE_AXE, "Stone Axe");
        addItem(ModItems.STONE_KNIFE, "Stone Knife");
        addItem(ModItems.STONE_SHOVEL, "Stone Shovel");
        addItem(ModItems.STONE_HOE, "Stone Hoe");
        addItem(ModItems.STONE_HAMMER, "Stone Hammer");
        addItem(ModItems.STONE_PICKAXE, "Stone Pickaxe");

        String knapped = StrataIndustria.MOD_ID + ".knapped_from.";
        add(knapped + "rock", "Knapped from %s (%s)");
        add(knapped + "flint", "Knapped from flint");
        for (Rock rock : Rock.values()) {
            add(knapped + "material." + rock.id(), rock.id().replace('_', ' '));
        }
        for (RockCategory category : RockCategory.values()) {
            add(knapped + "category." + category.getSerializedName(), category.getSerializedName().replace('_', ' '));
        }
        add("container." + StrataIndustria.MOD_ID + ".knapping", "Knapping");
        addItem(ModItems.FIRESTARTER, "Firestarter");
        addBlock(ModBlocks.FIRE_PIT, "Fire Pit");
        add("container." + StrataIndustria.MOD_ID + ".fire_pit", "Fire Pit");
        for (HeatBand band : HeatBand.values()) {
            if (band == HeatBand.NONE) continue;
            String name = band.id().replace('_', ' ');
            add(StrataIndustria.MOD_ID + ".heat." + band.id(), name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
        }
        add(StrataIndustria.MOD_ID + ".knapping.need_more", "You need %s %s to start");
        add("container." + StrataIndustria.MOD_ID + ".clay_forming", "Clay Forming");
        addItem(ModItems.UNFIRED_SMALL_VESSEL, "Unfired Small Vessel");
        addItem(ModItems.UNFIRED_LARGE_VESSEL, "Unfired Large Vessel");
        addItem(ModItems.UNFIRED_CRUCIBLE, "Unfired Crucible");
        addItem(ModItems.UNFIRED_INGOT_MOLD, "Unfired Ingot Mold");
        addItem(ModItems.UNFIRED_BRICK, "Unfired Brick");
        addItem(ModItems.SMALL_VESSEL, "Small Vessel");
        addBlock(ModBlocks.LARGE_VESSEL, "Large Vessel");
        addBlock(ModBlocks.CRUCIBLE, "Crucible");
        addItem(ModItems.INGOT_MOLD, "Ingot Mold");
        for (MoldType type : MoldType.values()) {
            String name = title(type.id());
            addItem(ModItems.UNFIRED_MOLDS.get(type), "Unfired " + name + " Mold");
            addItem(ModItems.MOLDS.get(type), name + " Mold");
        }
        add("container." + StrataIndustria.MOD_ID + ".small_vessel", "Small Vessel");
        add("container." + StrataIndustria.MOD_ID + ".large_vessel", "Large Vessel");
        addBlock(ModBlocks.PIT_KILN, "Pit Kiln");
        addBlock(ModBlocks.LOG_PILE, "Log Pile");
        addBlock(ModBlocks.CHARCOAL_PILE, "Charcoal Pile");
        addItem(ModItems.ASH, "Ash");
        addBlock(ModBlocks.FORGE, "Forge");
        addBlock(ModBlocks.QUERN, "Quern");
        addItem(ModItems.QUERNSTONE, "Quernstone");
        add("container." + StrataIndustria.MOD_ID + ".forge", "Forge");
        metals();
        String kiln = StrataIndustria.MOD_ID + ".pit_kiln.";
        add(kiln + "burning", "The kiln is already burning");
        add(kiln + "needs_straw", "The kiln needs 8 straw on top first");
        add(kiln + "needs_logs", "The kiln needs 8 logs on the straw first");
        add(kiln + "needs_walls", "The kiln needs solid blocks on all four sides");
        add(kiln + "needs_air", "The kiln needs open air above it");
        add(kiln + "rain", "Rain would put the fire straight out");

        String subtitles = "subtitles." + StrataIndustria.MOD_ID + ".";
        add(subtitles + "clay.shape", "Clay squelches");
        add(subtitles + "clay.finish", "Clay piece formed");
        add(subtitles + "pit_kiln.straw", "Straw laid");
        add(subtitles + "pit_kiln.log", "Log stacked");
        add(subtitles + "pit_kiln.fired", "Kiln burns out");
        add(subtitles + "heat.quench", "Hot metal hisses");
        add(subtitles + "forge.ignite", "Forge catches");
        add(subtitles + "crucible.melt", "Metal melts");
        add(subtitles + "quern.grind", "Quern grinds");
        add(subtitles + "quern.load", "Quern loaded");
        add(subtitles + "quern.done", "Quern spills ground");
        add(subtitles + "crucible.pour", "Molten metal pours");
        add(subtitles + "mold.knock", "Cast knocked out");
        add(subtitles + "mold.break", "Mold cracks");
        add(subtitles + "knapping.rock", "Stone chips");
        add(subtitles + "knapping.flint", "Flint chips");
        add(subtitles + "knapping.finish", "Stone tool knapped");
        add(subtitles + "firestarter.drill", "Bow drill whirs");
        add(subtitles + "fire_pit.ignite", "Fire catches");
        add(subtitles + "fire_pit.extinguish", "Fire goes out");
        add(subtitles + "fire_pit.torch", "Torch lit");

        for (Rock rock : Rock.values()) {
            String name = title(rock.id());
            addBlock(ModBlocks.RAW_ROCK.get(rock), "Raw " + name);
            addBlock(ModBlocks.COBBLED_ROCK.get(rock), "Cobbled " + name);
            addBlock(ModBlocks.LOOSE_ROCK.get(rock), "Loose " + name);
            for (OreMineral mineral : OreMineral.values()) {
                addBlock(ModBlocks.ORES.get(rock).get(mineral), name + " " + title(mineral.id()) + " Ore");
            }
        }
        for (OreMineral mineral : OreMineral.values()) {
            String name = title(mineral.id());
            addBlock(ModBlocks.SMALL_ORES.get(mineral), "Small " + name);
            for (OreGrade grade : OreGrade.values()) {
                String graded = grade == OreGrade.NORMAL ? name : title(grade.getSerializedName()) + " " + name;
                addItem(ModItems.ORE_PIECES.get(mineral).get(grade), graded);
                addItem(ModItems.CRUSHED_ORES.get(mineral).get(grade), "Crushed " + graded);
            }
        }
        addBlock(ModBlocks.LOOSE_STICK, "Loose Stick");
        addBlock(ModBlocks.LOOSE_FLINT, "Loose Flint");

        String config = StrataIndustria.MOD_ID + ".configuration.";
        add(config + "title", "Strata Industria Configs");
        add(config + "section." + StrataIndustria.MOD_ID + ".common.toml", "Strata Industria Configs");
        add(config + "section." + StrataIndustria.MOD_ID + ".common.toml.title", "Strata Industria Configs");
        add(config + "showDebugInfo", "Show debug info");
        add(config + "worldgen", "World Generation");
        add(config + "provinceScale", "Province size");
        add(config + "veinAttemptsPerCell", "Vein attempts per cell");
        add(config + "shallowBias", "Shallow vein bias");
        add(config + "emptyWeight", "Empty vein weight");
        add(config + "veinSizeMultiplier", "Vein size multiplier");
        add(config + "indicatorDepth", "Indicator depth");
        add(config + "indicatorDensity", "Indicator density");
        add(config + "spawnGuarantee", "Copper near spawn");
        add(config + "vanilla", "Vanilla Changes");
        add(config + "logsNeedAxe", "Logs need an axe");
        add(config + "removeWoodTools", "Remove wooden tools");
        add(config + "removeStoneTools", "Remove stone tools");
        add(config + "planksNeedTools", "Planks need tools");
        add(config + "removeCampfire", "Remove campfire recipe");
        add(config + "gateFurnace", "Gate the furnace");
        add(config + "replaceCopperGear", "Replace copper gear recipes");
        add(config + "leavesDropSticks", "Leaves drop sticks");
        add(config + "gravelFlintChance", "More flint from gravel");
        add(config + "kiln", "Pit Kiln");
        add(config + "burnTicks", "Burn time (ticks)");
        add(config + "charcoal", "Charcoal Pit");
        add(config + "perLog", "Charcoal per log");
        add(config + "crucible", "Crucible");
        add(config + "capacity", "Capacity (units)");
        add(config + "casting", "Casting");
        add(config + "ingotMoldBreak", "Ingot mold break chance");
        add(config + "toolMoldBreak", "Tool mold break chance");
    }

    /** Spec 6 to 8: metal names, the crucible screen, molds, cast parts and tools. */
    private void metals() {
        String id = StrataIndustria.MOD_ID;
        for (Metal metal : Metal.values()) {
            String name = metal == Metal.SLAG_METAL ? "Slag" : title(metal.id());
            add(id + ".metal." + metal.id(), name);
            if (metal.isVanilla() || !metal.hasIngot()) {
                // Copper's ingot and nugget are the vanilla items.
            } else {
                addItem(ModItems.INGOTS.get(metal), name + " Ingot");
                if (metal.hasNugget()) addItem(ModItems.NUGGETS.get(metal), name + " Nugget");
            }
            if (!metal.isToolMetal()) continue;
            addItem(ModItems.PLATES.get(metal), name + " Plate");
            for (MoldType type : MoldType.values()) {
                addItem(ModItems.HEADS.get(metal).get(type), name + " " + title(type.id()));
                var tool = ModItems.TOOLS.get(metal).get(type);
                if (tool instanceof net.neoforged.neoforge.registries.DeferredItem<?> item) {
                    add(item.get(), name + " " + title(type.tool()));
                }
            }
        }
        add("container." + id + ".crucible", "Crucible");
        String status = id + ".crucible.status.";
        add(status + "cold", "Cold");
        add(status + "heating", "Heating up");
        add(status + "melting", "Melting... %s%%");
        add(status + "molten", "%s, molten");
        add(status + "molten_unknown", "Molten, but no known alloy");
        add(status + "solid", "Solid, needs more heat");
        add(status + "no_forge", "Needs a lit forge below");
        add(id + ".crucible.pour", "Pour");
        // The alloy name (%1$s) is left out to fit the line; the status line names it once it forms.
        add(id + ".crucible.hint", "Needs %2$s-%3$s%% %4$s, has %5$s%%");
        String problem = id + ".crucible.problem.";
        add(problem + "no_mold", "Put an empty mold in the mold slot");
        add(problem + "pouring", "Already pouring");
        add(problem + "not_molten", "The metal is not molten");
        add(problem + "not_enough", "Not enough metal to fill that mold");
        add(problem + "no_alloy", "That mix will not make a usable metal");
        add(id + ".mold.contents", "Holds %s (%s units)");
        add(id + ".mold.still_molten", "The metal is still molten. Let it cool first");
        add(id + ".mold.broke", "The mold cracked apart");
        add(id + ".metal.slag_note", "Remelts to most of the metal that went in");
        for (String grade : new String[] {"crude", "rough", "standard", "fine", "masterwork"}) {
            add(id + ".quality." + grade, title(grade) + " quality");
        }
    }
}
