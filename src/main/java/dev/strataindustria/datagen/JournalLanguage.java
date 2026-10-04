package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import java.util.function.BiConsumer;

/**
 * English text of the leads notebook (journal leads spec): each goal's open question ({@code .lead}) and the line
 * written when it closes ({@code .note}), the observations, the study notes, and the notebook itself. The goal
 * titles and hints stay in {@link ModLanguageProvider}. Later tiers add their own leads and notes next to their goals.
 */
final class JournalLanguage {
    private JournalLanguage() {}

    static void add(BiConsumer<String, String> lang) {
        String journal = "journal." + StrataIndustria.MOD_ID + ".";
        leads(lead(lang, journal));
        observations(lang, journal + "observe.");
        study(lang, journal + "study.");
        ui(lang, journal + "ui.");
        lang.accept(journal + "place.note", "%s. %s");

        String subtitles = "subtitles." + StrataIndustria.MOD_ID + ".";
        lang.accept(subtitles + "journal.write", "Pencil scratches");
        lang.accept(subtitles + "journal.cross_off", "Pencil crosses out");
        lang.accept(subtitles + "journal.remember", "Pages leafed back");
        lang.accept(subtitles + "journal.page", "Page turns");
        lang.accept(subtitles + "journal.pin", "Card pinned");
        lang.accept(subtitles + "journal.study", "Something studied");

        String config = StrataIndustria.MOD_ID + ".configuration.";
        lang.accept(config + "hintMinutes", "Minutes before a lead's hint comes to mind");
        lang.accept(config + "checklist", "Show the checklist tab");
    }

    /** Writes a goal's question and closing note. */
    private interface Lead {
        void add(String path, String question, String note);
    }

    private static Lead lead(BiConsumer<String, String> lang, String journal) {
        return (path, question, note) -> {
            String key = journal + path.replace('/', '.');
            lang.accept(key + ".lead", question);
            lang.accept(key + ".note", note);
        };
    }

