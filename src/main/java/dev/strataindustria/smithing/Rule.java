package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** One finishing rule of a smithing recipe (spec 9.2), such as "punch last" or "bend not last". */
public record Rule(Kind hit, Where where) {
    public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.fieldOf("hit").forGetter(Rule::hit),
            Where.CODEC.fieldOf("where").forGetter(Rule::where)
    ).apply(i, Rule::new));
    public static final StreamCodec<ByteBuf, Rule> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Rule::decode, Rule::encode);

    /** Which hits a rule accepts. "Hit" is light, medium or hard. */
    public enum Kind implements StringRepresentable {
        HIT, DRAW, PUNCH, BEND, UPSET, SHRINK;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        public boolean accepts(HitType type) {
            return switch (this) {
                case HIT -> type == HitType.LIGHT || type == HitType.MEDIUM || type == HitType.HARD;
                case DRAW -> type == HitType.DRAW;
                case PUNCH -> type == HitType.PUNCH;
                case BEND -> type == HitType.BEND;
                case UPSET -> type == HitType.UPSET;
                case SHRINK -> type == HitType.SHRINK;
            };
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Where among the last three hits the rule's hit has to be. */
    public enum Where implements StringRepresentable {
        LAST, SECOND_LAST, THIRD_LAST, NOT_LAST, ANY;

        public static final Codec<Where> CODEC = StringRepresentable.fromEnum(Where::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static Rule of(Kind hit, Where where) {
        return new Rule(hit, where);
    }

    /** Whether the rule holds for {@code recent}, newest hit first (null where there was no hit yet). */
    public boolean test(HitType last, HitType second, HitType third) {
        return switch (where) {
            case LAST -> ok(last);
            case SECOND_LAST -> ok(second);
            case THIRD_LAST -> ok(third);
            case NOT_LAST -> ok(second) || ok(third);
            case ANY -> ok(last) || ok(second) || ok(third);
        };
    }

    private boolean ok(HitType type) {
        return type != null && hit.accepts(type);
    }

    public static boolean all(List<Rule> rules, HitType last, HitType second, HitType third) {
        for (Rule rule : rules) if (!rule.test(last, second, third)) return false;
        return true;
    }

    /** Packs the rule into one small int for menu data. */
    public int encode() {
        return hit.ordinal() * 8 + where.ordinal();
    }

    public static Rule decode(int code) {
        return new Rule(Kind.values()[Math.floorMod(code / 8, Kind.values().length)], Where.values()[Math.floorMod(code % 8, Where.values().length)]);
    }
}
