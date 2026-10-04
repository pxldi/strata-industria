package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Spec 23.7: a slow drop of latex falling from a tree tap's spout into its bowl. */
public final class Tier5Particles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, StrataIndustria.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LATEX_DRIP = PARTICLES.register("latex_drip",
            () -> new SimpleParticleType(false));

    private Tier5Particles() {}
}
