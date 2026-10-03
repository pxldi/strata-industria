package dev.strataindustria.registry;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.crafting.ConfigCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, StrataIndustria.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ConfigCondition>> CONFIG =
            CONDITIONS.register("config", () -> ConfigCondition.CODEC);

    private ModConditions() {}
}
