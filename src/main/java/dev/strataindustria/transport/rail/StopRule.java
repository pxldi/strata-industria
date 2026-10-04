package dev.strataindustria.transport.rail;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** When a stop lets a consist go (outposts spec 6.3). A redstone pulse releases it whatever the rule. */
public enum StopRule implements StringRepresentable {
    WAIT("wait", true),
    FULL("full", false),
    EMPTY("empty", false),
    REDSTONE("redstone", false),
    IDLE("idle", true);

    public static final Codec<StopRule> CODEC = StringRepresentable.fromEnum(StopRule::values);

    private final String id;
    private final boolean usesSeconds;

    StopRule(String id, boolean usesSeconds) {
        this.id = id;
        this.usesSeconds = usesSeconds;
    }

    /** Whether the rule counts seconds ({@code wait N seconds}, {@code until idle N seconds}). */
    public boolean usesSeconds() {
        return usesSeconds;
    }

    public String langKey() {
        return "strataindustria.stop.rule." + id;
    }

    public StopRule next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
