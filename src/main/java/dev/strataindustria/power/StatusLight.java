package dev.strataindustria.power;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The blockstate {@code status} that drives the 2x2 lamp on every electric machine and generator
 * (tier 5 spec 23.2): off, green running, amber low power or waiting, red error.
 */
public enum StatusLight implements StringRepresentable {
    OFF,
    RUN,
    WAIT,
    ERROR;

    public static final EnumProperty<StatusLight> PROPERTY = EnumProperty.create("status", StatusLight.class);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
