package dev.strataindustria.util;

/**
 * Small deterministic value noise and hashing, independent of Minecraft so that worldgen logic can
 * be unit tested. All functions are pure.
 */
public final class Noise {
    private Noise() {}

    /** SplitMix64 finaliser. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long hash(long seed, long a) {
        return mix(seed ^ mix(a + 0x9E3779B97F4A7C15L));
    }

    public static long hash(long seed, long a, long b) {
        return hash(hash(seed, a), b);
    }

    public static long hash(long seed, long a, long b, long c) {
        return hash(hash(hash(seed, a), b), c);
    }

    /** Uniform double in [0, 1) from a hash. */
    public static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** 2D value noise in [0, 1). {@code frequency} is 1 / feature size in blocks. */
    public static double value2(long seed, double x, double z, double frequency) {
        double fx = x * frequency, fz = z * frequency;
        long x0 = (long) Math.floor(fx), z0 = (long) Math.floor(fz);
        double tx = smooth(fx - x0), tz = smooth(fz - z0);
        double a = unit(hash(seed, x0, z0)), b = unit(hash(seed, x0 + 1, z0));
        double c = unit(hash(seed, x0, z0 + 1)), d = unit(hash(seed, x0 + 1, z0 + 1));
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    /** 3D value noise in [0, 1). */
    public static double value3(long seed, double x, double y, double z, double frequency) {
        double fx = x * frequency, fy = y * frequency, fz = z * frequency;
        long x0 = (long) Math.floor(fx), y0 = (long) Math.floor(fy), z0 = (long) Math.floor(fz);
        double tx = smooth(fx - x0), ty = smooth(fy - y0), tz = smooth(fz - z0);
        double c000 = unit(hash(seed, x0, y0, z0)), c100 = unit(hash(seed, x0 + 1, y0, z0));
        double c010 = unit(hash(seed, x0, y0 + 1, z0)), c110 = unit(hash(seed, x0 + 1, y0 + 1, z0));
        double c001 = unit(hash(seed, x0, y0, z0 + 1)), c101 = unit(hash(seed, x0 + 1, y0, z0 + 1));
        double c011 = unit(hash(seed, x0, y0 + 1, z0 + 1)), c111 = unit(hash(seed, x0 + 1, y0 + 1, z0 + 1));
        double x00 = lerp(c000, c100, tx), x10 = lerp(c010, c110, tx);
        double x01 = lerp(c001, c101, tx), x11 = lerp(c011, c111, tx);
        return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
    }

    /** Two octaves of 2D value noise, in [0, 1). */
    public static double fractal2(long seed, double x, double z, double frequency) {
        return value2(seed, x, z, frequency) * 0.67 + value2(seed + 1, x, z, frequency * 2.0) * 0.33;
    }
}
