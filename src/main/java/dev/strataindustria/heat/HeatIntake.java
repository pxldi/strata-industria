package dev.strataindustria.heat;

import org.jspecify.annotations.Nullable;

/**
 * What a consumer takes from its sources in a tick, and what it took last tick: the heat, the hottest
 * offer, and the pipe rating that held the temperature down, if one did. Sources may tick before or after
 * the consumer, so the consumer works on last tick's heat.
 */
public final class HeatIntake {
    private final int minTemperature;
    private final int maxHeat;
    private int pending;
    private float offered;
    private int pendingLimit;
    private int heat;
    private float temperature;
    private int limit;

    /**
     * @param minTemperature below this it takes nothing
     * @param maxHeat        HU it takes per tick at most
     */
    public HeatIntake(int minTemperature, int maxHeat) {
        this.minTemperature = minTemperature;
        this.maxHeat = maxHeat;
    }

    /** What it asks for at {@code temperature} while {@code wanted}. */
    public int demand(float temperature, boolean wanted) {
        return wanted && temperature >= minTemperature ? Math.max(0, maxHeat - pending) : 0;
    }

    public int offer(float temperature, int heat) {
        offered = Math.max(offered, temperature);
        if (temperature < minTemperature) return 0;
        int take = Math.max(0, Math.min(heat, maxHeat - pending));
        pending += take;
        return take;
    }

    public void route(@Nullable HeatPipeBlock limitedBy) {
        if (limitedBy != null) pendingLimit = limitedBy.maxTemperature();
    }

    /** Ends the tick: what came in becomes last tick's heat. */
    public void roll() {
        heat = pending;
        temperature = offered;
        limit = pendingLimit;
        pending = 0;
        offered = 0;
        pendingLimit = 0;
    }

    public int heat() {
        return heat;
    }

    public float temperature() {
        return temperature;
    }

    /** The rating of the pipe that held the temperature down last tick, or 0. */
    public int limit() {
        return limit;
    }

    public int minTemperature() {
        return minTemperature;
    }

    /** How much of a full supply came in last tick, from 0 to 1. */
    public float share() {
        return Math.min(1.0f, heat / (float) maxHeat);
    }
}