    private static void leads(Lead lead) {
        // Tier 0: stone
        lead.add("t0/loose_rock", "Bare hands won't get me far. Is there anything lying around I could use?",
                "A fist-sized rock, picked off the ground. It is a start.");
        lead.add("t0/knap", "If I strike one rock against another, could I chip out an edge?",
                "Flake by flake, a rough head came free. Sharp enough to cut, if I am careful. Limestone and shale crumble; rhyolite and flint flake clean.");
        lead.add("t0/stone_axe", "A blade is no good without something to hold it by. How do I haft it?",
                "Lashed to a stick, the head bites into wood. My first real tool.");
        lead.add("t0/log", "The trees here would give me timber, if I had a way to fell them.",
                "The trunk came down with a crack that echoed through the woods.");
        lead.add("t0/crafting_table", "I keep working on my knees in the dirt. A bench would help.",
                "A flat top and four legs. Everything is easier with a place to work.");
        lead.add("t0/twine", "Grass stems are tough when they dry. Could I twist them into cord?",
                "Two strands twisted against each other hold far more than either alone.");
        lead.add("t0/fire", "The nights are cold and the meat is raw. How do I make fire?",
                "Smoke, then a glow, then flame. I sat by it a long while.");
        lead.add("t0/clay", "The banks of the river are slick and grey. Is that clay?",
                "Wet clay, a good armful of it. It holds whatever shape I press into it.");
        // Tier 1: fire and clay
        lead.add("t1/clay_forming", "Clay takes any shape I give it. What should I shape first?",
                "Pinched and smoothed into a pot. Raw clay crumbles, though; it needs fire.");
        lead.add("t1/pit_kiln", "A campfire only cracks raw clay. How do I fire it evenly?",
                "Buried in straw and logs, the pots baked all night. They ring now when I tap them.");
        lead.add("t1/charcoal", "Wood burns too fast and too cool for real work. Can I make a better fuel?",
                "Smothered under earth, the wood charred black and light. It burns hot and clean.");
        lead.add("t1/forge", "Charcoal on an open fire throws its heat away. I need something to hold it in.",
                "A stone hearth with walls around the coals. It glows hotter than anything I have built.");
        lead.add("t1/nugget", "Some of the pebbles here are strangely coloured. What are they?",
                "Ore, washed up from a vein below. Somewhere under my feet there is more.");
        lead.add("t1/crucible", "If I could melt this ore, what would I pour it into?",
                "A crucible to melt in and a mold to pour into, both fired hard.");
        // Tier 2: copper and bronze
        lead.add("t2/melt", "Will my forge get hot enough to melt ore in the crucible?",
                "The ore slumped and ran. I watched a pool of molten metal shine back at me.");
        lead.add("t2/brick_kiln", "A pit kiln takes a night and a pile of logs for a few pots. Is there a quicker way?",
                "A small brick oven that stands on the forge. A load of pots is done in half a minute.");
        lead.add("t2/pattern_casting", "Every mold I make by hand is a day of clay. Can I carve the shape once and use it again?",
                "A plank carved to the shape, pressed into damp sand. The sand mold takes one pour and goes, the plank stays.");
        lead.add("t2/casting_table", "I pour one mold at a time and stand and wait. Can I do several at once?",
                "Four molds on a stone table, and one pour fills the lot.");
        lead.add("t2/copper_ingot", "The melt is ready. What do I do with it before it sets?",
                "Poured, cooled, and knocked out of the mold: a bar of copper.");
        lead.add("t2/copper_pickaxe", "Some rock just shrugs off my stone pick. Would metal cut it?",
                "The copper pick bites where stone only skated. New ground opens up.");
        lead.add("t2/alloy_metal", "Copper bends too easily. Is there another ore that would harden it?",
                "Dark, heavy ore that copper alone could never be. This may be what I was missing.");
        lead.add("t2/quern", "Lumps of ore melt down poorly. Would they melt better ground fine?",
                "Two stones turned against each other, and the ore comes out as grit. More metal from less ore.");
        lead.add("t2/bronze", "What happens if I melt copper together with this new metal?",
                "Copper and tin ran together into something golden and much harder: bronze.");
        lead.add("t2/stone_anvil", "Cast metal is brittle and rough. I need something solid to hammer it on.",
                "A block of hard rock dressed flat on top. It will take a beating.");
        lead.add("t2/smith", "Can I beat hot metal into a shape a mold cannot give?",
                "Heat, strike, heat again. The ingot spread into a thin, even plate.");
        lead.add("t2/bright_strike", "The metal glows brightest just out of the forge. What if I never let it dull?",
                "Every blow landed on bright metal. The piece came out finer than any I had made.");
        lead.add("t2/bronze_tools", "Bronze holds an edge. Every tool I own could be better for it.",
                "Pick, axe, shovel, knife, hammer, saw and sword, all in bronze. A proper kit.");
        lead.add("t2/bronze_armour", "Bronze plate would turn a blow that would cut through leather.",
                "Clad head to foot in bronze. Heavy, but I feel far safer.");
        lead.add("t2/prospectors_pick", "Ore hides in the rock. Could I hear it if I tapped the stone?",
                "The rock rings differently near ore. I am learning to listen.");
        // Tier 3: iron
        lead.add("t3/fire_clay", "Bronze cannot melt iron. What could stand a fire hotter than any I have made?",
                "A pale, gritty clay that does not slump in the fire. Bricks of this would last.");
        lead.add("t3/fire_brick", "Fire clay is no use as a lump. What shape would let me build with it?",
                "Fired bricks, pale and hard. Enough of these would make a furnace.");
        lead.add("t3/iron_ore", "The rust-red stone is everywhere in the old stories. Where does iron ore hide?",
                "Red and heavy, and it took a bronze pick to cut it. Iron ore at last.");
        lead.add("t3/bloomery", "Iron will not run like copper. How did the old smiths ever get it out of the rock?",
                "A brick chimney with a door at the bottom. They baked the iron out instead of melting it.");
        lead.add("t3/bloom", "The bloomery stands ready. What do I feed it, and how long does it burn?",
                "Out of the ashes came a spongy, smoking lump. Iron, full of slag.");
        lead.add("t3/refine", "The bloom is full of slag. How do I get clean iron out of it?",
                "Hammered hot, the slag squirted out in sparks. What is left is wrought iron.");
        lead.add("t3/iron_pickaxe", "Iron is tougher than bronze. What would an iron pick cut?",
                "An iron pick. Rock that laughed at bronze gives way to it.");
        lead.add("t3/bucket", "I have always carried water in clay. Could iron hold more?",
                "A bucket of thin iron. Water, lava, anything goes where I carry it.");
        lead.add("t3/furnace", "A furnace of fire bricks would hold its heat like the bloomery does.",
                "Eight bricks and a door. It cooks and bakes without minding.");
        lead.add("t3/rotation", "Every machine I can picture needs something turning. Where does the turning come from?",
                "A crank on an axle, and my arm makes it turn. Slow, but a start.");
        lead.add("t3/water_power", "The river never stops running. Could it turn my axles for me?",
                "The wheel caught the current and has not stopped turning since.");
        lead.add("t3/millstone", "My arms ache from the quern. Could the axle grind for me?",
                "The millstone grinds day and night, and I never have to touch it.");
        lead.add("t3/bellows", "Fires burn hotter with more air. Could an axle blow it in for me?",
                "The bellows breathe in time with the shaft, and the coals roar white.");
        lead.add("t3/saw_mill", "Splitting planks by hand is slow and wasteful. Could a turning blade do better?",
                "The saw mill rips a log into planks in moments, with the bark left over.");
        lead.add("t3/charter", "The tin is a long walk off. Could I keep a mine going out there?",
                "Charter set at the tin workings. The line runs; the place keeps working while I'm home.");
        lead.add("t3/iron_anvil", "Iron is too hard for my stone anvil. Could I build one from iron itself?",
                "An iron anvil, heavy as the earth. It takes iron and steel.");
        lead.add("t3/weld", "One ingot is too small for the bigger tools. Could two be joined while hot?",
                "Hammered together white hot, two ingots became one. The seam does not show.");
        lead.add("t3/trip_hammer", "I smith the same pieces again and again. With a turning shaft, could a hammer do it for me?",
                "The trip hammer rises and falls on its own and makes whatever shape I set on it.");
        lead.add("t3/core_sample", "My prospector's pick only hears so far. What lies deep below?",
                "The core came up in bands: every layer of rock beneath me, laid out in order.");
        lead.add("t3/wash", "There is ore in the river gravel. Could I wash it out?",
                "Swirled in the pan, the light gravel washed away. Heavy grains stayed behind.");
        lead.add("t3/hide", "The animals I hunt could give me more than meat.",
                "A raw hide, stiff and smelly. It will rot unless I treat it.");
        lead.add("t3/leather", "A raw hide rots. How do I turn it into leather that lasts?",
                "Limed, scraped and soaked in tannin for days. Supple leather, finally.");
        // Tier 4: steel and steam
        lead.add("t4/coal", "Charcoal costs a forest. Is there black rock that burns?",
                "Coal, cut from a deep seam. It burns long and hot, but smoky.");
        lead.add("t4/coke_oven", "Raw coal is too dirty for iron. Could I bake the dirt out of it?",
                "A sealed oven of brick, hollow inside. Whatever I put in will bake without air.");
        lead.add("t4/coke", "The oven is ready. What comes out of coal baked without air?",
                "Grey, light coke that burns hotter than any fuel I have had.");
        lead.add("t4/creosote", "The oven drips a thick black oil. Is it good for anything?",
                "Creosote. Planks soaked in it shrug off rot and water.");
        lead.add("t4/refractory_crucible", "My old crucible would crack in a fire hot enough for iron. What would stand it?",
                "A crucible of fire clay, fired hard. It will hold molten iron.");
        lead.add("t4/molten_iron", "Could iron be melted outright, with coke and enough air?",
                "Iron ran liquid in the crucible. I never thought I would see it.");
        lead.add("t4/steel", "Iron is tough but soft. Would a little carbon make it hard?",
                "A pinch of carbon stirred into molten iron, and it set as steel.");
        lead.add("t4/sphalerite", "Copper and tin make bronze. Is there an ore that makes something brighter?",
                "Sphalerite, glittering and brown. The smiths called it blende: zinc ore.");
        lead.add("t4/roast", "Zinc ore stinks of sulfur and will not melt clean. How do I drive the sulfur off?",
                "Roasted in the forge until the fumes stopped, the ore turned to a crumbly calcine.");
        lead.add("t4/brass", "What would copper and zinc make together?",
                "Brass, bright as gold and easy to work. Fine fittings will come from this.");
        lead.add("t4/solder", "How do I join metal without welding it white hot?",
                "Lead and tin melt low and hold fast. Solder joins what heat cannot.");
        lead.add("t4/pipe", "Steam and water need a way to get from one place to another.",
                "Plates soldered into pipe. Now I can carry fluids wherever I need them.");
        lead.add("t4/boiler", "Water swells into steam when it boils. Could I trap that force?",
                "A bronze boiler over a firebox, and the gauge needle climbing.");
        lead.add("t4/heat_network", "Every machine burns its own fire. Could one fire feed them all through pipes?",
                "Heat ran down the pipe from the firebox and warmed a boiler two rooms away.");
        lead.add("t4/steam_engine", "Steam pushes hard. Could it turn a shaft faster than any river?",
                "The engine hisses and the shaft spins. Steam power at last.");
        lead.add("t4/steam_hammer", "A pattern replays my hand's work. Could steam swing the hammer and keep the iron hot as well?",
                "The steam hammer thunders away at steel and never lets it cool. A hundred plates without lifting a hand.");
        lead.add("t4/crusher", "Steam is strong enough to crush rock. Could it break ore finer than a millstone?",
                "The crusher eats ore and spits out grit, often with a little extra besides.");
        lead.add("t4/blast_furnace", "Crucibles melt iron a little at a time. Could a whole tower of it run at once?",
                "Air roaring into the tuyere, and pig iron running out of the tap hole.");
        lead.add("t4/converter", "Pig iron is brittle from all its carbon. Could I blow the carbon out of it?",
                "Air through molten pig iron, a roaring flame, and when it died down: steel by the ton.");
        lead.add("t4/automated_chain", "I spend my days carrying ore from one machine to the next. Could the machines pass it along themselves?",
                "Belts, chutes and inserters, and a crusher that ran through a whole load with nobody there to feed it.");
        // Tier 5: electricity
        lead.add("t5/latex", "Some trees run thick and white where the bark is cut. Could I collect it?",
                "Latex, slow and sticky. A cup fills in about a day.");
        lead.add("t5/rubber", "Latex sets soft and tacky. How do I make it hold its shape?",
                "Roasted slowly, the raw rubber turned firm and springy.");
        lead.add("t5/cinnabar", "A red ore turns up in the dark rock, and it breaks my pick. What is it?",
                "Cinnabar. It crushes to redstone.");
        lead.add("t5/red_alloy", "Redstone carries a signal like nothing else. Could copper carry it further?",
                "Copper and redstone ran together into a dull red metal.");
        lead.add("t5/wire", "A rod is too thick to carry much. Could I draw it out thinner?",
                "Pulled through the plate, the rod became fine wire. Slow work.");
        lead.add("t5/circuit", "Wire alone does nothing. What would let a machine decide?",
                "Red wire on a soaked board. The first circuit.");
        lead.add("t5/dynamo", "Magnet, wire and something turning. Could that make power?",
                "The dynamo spins and the wire hums. Power from a shaft.");
        lead.add("t5/cable", "Power in the dynamo is no use there. How do I carry it to a machine?",
                "Copper in rubber, laid block to block. The power follows it.");
        lead.add("t5/first_machine", "I have power in the wire. What can it do?",
                "The machine woke, worked, and slowed when the shaft did.");
        lead.add("t5/turbine", "A steam engine turns a shaft and a dynamo turns it back. Is there a shorter way?",
                "Steam straight to power, with no shaft between.");
        lead.add("t5/macerator", "The crusher needs a shaft and steam. Could a machine on the wire do its work?",
                "The macerator grinds ore to grit, and often gives me a second piece.");
        lead.add("t5/assembler", "Small parts take me a day by hand. Could a machine put them together?",
                "The assembler fitted six parts together on its own.");
        lead.add("t5/power_hammer", "The steam hammer needs a boiler and a heat pipe. Could the hammer heat its own work?",
                "The coil heats the metal and the hammer falls. No firebox needed.");
        lead.add("t5/battery", "A dynamo makes power only while the shaft turns. Can I keep some?",
                "Half full and holding. It will run a machine when the shaft stops.");
        lead.add("t5/electric_heat", "A firebox needs fuel and tending. Could the wire heat a crucible instead?",
                "The coil glows and the crucible warms. No fuel to carry.");
        lead.add("t5/sulfuric_acid", "Lye eats grease. Is there something that eats stone and metal?",
                "Acid, heavy and clear. It needs a tank and a steady hand.");
        lead.add("t5/electrolysis", "Could the wire pull water apart?",
                "Two gases from one water, twice as much hydrogen as oxygen.");
        lead.add("t5/alumina", "Clay is mostly one metal that fire alone cannot free. What else could pull it out?",
                "White powder, alumina, and sulfur gas going up the vent.");
        lead.add("t5/aluminium", "Alumina will not melt out like other ores. Could the wire drag the metal out?",
                "A silver ingot, light as wood. Aluminium.");
        lead.add("t5/mv", "My machines are slow. Can they take more power?",
                "The same machine runs four times as fast on the heavier supply.");
        lead.add("t5/transformer", "LV and MV do not mix. Is there a way to pass power between them?",
                "The transformer hums. A little is lost, and both sides stay apart.");
        lead.add("t5/power_line", "Cable wastes power over distance. Is there a way to carry it far?",
                "Pole to pole across the valley, and the machine at the far end ran.");
        lead.add("t5/item_pipe", "I carry every load by hand. Could pipes do it?",
                "Items travel the pipe and land in the chest. No hands.");
        lead.add("t5/storage", "Chests everywhere and I never know what is in which. Could one screen list them?",
                "One screen, every chest. Powered, it also moves items.");
        lead.add("t5/stethoscope", "The shed hums, but I cannot tell if it is well. Could I hear it?",
                "A steady beat is a good machine. A rough one is short of power.");
        lead.add("t5/lightning", "Every storm throws more power at the hill than I make in a week. Could I catch some?",
                "A mast of rods over a row of jars. One bolt filled them.");
        lead.add("t5/ore_scanner", "Prospecting by pick is slow. Could a machine do it for me?",
                "The scanner showed me every ore for three chunks around.");
        lead.add("t5/electric_chain", "I still carry ore from machine to machine. Could the wire and pipes run the whole line?",
                "A line from macerator to furnace to chest, and nobody there to run it.");
    }

