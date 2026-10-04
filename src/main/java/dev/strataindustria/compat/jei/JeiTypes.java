package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.machine.SawingRecipe;
import dev.strataindustria.quern.QuernRecipe;
import dev.strataindustria.roasting.RoastingRecipe;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.WeldingRecipe;
import dev.strataindustria.tanning.BarrelRecipe;
import dev.strataindustria.washing.WashingRecipe;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;

/** The JEI recipe types of the mod's pages. Data recipes keep their holder; code processes use display records. */
public final class JeiTypes {
    public static final IRecipeHolderType<KnappingRecipe> KNAPPING = IRecipeHolderType.create(StrataIndustria.id("knapping"));
    /** Clay forming shares the knapping recipe type; its own page sorts clay recipes out of knapping. */
    public static final IRecipeHolderType<KnappingRecipe> CLAY_FORMING = IRecipeHolderType.create(StrataIndustria.id("clay_forming"));
    public static final IRecipeHolderType<QuernRecipe> QUERN = IRecipeHolderType.create(StrataIndustria.id("quern"));
    public static final IRecipeHolderType<SawingRecipe> SAWING = IRecipeHolderType.create(StrataIndustria.id("sawing"));
    public static final IRecipeHolderType<WashingRecipe> WASHING = IRecipeHolderType.create(StrataIndustria.id("washing"));
    public static final IRecipeHolderType<BarrelRecipe> BARREL = IRecipeHolderType.create(StrataIndustria.id("barrel"));
    public static final IRecipeHolderType<AnvilRecipe> ANVIL = IRecipeHolderType.create(StrataIndustria.id("anvil"));
    public static final IRecipeHolderType<WeldingRecipe> WELDING = IRecipeHolderType.create(StrataIndustria.id("welding"));
    public static final IRecipeHolderType<RoastingRecipe> ROASTING = IRecipeHolderType.create(StrataIndustria.id("roasting"));

    public static final IRecipeType<Processes.Firing> PIT_KILN = IRecipeType.create(StrataIndustria.id("pit_kiln"), Processes.Firing.class);
    public static final IRecipeType<Processes.CharcoalPit> CHARCOAL_PIT = IRecipeType.create(StrataIndustria.id("charcoal_pit"), Processes.CharcoalPit.class);
    public static final IRecipeType<Processes.Alloying> ALLOYING = IRecipeType.create(StrataIndustria.id("alloying"), Processes.Alloying.class);
    public static final IRecipeType<Processes.Casting> CASTING = IRecipeType.create(StrataIndustria.id("casting"), Processes.Casting.class);
    public static final IRecipeType<Processes.Bloomery> BLOOMERY = IRecipeType.create(StrataIndustria.id("bloomery"), Processes.Bloomery.class);
    public static final IRecipeType<Processes.Coking> COKING = IRecipeType.create(StrataIndustria.id("coking"), Processes.Coking.class);

    private JeiTypes() {}
}
