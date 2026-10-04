package dev.strataindustria.client.oil;

import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Spec 24.7: crude oil hanging under a block, falling and splashing, like dripping honey but in the crude
 * oil ramp. Uses the vanilla drip sprites, tinted.
 */
public final class OilDripParticle extends DripParticle {
    private enum Stage { HANG, FALL, LAND }

    /** Crude oil ramp step 3 and 4 (spec 24.1), as tints. */
    private static final float[] HANG = {0x30 / 255f, 0x28 / 255f, 0x28 / 255f};
    private static final float[] FALL = {0x22 / 255f, 0x1c / 255f, 0x1e / 255f};

    private final Stage stage;

    private OilDripParticle(ClientLevel level, double x, double y, double z, Stage stage, TextureAtlasSprite sprite, RandomSource random) {
        super(level, x, y, z, Tier6Fluids.CRUDE_OIL.source().get(), sprite);
        this.stage = stage;
        switch (stage) {
            case HANG -> {
                gravity *= 0.01f;
                lifetime = 100;
                setColor(HANG[0], HANG[1], HANG[2]);
            }
            case FALL -> {
                gravity = 0.01f;
                lifetime = (int) (64.0 / (random.nextFloat() * 0.8 + 0.2));
                setColor(FALL[0], FALL[1], FALL[2]);
            }
            case LAND -> {
                lifetime = (int) (128.0 / (random.nextFloat() * 0.8 + 0.2));
                setColor(FALL[0], FALL[1], FALL[2]);
            }
        }
    }

    @Override
    protected void preMoveUpdate() {
        if (lifetime-- > 0) return;
        remove();
        if (stage == Stage.HANG) level.addParticle(Tier6Particles.FALLING_OIL.get(), x, y, z, xd, yd, zd);
    }

    @Override
    protected void postMoveUpdate() {
        switch (stage) {
            case HANG -> {
                xd *= 0.02;
                yd *= 0.02;
                zd *= 0.02;
            }
            case FALL -> {
                if (onGround) {
                    remove();
                    level.addParticle(Tier6Particles.LANDING_OIL.get(), x, y, z, 0, 0, 0);
                }
            }
            case LAND -> {}
        }
    }

    public static ParticleProvider<SimpleParticleType> hang(SpriteSet sprites) {
        return (type, level, x, y, z, xa, ya, za, random) -> new OilDripParticle(level, x, y, z, Stage.HANG, sprites.get(random), random);
    }

    public static ParticleProvider<SimpleParticleType> fall(SpriteSet sprites) {
        return (type, level, x, y, z, xa, ya, za, random) -> new OilDripParticle(level, x, y, z, Stage.FALL, sprites.get(random), random);
    }

    public static ParticleProvider<SimpleParticleType> land(SpriteSet sprites) {
        return (type, level, x, y, z, xa, ya, za, random) -> new OilDripParticle(level, x, y, z, Stage.LAND, sprites.get(random), random);
    }
}
