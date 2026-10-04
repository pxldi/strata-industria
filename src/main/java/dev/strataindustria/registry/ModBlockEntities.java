package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
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

    private ModBlockEntities() {}
}
