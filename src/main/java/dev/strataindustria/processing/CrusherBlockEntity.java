package dev.strataindustria.processing;

import dev.strataindustria.journal.Journal;
import dev.strataindustria.quern.QuernBlockEntity;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The crusher (tier 4 spec 11.2): three inputs crushed side by side, 80 ticks each at 16 RPM. It runs
 * every crushing recipe, with their chance outputs, and every quern recipe the crushing ones do not cover.
 */
public class CrusherBlockEntity extends ProcessingBlockEntity {
    public static final int IMPACT = 8, MIN_SPEED = 16;
    public static final float BASE_TICKS = 80.0f;

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.CRUSHER.get(), MachineLayout.CRUSHER, pos, state);
    }

    @Override
    protected Optional<Processing> processing(ServerLevel level, ItemStack input) {
        var crushing = CrushingRecipe.recipeFor(level, input);
        if (crushing.isPresent()) return Optional.of(crushing.get().value().processing());
        return QuernBlockEntity.recipeFor(level, input)
                .map(quern -> new Processing(List.of(quern.value().assemble(new SingleRecipeInput(input))), List.of()));
    }

    @Override
    protected float baseTicks() {
        return BASE_TICKS;
    }

    @Override
    protected SoundEvent workSound() {
        return Tier4Sounds.CRUSHER_CRUSH.get();
    }

    @Override
    protected String journalGoal() {
        return Journal.CRUSHER;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }
}