    private static void observations(BiConsumer<String, String> lang, String observe) {
        lang.accept(observe + "flint", "Flint breaks with an edge sharper than any rock. Worth knapping.");
        lang.accept(observe + "copper_nugget", "A green-stained pebble with a bead of raw copper in it. If I could get it hot "
                + "enough to run, I could cast it.");
        lang.accept(observe + "tin_nugget", "Heavy dark grains, unlike copper. The old smiths said a little of something like "
                + "this, melted into copper, makes a harder metal.");
        lang.accept(observe + "iron_nugget", "A rust-red pebble, heavy for its size. No crucible fire of mine will touch "
                + "this; iron wants a furnace of its own.");
        lang.accept(observe + "gold_nugget", "A fleck of gold. Pretty and soft, and no good for tools.");
        lang.accept(observe + "clay", "Clay squeezes between my fingers and keeps the shape. Fired, it might hold water. "
                + "Or metal.");
        lang.accept(observe + "raw_hide", "A raw hide. It already smells; left like this it will rot. Something must "
                + "stop that.");
        lang.accept(observe + "coal", "Black stone that burns. Too dirty for iron as it is, but baked in a closed oven it "
                + "might come out clean.");
        lang.accept(observe + "survey_notes", "Someone's field notes on an ore deposit.");
        lang.accept(observe + "overstress", "The shaft groaned and stopped. I asked more of it than its power can give; "
                + "fewer machines, or more power.");
        lang.accept(observe + "too_hard", "The %s is too hard for the pick in my hand. %s");
        lang.accept(observe + "too_hard.copper", "It wants a copper pick.");
        lang.accept(observe + "too_hard.bronze", "It wants a bronze pick.");
        lang.accept(observe + "too_hard.wrought_iron", "It wants an iron pick.");
        lang.accept(observe + "too_hard.steel", "It wants a steel pick.");
        lang.accept(observe + "plant.copper_flower", "A small blue flower, a kind of mint, growing in a patch on this hill. "
                + "I have not seen it on the other hills.");
        lang.accept(observe + "plant.horsetail", "Jointed grey stalks with a ring of thin needles at every joint, "
                + "standing in a clump on the bank.");
        lang.accept(observe + "plant.stunted_birch", "A birch that stopped at knee height. Its leaves have gone yellow, "
                + "and so has every other one around it.");
        lang.accept(observe + "plant.pink_thrift", "Pink pompoms on a mat of grass, in soil too thin for most things.");
        lang.accept(observe + "plant.locoweed", "Violet flower spikes on bare granite. Nothing else grows close to them. "
                + "I don't know what they are telling me.");
        lang.accept(observe + "link.copper_flower", "Copper again, and the same little blue flower over it. "
                + "I think they go together.");
        lang.accept(observe + "link.horsetail", "Gold in the gravel, and horsetail on the bank above it.");
        lang.accept(observe + "link.stunted_birch", "Zinc and lead under the yellow birches. "
                + "Whatever is in that ground stunts the trees.");
        lang.accept(observe + "link.pink_thrift", "Tin under the pink flowers. Same as the last time.");
    }

