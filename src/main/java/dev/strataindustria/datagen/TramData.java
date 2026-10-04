package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.transport.rail.TrolleyBracketBlock;
import dev.strataindustria.transport.rail.TramRegistry;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** Models, text and recipes for the trolley wire and the electric tram (outposts and transport spec 9.2 and 9.3): {@link TramRegistry}. */
final class TramData {
    private TramData() {}

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // The bracket's arm is hand-modelled pointing north; the blockstate turns it.
        Identifier bracket = StrataIndustria.id("block/trolley_bracket");
        MultiVariant north = BlockModelGenerators.plainVariant(bracket);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(TramRegistry.TROLLEY_BRACKET.get()).with(
                PropertyDispatch.initial(TrolleyBracketBlock.FACING)
                        .select(Direction.NORTH, north)
                        .select(Direction.EAST, north.with(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.SOUTH, north.with(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.WEST, north.with(BlockModelGenerators.Y_ROT_270))));
        blockModels.itemModelOutput.accept(TramRegistry.TROLLEY_BRACKET_ITEM.get(), ItemModelUtils.plainModel(bracket));
        itemModels.generateFlatItem(TramRegistry.TROLLEY_WIRE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(TramRegistry.ELECTRIC_TRAM.get(), ModelTemplates.FLAT_ITEM);
    }

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".trolley_bracket", "Trolley Bracket");
        add.accept("item." + id + ".trolley_wire", "Trolley Wire");
        add.accept("item." + id + ".electric_tram", "Electric Tram");
        add.accept("entity." + id + ".electric_tram", "Electric Tram");

        add.accept(id + ".trolley.started", "Wire clamped. Use it on the next bracket.");
        add.accept(id + ".trolley.joined", "Wire strung, %s used.");
        add.accept(id + ".trolley.not_bracket", "That is not a trolley bracket.");
        add.accept(id + ".trolley.already", "Already strung.");
        add.accept(id + ".trolley.too_far", "Too far. %s blocks at most.");
        add.accept(id + ".trolley.steep", "Too steep for the wire.");
        add.accept(id + ".trolley.full", "That bracket holds three wires.");
        add.accept(id + ".trolley.blocked", "Something is in the way.");
        add.accept(id + ".trolley.needs", "Needs %s wire.");

        add.accept(id + ".tram.hud", "Throttle %s  Charge %s%%  Wire %s");
        add.accept(id + ".tram.wire.none", "none");
        add.accept(id + ".tram.wire.live", "live");
        add.accept(id + ".tram.wire.dead", "dead");
        add.accept(id + ".tram.wire.too_strong", "too strong");
        add.accept(id + ".tram.wire.too_far", "too far");
        add.accept(id + ".tram.no_power", "No power in the wire.");
        add.accept(id + ".tram.too_strong", "The wire carries MV. A tram wants LV.");
        add.accept(id + ".tram.lost_wire", "Lost the wire.");
        add.accept(id + ".tram.parked", "Brakes on.");
        add.accept(id + ".tram.released", "Brakes off.");
        add.accept(id + ".tram.leads", "The tram leads. Couple the wagons behind it.");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "tram.motor", "Tram hums");
        add.accept(subtitles + "tram.spark", "Wire crackles");
        add.accept(subtitles + "tram.bell", "Bell rings");
        add.accept(subtitles + "tram.contact", "Pole clicks onto the wire");
        add.accept(subtitles + "tram.lose_wire", "Pole slips off the wire");
        add.accept(subtitles + "trolley_wire.strung", "Wire clamps");
        add.accept(subtitles + "trolley_wire.cut", "Wire drops");

        String journal = "journal." + id + ".";
        add.accept(journal + "t5.tram", "Run a Tram");
        add.accept(journal + "t5.tram.hint", "Brackets on the poles, wire between them, a tram under the wire. 256 blocks on wire power alone.");
        add.accept(journal + "t5.tram.lead", "A steam engine wants water and coal at every stop.");
        add.accept(journal + "t5.tram.note", "Ran 256 blocks off the wire. Quiet, and quicker than the engine.");
    }

    static final class Recipes extends net.minecraft.data.recipes.RecipeProvider {
        Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
        }

        private static ResourceKey<Recipe<?>> key(String path) {
            return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
        }

        @Override
        protected void buildRecipes() {
            Item plate = ModItems.PLATES.get(Metal.STEEL).get();
            Item rod = ModItems.RODS.get(Metal.STEEL).get();
            shaped(RecipeCategory.TRANSPORTATION, TramRegistry.TROLLEY_BRACKET_ITEM.get(), 2)
                    .pattern("E  ")
                    .pattern("EDD")
                    .pattern("E  ")
                    .define('E', plate)
                    .define('D', rod)
                    .unlockedBy("has_utility_pole", has(Tier5Items.UTILITY_POLE.get()))
                    .save(output, key("trolley_bracket"));
            shaped(RecipeCategory.TRANSPORTATION, TramRegistry.ELECTRIC_TRAM.get())
                    .pattern("EGE")
                    .pattern("MhM")
                    .pattern("DcD")
                    .define('E', plate)
                    .define('G', Items.GLASS_PANE)
                    .define('M', Tier5Items.ELECTRIC_MOTOR.get())
                    .define('h', Tier5Items.LV_MACHINE_HULL.get())
                    .define('D', rod)
                    .define('c', Tier5Items.BASIC_CIRCUIT.get())
                    .unlockedBy("has_electric_motor", has(Tier5Items.ELECTRIC_MOTOR.get()))
                    .save(output, key("electric_tram"));
        }
    }
}
