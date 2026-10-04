package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Spec 24.7: crude oil drips through the block under a pool, like honey but black. */
public final class Tier6Particles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, StrataIndustria.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRIPPING_OIL = PARTICLES.register("dripping_oil",
            () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FALLING_OIL = PARTICLES.register("falling_oil",
            () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LANDING_OIL = PARTICLES.register("landing_oil",
            () -> new SimpleParticleType(false));

    private Tier6Particles() {}
}
