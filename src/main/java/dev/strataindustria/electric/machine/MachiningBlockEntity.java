package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.processing.MachiningRecipe;
import dev.strataindustria.processing.Processing;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A shaping machine (spec 10.4 to 10.6): runs the {@code machining} recipes written for it, 100 LV ticks
 * for each item. Subclasses name the machine and, if it has a mode button, the current mode.
 */
public abstract class MachiningBlockEntity extends ElectricMachineBlockEntity {
    public static final float BASE_TICKS = 100.0f;

    protected MachiningBlockEntity(BlockEntityType<?> type, ElectricMachineLayout layout, ElectricStats stats, BlockPos pos, BlockState state) {
        super(type, layout, stats, pos, state);
    }

    /** The mode recipes are matched against; empty for a machine without modes. */
    protected String mode() {
        return "";
    }

    @Override
    protected Optional<Operation> operation(ServerLevel level, ItemStack input) {
        return MachiningRecipe.recipeFor(level, layout.id(), mode(), input)
                .map(holder -> new Operation(new Processing(List.of(holder.value().result().create()), List.of()), BASE_TICKS));
    }

    /** A few sparks of the metal being worked, spat out of the front. */
    @Override
    protected void running(ServerLevel level, ItemStack shown) {
        if (level.getGameTime() % 6 != 0 || shown.isEmpty()) return;
        BlockPos pos = getBlockPos();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown.getItem()), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                1, 0.2, 0.02, 0.2, 0.03);
    }
}
