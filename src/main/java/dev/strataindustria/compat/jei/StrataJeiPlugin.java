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
import dev.strataindustria.smithing.AnvilBlock;
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
                new WeldingCategory(gui),
                new BloomeryCategory(gui),
                new SawingCategory(gui),
                new WashingCategory(gui),
                new BarrelCategory(gui),
                new RoastingCategory(gui),
                new CokingCategory(gui),
                new FireboxFuelCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IngredientStacks.clear();
        List<RecipeHolder<KnappingRecipe>> knapping = ClientRecipes.byType(ModRecipes.KNAPPING.get());
        registration.addRecipes(JeiTypes.KNAPPING, knapping.stream().filter(h -> !KnappingCategory.isClay(h.value())).toList());
        registration.addRecipes(JeiTypes.CLAY_FORMING, knapping.stream().filter(h -> KnappingCategory.isClay(h.value())).toList());
        registration.addRecipes(JeiTypes.QUERN, ClientRecipes.byType(ModRecipes.QUERN.get()));
        registration.addRecipes(JeiTypes.ANVIL, ClientRecipes.byType(ModRecipes.ANVIL.get()));
        registration.addRecipes(JeiTypes.WELDING, ClientRecipes.byType(ModRecipes.WELDING.get()));
        registration.addRecipes(JeiTypes.SAWING, ClientRecipes.byType(ModRecipes.SAWING.get()));
        registration.addRecipes(JeiTypes.WASHING, ClientRecipes.byType(ModRecipes.WASHING.get()));
        registration.addRecipes(JeiTypes.BARREL, ClientRecipes.byType(ModRecipes.BARREL.get()));
        registration.addRecipes(JeiTypes.ROASTING, ClientRecipes.byType(Tier4Recipes.ROASTING.get()));

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
        registration.addCraftingStation(JeiTypes.PIT_KILN, ModItems.STRAW.get(), Tier4Items.KILN.get());
        registration.addCraftingStation(JeiTypes.CHARCOAL_PIT, ModItems.FIRESTARTER.get());
        registration.addCraftingStation(JeiTypes.QUERN, ModItems.QUERN.get(), ModItems.MILLSTONE.get());
        registration.addCraftingStation(JeiTypes.ALLOYING, ModItems.CRUCIBLE.get(), Tier4Items.REFRACTORY_CRUCIBLE.get(), ModItems.FORGE.get());
        registration.addCraftingStation(JeiTypes.CASTING, ModItems.CRUCIBLE.get(), Tier4Items.REFRACTORY_CRUCIBLE.get());
        Item[] anvils = BuiltInRegistries.ITEM.stream()
                .filter(i -> i instanceof BlockItem block && block.getBlock() instanceof AnvilBlock).toArray(Item[]::new);
        registration.addCraftingStation(JeiTypes.ANVIL, anvils);
        registration.addCraftingStation(JeiTypes.ANVIL, ModItems.TRIP_HAMMER.get());
        registration.addCraftingStation(JeiTypes.WELDING, anvils);
        registration.addCraftingStation(JeiTypes.BLOOMERY, ModItems.BLOOMERY.get(), ModItems.BELLOWS.get());
        registration.addCraftingStation(JeiTypes.SAWING, ModItems.SAW_MILL.get());
        registration.addCraftingStation(JeiTypes.WASHING, ModItems.WASHING_PAN.get(), ModItems.SLUICE.get());
        registration.addCraftingStation(JeiTypes.BARREL, ModItems.SOAKING_BARREL.get());
        registration.addCraftingStation(JeiTypes.ROASTING, ModItems.FORGE.get(), Tier4Items.ROASTER.get());
        registration.addCraftingStation(JeiTypes.COKING, Tier4Items.COKE_OVEN_DOOR.get());
        registration.addCraftingStation(JeiTypes.FIREBOX_FUEL, Tier4Items.FIREBOX.get());
        // The fire pit cooks vanilla campfire recipes.
        registration.addCraftingStation(RecipeTypes.CAMPFIRE_COOKING, ModItems.FIRE_PIT.get());
    }
}
