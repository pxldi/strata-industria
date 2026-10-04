package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.processing.CrushingRecipe;
import dev.strataindustria.processing.Processing;
import dev.strataindustria.quern.QuernBlockEntity;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.washing.WashingRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The macerator (spec 10.3): every crushing and quern recipe, crushing first, at 100 LV ticks and 16 J/t.
 * Its grinding wheels are finer than the crusher's jaws: an ore piece's second crushed piece comes up
 * 25% of the time instead of 10%; byproducts keep their crusher chance.
 */
public class MaceratorBlockEntity extends ElectricMachineBlockEntity {
    public static final float BASE_TICKS = 100.0f, SECOND_PIECE = 0.25f;
    public static final ElectricStats STATS = ElectricStats.standard(16);

    public MaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.MACERATOR.get(), ElectricMachineLayout.MACERATOR, STATS, pos, state);
    }

    @Override
    protected Optional<Operation> operation(ServerLevel level, ItemStack input) {
        var crushing = CrushingRecipe.recipeFor(level, input, tier());
        if (crushing.isPresent()) return Optional.of(new Operation(maceration(crushing.get().value()), BASE_TICKS));
        return QuernBlockEntity.recipeFor(level, input)
                .map(quern -> new Operation(new Processing(List.of(quern.value().assemble(new SingleRecipeInput(input))), List.of()), BASE_TICKS));
    }

    /** The crushing recipe with its second-piece chance raised to the macerator's. */
    public static Processing maceration(CrushingRecipe recipe) {
        ItemStack main = recipe.result().create();
        List<WashingRecipe.Chance> chances = new ArrayList<>();
        for (WashingRecipe.Chance chance : recipe.chances()) {
            boolean secondPiece = ItemStack.isSameItemSameComponents(chance.item().create(), main);
            chances.add(secondPiece ? new WashingRecipe.Chance(chance.item(), Math.max(chance.chance(), SECOND_PIECE)) : chance);
        }
        return new Processing(List.of(main), chances);
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.MACERATOR_GRIND.get();
    }

    @Override
    protected void running(ServerLevel level, ItemStack shown) {
        if (level.getGameTime() % 4 != 0 || shown.isEmpty()) return;
        BlockPos pos = getBlockPos();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown.getItem()), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                2, 0.2, 0.02, 0.2, 0.04);
    }
}
