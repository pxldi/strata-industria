package dev.strataindustria.registry;

import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.LargeVesselBlockEntity;
import dev.strataindustria.ceramics.PitKilnBlockEntity;
import dev.strataindustria.charcoal.LogPileBlockEntity;
import dev.strataindustria.fire.FirePitBlockEntity;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.quern.QuernBlockEntity;
import dev.strataindustria.smithing.AnvilBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogPileBlockEntity>> LOG_PILE =
            BLOCK_ENTITIES.register("log_pile", () -> new BlockEntityType<>(LogPileBlockEntity::new, ModBlocks.LOG_PILE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeBlockEntity>> FORGE =
            BLOCK_ENTITIES.register("forge", () -> new BlockEntityType<>(ForgeBlockEntity::new, ModBlocks.FORGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuernBlockEntity>> QUERN =
            BLOCK_ENTITIES.register("quern", () -> new BlockEntityType<>(QuernBlockEntity::new, ModBlocks.QUERN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnvilBlockEntity>> ANVIL =
            BLOCK_ENTITIES.register("anvil", () -> {
                java.util.List<net.minecraft.world.level.block.Block> anvils = new java.util.ArrayList<>();
                ModBlocks.STONE_ANVILS.values().forEach(b -> anvils.add(b.get()));
                anvils.add(ModBlocks.BRONZE_ANVIL.get());
                return new BlockEntityType<>(AnvilBlockEntity::new, anvils.toArray(new net.minecraft.world.level.block.Block[0]));
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible", () -> new BlockEntityType<>(CrucibleBlockEntity::new, ModBlocks.CRUCIBLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloomeryBlockEntity>> BLOOMERY =
            BLOCK_ENTITIES.register("bloomery", () -> new BlockEntityType<>(BloomeryBlockEntity::new, ModBlocks.BLOOMERY.get()));

    private ModBlockEntities() {}
}
