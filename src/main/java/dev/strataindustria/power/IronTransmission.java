package dev.strataindustria.power;

import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tier 4 spec 11.7: iron axles and gearboxes. They behave exactly like the wooden ones apart from the
 * speed they can take.
 */
public final class IronTransmission {
    public static final int SPEED_LIMIT = 256;

    private IronTransmission() {}

    /** The shared block entity of the iron parts: a plain kinetic block with the iron speed limit. */
    public static class Entity extends KineticBlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(Tier4BlockEntities.IRON_TRANSMISSION.get(), pos, state);
        }

        @Override
        public int speedLimit() {
            return SPEED_LIMIT;
        }
    }

    public static class Axle extends AxleBlock {
        public Axle(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Entity(pos, state);
        }
    }

    public static class Gearbox extends GearboxBlock {
        public Gearbox(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Entity(pos, state);
        }
    }

    public static class StepUpGearbox extends StepUpGearboxBlock {
        public StepUpGearbox(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Entity(pos, state);
        }
    }
}
