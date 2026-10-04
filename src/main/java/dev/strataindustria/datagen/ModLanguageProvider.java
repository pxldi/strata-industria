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
        addItem(ModItems.FIELD_JOURNAL, "Field Journal");
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
        smithing();
        journal();
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
        add(subtitles + "heat.sear", "Skin sears");
        add(subtitles + "forge.ignite", "Forge catches");
        add(subtitles + "crucible.melt", "Metal melts");
        add(subtitles + "quern.grind", "Quern grinds");
        add(subtitles + "anvil.hit", "Hammer rings");
        add(subtitles + "prospect", "Pick taps rock");
        add(subtitles + "journal.open", "Pages turn");
        add(subtitles + "anvil.done", "Piece finished");
        add(subtitles + "anvil.dress", "Stone dressed");
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
            for (OreMineral mineral : OreMineral.inRockValues()) {
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
        ironAge();
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
        add(config + "smithing", "Smithing");
        add(config + "randomTargets", "Random targets per world");
        add(config + "removeFurnaceCharcoal", "Remove furnace charcoal");
        add(config + "removeOreSmelting", "Remove ore smelting");
        add(config + "removeBlastFurnace", "Remove blast furnace recipe");
        add(config + "replaceIronGear", "Replace iron gear recipes");
        add(config + "replaceGoldGear", "Replace gold gear recipes");
        add(config + "golemDropsNuggets", "Iron golems drop nuggets");
        add(config + "hitCooling", "Cooling per hit on iron");
        add(config + "heat", "Item Heat");
        add(config + "burnPlayer", "Hot items burn bare hands");
        add(config + "journal", "Field Journal");
        add(config + "giveOnJoin", "Give a journal on first join");
    }

    /** Tier 3 spec 3 and 4: fire clay, the new deposits, and the wrought iron name for vanilla iron. */
    private void ironAge() {
        String id = StrataIndustria.MOD_ID;
        addItem(ModItems.FIRE_CLAY_BALL, "Fire Clay Ball");
        addItem(ModItems.GROG, "Grog");
        addItem(ModItems.UNFIRED_FIRE_BRICK, "Unfired Fire Brick");
        addItem(ModItems.FIRE_BRICK, "Fire Brick");
        addBlock(ModBlocks.FIRE_CLAY, "Fire Clay");
        addBlock(ModBlocks.FIRE_BRICKS, "Fire Bricks");
        addBlock(ModBlocks.FIRE_BRICK_SLAB, "Fire Brick Slab");
        addBlock(ModBlocks.FIRE_BRICK_STAIRS, "Fire Brick Stairs");
        addBlock(ModBlocks.FIRE_BRICK_WALL, "Fire Brick Wall");
        addItem(ModItems.LIGNITE, "Lignite");
        addBlock(ModBlocks.LIGNITE_SEAM, "Lignite Seam");
        addBlock(ModBlocks.BOG_IRON, "Bog Iron");
        addBlock(ModBlocks.PLACER_GRAVEL, "Placer Gravel");
        addBlock(ModBlocks.PLACER_SAND, "Placer Sand");
        addItem(ModItems.WROUGHT_IRON_ROD, "Wrought Iron Rod");
        addItem(ModItems.WROUGHT_IRON_DOUBLE_INGOT, "Wrought Iron Double Ingot");
        // Spec 2: the vanilla iron ingot and nugget are wrought iron.
        add("item.minecraft.iron_ingot", "Wrought Iron Ingot");
        add("item.minecraft.iron_nugget", "Wrought Iron Nugget");
        add(id + ".ore.lignite_seam", "lignite");
        add(id + ".ore.fire_clay", "fire clay");
        add(id + ".prospect.rich", ", rich ore nearby");
        add(id + ".crucible.no_iron", "Iron does not melt this hot");
    }

    /** Spec 11: the field journal's goals and hints. */
    private void journal() {
        String journal = "journal." + StrataIndustria.MOD_ID + ".";
        add(journal + "next", "Field journal, next: %s");
        add(journal + "root", "Field Journal");
        add(journal + "root.hint", "From the first loose rock to a bronze anvil. Use the journal to open this page.");
        add(journal + "t0.loose_rock", "A Rock to Start With");
        add(journal + "t0.loose_rock.hint", "Pick up a loose rock lying on the ground.");
        add(journal + "t0.knap", "First Edge");
        add(journal + "t0.knap.hint", "Knap a tool head: use loose rocks or flint and strike away the waste.");
        add(journal + "t0.stone_axe", "Stone Axe");
        add(journal + "t0.stone_axe.hint", "Haft a knapped axe head onto a stick.");
        add(journal + "t0.log", "Timber");
        add(journal + "t0.log.hint", "Fell a tree with your axe. Bare hands will not do.");
        add(journal + "t0.crafting_table", "A Proper Workbench");
        add(journal + "t0.crafting_table.hint", "Build a crafting table.");
        add(journal + "t0.twine", "Twisted Fibre");
        add(journal + "t0.twine.hint", "Cut grass with a knife for plant fibre, then twist it into twine.");
        add(journal + "t0.fire", "Firelight");
        add(journal + "t0.fire.hint", "Build a fire pit and light it with a firestarter.");
        add(journal + "t0.clay", "Riverbank Clay");
        add(journal + "t0.clay.hint", "Gather five clay balls. Look along riverbanks and in swamps.");
        add(journal + "t1.clay_forming", "Shaped by Hand");
        add(journal + "t1.clay_forming.hint", "Form something from clay: use five clay balls to open the forming grid.");
        add(journal + "t1.pit_kiln", "Fired Pottery");
        add(journal + "t1.pit_kiln.hint", "Fire clay pieces in a pit kiln under straw and logs, and let it burn out.");
        add(journal + "t1.charcoal", "Charcoal Burner");
        add(journal + "t1.charcoal.hint", "Stack logs into a pile, light it, then cover every face with soil or stone.");
        add(journal + "t1.forge", "The Forge");
        add(journal + "t1.forge.hint", "Build a forge. It burns charcoal far hotter than a fire pit.");
        add(journal + "t1.nugget", "Signs in the Soil");
        add(journal + "t1.nugget.hint", "Find a small ore on the surface. A vein lies somewhere below it.");
        add(journal + "t1.crucible", "Vessel and Mold");
        add(journal + "t1.crucible.hint", "Fire a crucible and an ingot mold in the pit kiln.");
        add(journal + "t2.melt", "Molten");
        add(journal + "t2.melt.hint", "Set a crucible on a lit forge and melt ore in it.");
        add(journal + "t2.copper_ingot", "First Metal");
        add(journal + "t2.copper_ingot.hint", "Pour molten copper into an ingot mold and knock the ingot out once it cools.");
        add(journal + "t2.copper_pickaxe", "Copper Pick");
        add(journal + "t2.copper_pickaxe.hint", "Make a copper pickaxe. It breaks ore a stone pick cannot.");
        add(journal + "t2.alloy_metal", "Tin, Bismuth or Arsenic");
        add(journal + "t2.alloy_metal.hint", "Mine cassiterite, bismuthinite or tennantite ore.");
        add(journal + "t2.quern", "Ground Fine");
        add(journal + "t2.quern.hint", "Grind ore in a quern. Crushed ore melts down to more metal.");
        add(journal + "t2.bronze", "Bronze Age");
        add(journal + "t2.bronze.hint", "Melt copper with tin, bismuth or arsenic in the right shares and cast a bronze ingot.");
        add(journal + "t2.stone_anvil", "Dressed Stone");
        add(journal + "t2.stone_anvil.hint", "Sneak and strike the top of raw igneous rock with a hammer to dress it into an anvil.");
        add(journal + "t2.smith", "Hammer Work");
        add(journal + "t2.smith.hint", "Heat an ingot in the forge and smith it into a plate on an anvil.");
        add(journal + "t2.bronze_tools", "Full Kit");
        add(journal + "t2.bronze_tools.hint", "Own a bronze pickaxe, axe, shovel, knife, hammer, saw and sword.");
        add(journal + "t2.bronze_armour", "Clad in Bronze");
        add(journal + "t2.bronze_armour.hint", "Wear a full set of bronze armour.");
        add(journal + "t2.prospectors_pick", "Listening to the Rock");
        add(journal + "t2.prospectors_pick.hint", "Strike rock with a prospector's pick to learn what ore lies nearby.");
        add(journal + "t2.bronze_anvil", "Bronze Anvil");
        add(journal + "t2.bronze_anvil.hint", "Build a bronze anvil, the last tool of the bronze age.");
        add(journal + "t3.iron", "Tier 3: Iron");
        add(journal + "t3.iron.hint", "Bronze cannot melt iron. You will need a bloomery.");
    }

    /** Spec 9: anvils, the smithing screen, and tongs. */
    private void smithing() {
        String id = StrataIndustria.MOD_ID;
        for (var entry : ModBlocks.STONE_ANVILS.entrySet()) addBlock(entry.getValue(), title(entry.getKey().id()) + " Anvil");
        addBlock(ModBlocks.BRONZE_ANVIL, "Bronze Anvil");
        addItem(ModItems.TONGS_JAW, "Tongs Jaw");
        addItem(ModItems.TONGS, "Tongs");
        add("item." + id + ".tongs.tooltip", "In the off hand: hold hot metal without burns");
        add("heat." + id + ".too_hot", "Too hot to hold bare-handed. Tongs in your off hand would help.");
        add("death.attack." + id + ".hot_item", "%1$s held on to hot metal for too long");
        add("death.attack." + id + ".hot_item.player", "%1$s held on to hot metal for too long while fighting %2$s");
        add("container." + id + ".anvil", "Anvil");
        String status = id + ".anvil.status.";
        add(status + "empty", "Put a heated workpiece in");
        add(status + "choose", "Pick what to make");
        add(status + "ready", "Hits so far: %s");
        add(status + "too_cold", "Too cold to work");
        add(status + "no_hammer", "Needs a hammer in your hotbar");
        add(status + "too_weak", "This anvil cannot work that metal");
        add(status + "output_full", "Take the finished piece out first");
        add(status + "not_enough", "Needs more of that metal");
        add(status + "no_plan", "Nothing can be smithed from that");
        add(id + ".anvil.out_of_range", "That would overwork it");
        add(id + ".anvil.rules", "Rules");
        add(id + ".anvil.recent", "Last hits");
        add(id + ".anvil.rule", "%s %s");
        for (var hit : dev.strataindustria.smithing.HitType.values()) {
            add(id + ".anvil.hit." + hit.id(), title(hit.id()) + " (%s)");
        }
        for (var kind : dev.strataindustria.smithing.Rule.Kind.values()) {
            add(id + ".anvil.kind." + kind.getSerializedName(), title(kind.getSerializedName()));
        }
        add(id + ".anvil.where.last", "last");
        add(id + ".anvil.where.second_last", "second last");
        add(id + ".anvil.where.third_last", "third last");
        add(id + ".anvil.where.not_last", "not last");
        add(id + ".anvil.where.any", "any of the last three");
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
            if (!metal.isVanilla()) {
                for (var piece : ModItems.ARMOUR.get(metal).entrySet()) {
                    add(piece.getValue().get(), name + " " + title(piece.getKey().getName()));
                }
            }
            if (ModItems.PROSPECTOR_HEADS.containsKey(metal)) {
                addItem(ModItems.PROSPECTOR_HEADS.get(metal), name + " Prospector's Pick Head");
                addItem(ModItems.PROSPECTORS_PICKS.get(metal), name + " Prospector's Pick");
            }
            for (MoldType type : metal.toolTypes()) {
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
        String prospect = id + ".prospect.";
        add(prospect + "nothing", "No ore nearby");
        String[][] sizes = {{"traces", "Traces of %s", "traces of %s"}, {"small", "Small %s", "small %s"},
                {"medium", "Medium %s", "medium %s"}, {"large", "Large %s", "large %s"}, {"very_large", "Very large %s", "very large %s"}};
        for (String[] size : sizes) {
            add(prospect + size[0], size[1]);
            add(prospect + size[0] + ".more", size[2]);
        }
        for (OreMineral mineral : OreMineral.values()) add(id + ".ore." + mineral.id(), mineral.id().replace('_', ' '));
        add(id + ".metal.slag_note", "Remelts to most of the metal that went in");
        for (String grade : new String[] {"crude", "rough", "standard", "fine", "masterwork"}) {
            add(id + ".quality." + grade, title(grade) + " quality");
        }
    }
}
