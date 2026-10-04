package dev.strataindustria.transport.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What a stop's screen shows and sends back: its name, departure rule and whether the consist reverses. */
public record StopData(BlockPos pos, String name, StopRule rule, int seconds, boolean reverse) {
    public static final int MAX_NAME = 24;
    public static final int MIN_SECONDS = 1, MAX_SECONDS = 600, DEFAULT_SECONDS = 10;

    public static final StreamCodec<RegistryFriendlyByteBuf, StopData> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StopData::pos,
            ByteBufCodecs.stringUtf8(MAX_NAME * 4), StopData::name,
            ByteBufCodecs.idMapper(i -> StopRule.values()[Math.floorMod(i, StopRule.values().length)], StopRule::ordinal), StopData::rule,
            ByteBufCodecs.VAR_INT, StopData::seconds,
            ByteBufCodecs.BOOL, StopData::reverse,
            StopData::new);

    public static String cleanName(String name) {
        String trimmed = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        return trimmed.length() > MAX_NAME ? trimmed.substring(0, MAX_NAME) : trimmed;
    }

    public static int cleanSeconds(int seconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }
}
