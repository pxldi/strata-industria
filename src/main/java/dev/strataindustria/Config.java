package dev.strataindustria;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_DEBUG_INFO = BUILDER
            .comment("Show extra diagnostic information in tooltips and logs.")
            .define("showDebugInfo", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