    private static void study(BiConsumer<String, String> lang, String study) {
        lang.accept(study + "ore", "%s. %s");
        lang.accept(study + "tool.any", "Soft enough for any pick.");
        lang.accept(study + "rock", "%s, a %s rock. Veins in it tend to hold %s.");
        lang.accept(study + "rock.barren", "%s, a %s rock. I have never heard of ore in it.");
        lang.accept(study + "category.igneous_extrusive", "volcanic");
        lang.accept(study + "category.igneous_intrusive", "deep igneous");
        lang.accept(study + "category.metamorphic", "metamorphic");
        lang.accept(study + "category.sedimentary", "sedimentary");
        lang.accept(study + "lead", "I looked the %s over closely. It has something to do with a question I have been asking.");
        lang.accept(study + "plain", "I looked the %s over closely. Nothing I did not already know.");
        lang.accept(study + "nothing_new", "Nothing new about the %s");
        lang.accept(study + "plant.copper_flower", "%s. It grows in patches, and only on some hills. Worth looking under.");
        lang.accept(study + "plant.horsetail", "%s. Likes wet gravel. It takes up more from the ground than most plants.");
        lang.accept(study + "plant.stunted_birch", "%s. Something in the ground is poisoning it.");
        lang.accept(study + "plant.pink_thrift", "%s. It grows where the soil is thin and the rock is not far below.");
        lang.accept(study + "plant.locoweed", "%s. Cattle that eat it go strange. It must be taking something from the rock.");

        lang.accept(study + "bloomery.incomplete", "The %s is not finished. Fire bricks all round, a chimney, and open air "
                + "above it.");
        lang.accept(study + "bloomery.empty", "The %s stands empty. It wants iron ore and charcoal.");
        lang.accept(study + "bloomery.charged", "The %s is charged and waiting for a flame.");
        lang.accept(study + "bloomery.needs_charcoal", "The %s has ore but not enough charcoal to bake it.");
        lang.accept(study + "bloomery.heating", "The %s is warming up. Patience.");
        lang.accept(study + "bloomery.too_cool", "The %s will not get hot enough. More air would make it roar; a bellows, "
                + "perhaps, or a taller chimney.");
        lang.accept(study + "bloomery.burning", "The %s roars. The bloom is growing inside.");
        lang.accept(study + "bloomery.ready", "The %s has burned down. The bloom is ready to pull out.");

        lang.accept(study + "boiler.no_heat", "The %s is cold. It needs a burning firebox under it.");
        lang.accept(study + "boiler.too_cool", "The %s is warm but will not boil. The fire under it needs to be hotter.");
        lang.accept(study + "boiler.no_water", "The %s is dry. Firing it empty would ruin it; it needs water first.");
        lang.accept(study + "boiler.heating", "The %s ticks and creaks as the water heats.");
        lang.accept(study + "boiler.running", "The %s hums. Steam is up.");
        lang.accept(study + "boiler.venting", "The %s is venting: more steam than anything is using.");
        lang.accept(study + "boiler.pipe_too_hot", "The pipe on the %s cannot take steam this hot. It needs a stronger pipe.");
        lang.accept(study + "boiler.low_water", "The %s is running low on water.");
        lang.accept(study + "boiler.dry_firing", "The %s is firing dry! Water, now, before it is ruined.");
    }

