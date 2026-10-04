package dev.strataindustria.electric.machine;

import dev.strataindustria.ceramics.KilnFiring;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.processing.Processing;
import dev.strataindustria.registry.Tier4Recipes;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.roasting.RoastingRecipe;
import dev.strataindustria.tanning.FluidAmount;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * The electric furnace (spec 10.2), at 8 J/t. In order of precedence it roasts (recipes needing at most
 * 1000 °C, in half their ticks, with the gas piped out of the back or vented), fires clay like a kiln
 * (200 ticks) and smelts like a vanilla furnace (100 ticks). It never melts metal.
 */
public class ElectricFurnaceBlockEntity extends ElectricMachineBlockEntity implements FluidPort {
    public static final int MAX_ROASTING_TEMPERATURE = 1000;
    public static final float FIRING_TICKS = 200.0f, SMELTING_TICKS = 100.0f, ROASTING_FACTOR = 0.5f;
    public static final ElectricStats STATS = ElectricStats.standard(8);
    /** The pale yellow fume of vented roasting gas, as the forge shows it. */
    private static final int FUME_COLOUR = 0xE2D46E;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.ELECTRIC_FURNACE.get(), ElectricMachineLayout.ELECTRIC_FURNACE, STATS, pos, state);
    }

    @Override
    protected Optional<Operation> operation(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        var roasting = level.recipeAccess().getRecipeFor(Tier4Recipes.ROASTING.get(), recipeInput, level)
                .filter(r -> r.value().minTemperature() <= MAX_ROASTING_TEMPERATURE);
        if (roasting.isPresent()) {
            RoastingRecipe recipe = roasting.get().value();
            return Optional.of(new Operation(new Processing(recipe.assemble(recipeInput).isEmpty() ? List.of() : List.of(recipe.assemble(recipeInput)), List.of()),
                    recipe.ticks() * ROASTING_FACTOR, recipe.gas()));
        }
        if (KilnFiring.isFireable(input)) {
            return Optional.of(new Operation(new Processing(List.of(KilnFiring.fire(input.copyWithCount(1))), List.of()), FIRING_TICKS));
        }
        return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, level)
                .map(smelting -> new Operation(new Processing(List.of(smelting.value().assemble(recipeInput)), List.of()), SMELTING_TICKS));
    }

    @Override
    protected void finished(ServerLevel level, Operation operation) {
        operation.gas().ifPresent(gas -> release(level, gas));
    }

    /** Into the pipe or tank on the back face; what it cannot take rises from the top as fume. */
    private void release(ServerLevel level, FluidAmount gas) {
        Direction back = back();
        Fluid fluid = gas.fluid();
        int moved = FluidPipes.push(level, FluidPipes.find(level, worldPosition, back), fluid, gas.amount(), 20.0f, 0.0f).moved();
        if (moved >= gas.amount()) return;
        level.sendParticles(new DustParticleOptions(FUME_COLOUR, 1.4f), worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                worldPosition.getZ() + 0.5, 6, 0.25, 0.05, 0.25, 0.01);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                2, 0.2, 0.05, 0.2, 0.01);
        level.playSound(null, worldPosition, Tier4Sounds.ROASTING_SIZZLE.get(), SoundSource.BLOCKS, 0.35f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    private Direction back() {
        return getBlockState().getValue(ElectricMachineBlock.FACING).getOpposite();
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.ELECTRIC_FURNACE_RUN.get();
    }

    /** A pipe on the back takes the roasting gas. */
    @Override
    public boolean connectsFluid(Direction side) {
        return side == back();
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return 0;
    }
}
