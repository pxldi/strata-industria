package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.LargeVesselBlockEntity;
import dev.strataindustria.ceramics.PitKilnBlockEntity;
import dev.strataindustria.fire.FirePitBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StrataIndustria.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FirePitBlockEntity>> FIRE_PIT =
            BLOCK_ENTITIES.register("fire_pit", () -> new BlockEntityType<>(FirePitBlockEntity::new, ModBlocks.FIRE_PIT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PitKilnBlockEntity>> PIT_KILN =
            BLOCK_ENTITIES.register("pit_kiln", () -> new BlockEntityType<>(PitKilnBlockEntity::new, ModBlocks.PIT_KILN.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeVesselBlockEntity>> LARGE_VESSEL =
            BLOCK_ENTITIES.register("large_vessel", () -> new BlockEntityType<>(LargeVesselBlockEntity::new, ModBlocks.LARGE_VESSEL.get()));

    private ModBlockEntities() {}
}
