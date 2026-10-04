package dev.strataindustria.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The last pattern one player knapped by hand, from rock and from flint. The knapping screen's repeat button
 * cuts it again. Kept on the player and copied on death; the menu reports the answer, so nothing needs syncing.
 */
public final class KnappedPatterns {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StrataIndustria.MOD_ID);

    private static final Codec<KnappedPatterns> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("rock", 0).forGetter(k -> k.rock),
            Codec.INT.optionalFieldOf("flint", 0).forGetter(k -> k.flint)
    ).apply(i, KnappedPatterns::new));

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<KnappedPatterns>> TYPE = ATTACHMENTS.register("knapped_patterns",
            () -> AttachmentType.<KnappedPatterns>builder(() -> new KnappedPatterns(0, 0))
                    .serialize(CODEC.fieldOf("patterns"))
                    .copyOnDeath()
                    .build());

    private int rock, flint;

    private KnappedPatterns(int rock, int flint) {
        this.rock = rock;
        this.flint = flint;
    }

    public static KnappedPatterns of(ServerPlayer player) {
        return player.getData(TYPE);
    }

    /** Bit mask of the cells left in place by the last finished knapping, or 0 if there was none. */
    public int last(boolean flint) {
        return flint ? this.flint : rock;
    }

    public void set(boolean flint, int mask) {
        if (flint) this.flint = mask;
        else rock = mask;
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
