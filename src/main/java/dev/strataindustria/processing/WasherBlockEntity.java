package dev.strataindustria.processing;

import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.washing.WashingRecipe;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The washer (tier 4 spec 11.3): the washing pan and sluice on a shaft. Two inputs washed side by side,
 * 40 ticks each at 16 RPM, using 100 mB of piped water per item from a 4000 mB tank. No flowing water
 * needed. Sulfide ore has no washing recipe, so it is refused.
 */
public class WasherBlockEntity extends ProcessingBlockEntity implements FluidPort {
    public static final int IMPACT = 4, MIN_SPEED = 8;
    public static final float BASE_TICKS = 40.0f;
    public static final int TANK = 4000, WATER_PER_ITEM = 100;

    private int water;

    public WasherBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.WASHER.get(), MachineLayout.WASHER, pos, state);
    }

    @Override
    protected Optional<Processing> processing(ServerLevel level, ItemStack input) {
        return WashingRecipe.recipeFor(level, input)
                .map(recipe -> new Processing(List.of(recipe.value().result().create()), recipe.value().chances()));
    }

    @Override
    protected @Nullable Status blocked(ItemStack input) {
        return water < WATER_PER_ITEM ? Status.NO_WATER : null;
    }

    @Override
    protected void used() {
        water -= WATER_PER_ITEM;
    }

    @Override
    protected int extraData(int index) {
        return index == 0 ? water : 0;
    }

    public int water() {
        return water;
    }

    @Override
    protected float baseTicks() {
        return BASE_TICKS;
    }

    @Override
    protected SoundEvent workSound() {
        return Tier4Sounds.WASHER_WASH.get();
    }

    @Override
    protected @Nullable String journalGoal() {
        return null;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    // ------------------------------------------------------------------ water

    /** Water comes in through a pipe on the sides or the back. */
    @Override
    public boolean connectsFluid(Direction side) {
        return side.getAxis().isHorizontal() && side != getBlockState().getValue(ProcessingBlock.FACING);
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (!connectsFluid(side) || !fluid.isSame(Fluids.WATER) || amount <= 0) return 0;
        int take = Math.min(amount, TANK - water);
        if (take > 0 && !simulate) {
            water += take;
            setChanged();
        }
        return Math.max(0, take);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        water = in.getIntOr("water", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("water", water);
    }
}