    private static void ui(BiConsumer<String, String> lang, String ui) {
        lang.accept(ui + "leads", "Open Leads");
        lang.accept(ui + "notes", "Notes");
        lang.accept(ui + "tab.leads", "Leads and notes");
        lang.accept(ui + "tab.corkboard", "Corkboard");
        lang.accept(ui + "tab.checklist", "Checklist");
        lang.accept(ui + "remember", "Remembered advice: %s");
        lang.accept(ui + "day", "Day %s");
        lang.accept(ui + "page", "%s / %s");
        lang.accept(ui + "no_leads", "Nothing to chase yet. Look around.");
        lang.accept(ui + "all_done", "Every question answered, for now.");
        lang.accept(ui + "no_notes", "The pages are still blank.");
        lang.accept(ui + "corkboard.drag", "Drag to look around");
        lang.accept(ui + "list.comma", ", ");
        lang.accept(ui + "list.and", " and ");
        lang.accept(ui + "chapter", "Chapter %s");
        lang.accept(ui + "chapter.0", "Stone");
        lang.accept(ui + "chapter.1", "Fire and Clay");
        lang.accept(ui + "chapter.2", "Copper and Bronze");
        lang.accept(ui + "chapter.3", "Iron");
        lang.accept(ui + "chapter.4", "Steel and Steam");
        lang.accept(ui + "chapter.5", "Electricity");
        lang.accept(ui + "chapter.6", "Industry");
        lang.accept(ui + "toast.lead", "New lead");
        lang.accept(ui + "toast.closed", "Crossed off");
        lang.accept(ui + "toast.remember", "Something comes to mind");
        lang.accept(ui + "toast.note", "Noted");
        lang.accept(ui + "toast.chapter", "A new chapter");
        lang.accept(ui + "toast.reminder", "Still on my mind");
        lang.accept(ui + "toast.more_heading", "Journal");
        lang.accept(ui + "toast.more", "%s more entries written");
    }
}
