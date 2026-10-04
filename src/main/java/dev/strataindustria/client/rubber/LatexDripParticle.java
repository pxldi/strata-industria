package dev.strataindustria.client.rubber;

import dev.strataindustria.registry.Tier5Fluids;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/** Spec 23.7: a drop of latex in the latex ramp, falling slowly from the spout and gone in the bowl. */
public final class LatexDripParticle extends DripParticle {
    /** Latex ramp step 4 (spec 23.1), as a tint. */
    private static final float R = 0xce / 255f, G = 0xc6 / 255f, B = 0xa8 / 255f;

    private LatexDripParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, Tier5Fluids.LATEX.get(), sprite);
        gravity = 0.006f;
        lifetime = 40;
        setColor(R, G, B);
    }

    @Override
    protected void preMoveUpdate() {
        if (lifetime-- <= 0) remove();
    }

    @Override
    protected void postMoveUpdate() {
        if (onGround) remove();
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, xa, ya, za, random) -> new LatexDripParticle(level, x, y, z, sprites.get(random));
    }
}
