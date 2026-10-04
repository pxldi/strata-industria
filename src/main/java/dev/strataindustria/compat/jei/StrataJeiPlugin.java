package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.compat.ClientRecipes;
import dev.strataindustria.compat.recipeview.IngredientStacks;
import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Recipes;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.registry.Tier5Recipes;
import dev.strataindustria.smithing.AnvilBlock;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.WeldingRecipe;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Recipe pages for JEI. JEI finds this class by its annotation; nothing else in the mod refers to this
 * package, so the mod runs without JEI.
 *
 * <p>To add a page: give it a type in {@link JeiTypes}, write a {@link StrataCategory}, then register the
 * category, its recipes and its stations below. Data recipes come from {@link ClientRecipes} (every type in
 * {@code ModRecipes.TYPES} is synced); code processes get a display record in {@code compat/recipeview}.
 */
@JeiPlugin
public final class StrataJeiPlugin implements IModPlugin {
    private static final Identifier UID = StrataIndustria.id("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new KnappingCategory(gui, false),
                new KnappingCategory(gui, true),
                new PitKilnCategory(gui),
                new CharcoalPitCategory(gui),
                new QuernCategory(gui),
                new AlloyingCategory(gui),
                new CastingCategory(gui),
                new AnvilCategory(gui),
                new BloomeryCategory(gui),
                new SawingCategory(gui),
                new WashingCategory(gui),
                new BarrelCategory(gui),
                new RoastingCategory(gui),
                new CokingCategory(gui),
                new FireboxFuelCategory(gui),
                new CrushingCategory(gui),
                new MachiningCategory(gui),
                new ChemicalCategory<>(JeiTypes.MIXING, "mixing", Tier5Items.MIXER.get(), gui, 66),
                new ChemicalCategory<>(JeiTypes.ELECTROLYSIS, "electrolysis", Tier5Items.ELECTROLYSER.get(), gui, 66),
                new ChemicalCategory<>(JeiTypes.ASSEMBLING, "assembling", Tier5Items.ASSEMBLER.get(), gui, 84),
                new ChemicalCategory<>(JeiTypes.EXTRUDING, "extruding", Tier5Items.EXTRUDER.get(), gui, 66),
                new OilStillCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IngredientStacks.clear();
        List<RecipeHolder<KnappingRecipe>> knapping = ClientRecipes.byType(ModRecipes.KNAPPING.get());
        registration.addRecipes(JeiTypes.KNAPPING, knapping.stream().filter(h -> !KnappingCategory.isClay(h.value())).toList());
        registration.addRecipes(JeiTypes.CLAY_FORMING, knapping.stream().filter(h -> KnappingCategory.isClay(h.value())).toList());
        registration.addRecipes(JeiTypes.QUERN, ClientRecipes.byType(ModRecipes.QUERN.get()));
        List<Processes.AnvilWork> anvilWork = new java.util.ArrayList<>();
        for (RecipeHolder<AnvilRecipe> h : ClientRecipes.byType(ModRecipes.ANVIL.get())) {
            AnvilRecipe r = h.value();
            anvilWork.add(new Processes.AnvilWork(IngredientStacks.of(r.ingredient(), r.count()), List.of(), r.result().create(), r.blows()));
        }
        for (RecipeHolder<WeldingRecipe> h : ClientRecipes.byType(ModRecipes.WELDING.get())) {
            WeldingRecipe r = h.value();
            anvilWork.add(new Processes.AnvilWork(IngredientStacks.of(r.first(), 1), IngredientStacks.of(r.second(), 1), r.result().create(),
                    dev.strataindustria.smithing.Smithing.WELD_BLOWS));
        }
        registration.addRecipes(JeiTypes.ANVIL, anvilWork);
        registration.addRecipes(JeiTypes.SAWING, ClientRecipes.byType(ModRecipes.SAWING.get()));
        registration.addRecipes(JeiTypes.WASHING, ClientRecipes.byType(ModRecipes.WASHING.get()));
        registration.addRecipes(JeiTypes.BARREL, ClientRecipes.byType(ModRecipes.BARREL.get()));
        registration.addRecipes(JeiTypes.ROASTING, ClientRecipes.byType(Tier4Recipes.ROASTING.get()));
        registration.addRecipes(JeiTypes.CRUSHING, ClientRecipes.byType(Tier4Recipes.CRUSHING.get()));
        registration.addRecipes(JeiTypes.MACHINING, ClientRecipes.byType(Tier5Recipes.MACHINING.get()));
        registration.addRecipes(JeiTypes.MIXING, ClientRecipes.byType(Tier5Recipes.MIXING.get()));
        registration.addRecipes(JeiTypes.ELECTROLYSIS, ClientRecipes.byType(Tier5Recipes.ELECTROLYSIS.get()));
        registration.addRecipes(JeiTypes.ASSEMBLING, ClientRecipes.byType(Tier5Recipes.ASSEMBLING.get()));
        registration.addRecipes(JeiTypes.EXTRUDING, ClientRecipes.byType(Tier5Recipes.EXTRUDING.get()));
        registration.addRecipes(JeiTypes.OIL_STILL, ClientRecipes.byType(dev.strataindustria.registry.Tier6Recipes.OIL_STILL.get()));

        // Hand work with no screen: how bark becomes cord and cord becomes cloth (redesign R3).
        registration.addItemStackInfo(ModItems.BARK.get().getDefaultInstance(), net.minecraft.network.chat.Component.translatable("jei.strataindustria.info.bark"));
        registration.addItemStackInfo(ModItems.CORD.get().getDefaultInstance(), net.minecraft.network.chat.Component.translatable("jei.strataindustria.info.cord"));
        registration.addItemStackInfo(ModItems.BARK_CLOTH.get().getDefaultInstance(), net.minecraft.network.chat.Component.translatable("jei.strataindustria.info.bark_cloth"));
        registration.addItemStackInfo(ModItems.STRAW.get().getDefaultInstance(), net.minecraft.network.chat.Component.translatable("jei.strataindustria.info.straw"));

        registration.addRecipes(JeiTypes.PIT_KILN, Processes.kilnFiring());
        registration.addRecipes(JeiTypes.CHARCOAL_PIT, Processes.charcoalPit());
        registration.addRecipes(JeiTypes.ALLOYING, Processes.alloying());
        registration.addRecipes(JeiTypes.CASTING, Processes.casting());
        registration.addRecipes(JeiTypes.BLOOMERY, Processes.bloomery());
        registration.addRecipes(JeiTypes.COKING, Processes.coking());
        registration.addRecipes(JeiTypes.FIREBOX_FUEL, Processes.fireboxFuel());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(JeiTypes.PIT_KILN, ModItems.STRAW.get(), dev.strataindustria.registry.PrologueRegistry.BRICK_KILN_ITEM.get(), Tier4Items.KILN.get());
        registration.addCraftingStation(JeiTypes.CHARCOAL_PIT, net.minecraft.world.item.Items.FLINT);
        registration.addCraftingStation(JeiTypes.QUERN, ModItems.QUERN.get(), ModItems.MILLSTONE.get());
        registration.addCraftingStation(JeiTypes.ALLOYING, ModItems.CRUCIBLE.get(), Tier4Items.REFRACTORY_CRUCIBLE.get(), Tier4Items.SMELTER.get(), ModItems.FORGE.get());
        registration.addCraftingStation(JeiTypes.CASTING, ModItems.CRUCIBLE.get(), Tier4Items.REFRACTORY_CRUCIBLE.get(), Tier4Items.SMELTER.get());
        Item[] anvils = BuiltInRegistries.ITEM.stream()
                .filter(i -> i instanceof BlockItem block && block.getBlock() instanceof AnvilBlock).toArray(Item[]::new);
        registration.addCraftingStation(JeiTypes.ANVIL, anvils);
        registration.addCraftingStation(JeiTypes.ANVIL, ModItems.TRIP_HAMMER.get(), Tier4Items.STEAM_HAMMER.get());
        registration.addCraftingStation(JeiTypes.BLOOMERY, ModItems.BLOOMERY.get(), ModItems.BELLOWS.get());
        registration.addCraftingStation(JeiTypes.SAWING, ModItems.SAW_MILL.get());
        registration.addCraftingStation(JeiTypes.WASHING, ModItems.WASHING_PAN.get(), ModItems.SLUICE.get());
        registration.addCraftingStation(JeiTypes.BARREL, ModItems.SOAKING_BARREL.get(), Tier5Items.MIXER.get());
        registration.addCraftingStation(JeiTypes.ROASTING, ModItems.FORGE.get(), Tier4Items.ROASTER.get(), Tier5Items.ELECTRIC_FURNACE.get());
        registration.addCraftingStation(JeiTypes.CRUSHING, Tier4Items.CRUSHER.get(), Tier5Items.MACERATOR.get());
        registration.addCraftingStation(JeiTypes.MACHINING, Tier5Items.WIREMILL.get(), Tier5Items.BENDER.get(), Tier5Items.LATHE.get());
        registration.addCraftingStation(JeiTypes.MIXING, Tier5Items.MIXER.get());
        registration.addCraftingStation(JeiTypes.ELECTROLYSIS, Tier5Items.ELECTROLYSER.get());
        registration.addCraftingStation(JeiTypes.ASSEMBLING, Tier5Items.ASSEMBLER.get());
        registration.addCraftingStation(JeiTypes.EXTRUDING, Tier5Items.EXTRUDER.get());
        registration.addCraftingStation(JeiTypes.OIL_STILL, dev.strataindustria.registry.Tier6Items.OIL_STILL.get());
        registration.addCraftingStation(JeiTypes.COKING, Tier4Items.COKE_OVEN_DOOR.get());
        registration.addCraftingStation(JeiTypes.FIREBOX_FUEL, Tier4Items.FIREBOX.get());
        // The fire pit cooks vanilla campfire recipes.
        registration.addCraftingStation(RecipeTypes.CAMPFIRE_COOKING, ModItems.FIRE_PIT.get());
    }
}
