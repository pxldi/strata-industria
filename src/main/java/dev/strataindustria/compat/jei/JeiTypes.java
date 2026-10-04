package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.machine.SawingRecipe;
import dev.strataindustria.processing.AssemblingRecipe;
import dev.strataindustria.processing.CrushingRecipe;
import dev.strataindustria.processing.ElectrolysisRecipe;
import dev.strataindustria.processing.ExtrudingRecipe;
import dev.strataindustria.processing.MachiningRecipe;
import dev.strataindustria.processing.MixingRecipe;
import dev.strataindustria.processing.OilStillRecipe;
import dev.strataindustria.quern.QuernRecipe;
import dev.strataindustria.roasting.RoastingRecipe;
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
    public static final IRecipeHolderType<RoastingRecipe> ROASTING = IRecipeHolderType.create(StrataIndustria.id("roasting"));
    public static final IRecipeHolderType<CrushingRecipe> CRUSHING = IRecipeHolderType.create(StrataIndustria.id("crushing"));
    public static final IRecipeHolderType<MachiningRecipe> MACHINING = IRecipeHolderType.create(StrataIndustria.id("machining"));
    public static final IRecipeHolderType<MixingRecipe> MIXING = IRecipeHolderType.create(StrataIndustria.id("mixing"));
    public static final IRecipeHolderType<ElectrolysisRecipe> ELECTROLYSIS = IRecipeHolderType.create(StrataIndustria.id("electrolysis"));
    public static final IRecipeHolderType<AssemblingRecipe> ASSEMBLING = IRecipeHolderType.create(StrataIndustria.id("assembling"));
    public static final IRecipeHolderType<ExtrudingRecipe> EXTRUDING = IRecipeHolderType.create(StrataIndustria.id("extruding"));

    public static final IRecipeHolderType<OilStillRecipe> OIL_STILL = IRecipeHolderType.create(StrataIndustria.id("oil_still"));

    /** Striking and welding share one page: both are work at the anvil. */
    public static final IRecipeType<Processes.AnvilWork> ANVIL = IRecipeType.create(StrataIndustria.id("anvil"), Processes.AnvilWork.class);
    public static final IRecipeType<Processes.Firing> FIRING = IRecipeType.create(StrataIndustria.id("firing"), Processes.Firing.class);
    public static final IRecipeType<Processes.CharcoalPit> CHARCOAL_PIT = IRecipeType.create(StrataIndustria.id("charcoal_pit"), Processes.CharcoalPit.class);
    public static final IRecipeType<Processes.Alloying> ALLOYING = IRecipeType.create(StrataIndustria.id("alloying"), Processes.Alloying.class);
    public static final IRecipeType<Processes.Casting> CASTING = IRecipeType.create(StrataIndustria.id("casting"), Processes.Casting.class);
    public static final IRecipeType<Processes.Bloomery> BLOOMERY = IRecipeType.create(StrataIndustria.id("bloomery"), Processes.Bloomery.class);
    public static final IRecipeType<Processes.Coking> COKING = IRecipeType.create(StrataIndustria.id("coking"), Processes.Coking.class);

    public static final IRecipeType<Processes.FireboxFuel> FIREBOX_FUEL = IRecipeType.create(StrataIndustria.id("firebox_fuel"), Processes.FireboxFuel.class);

    private JeiTypes() {}
}
