package dev.strataindustria.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * What one player is shaping by hand (redesign L5): the shape picked for each kind of material, whether the
 * how-to line has been shown, and the blows on the piece in hand. Only the first two are saved; a half-shaped
 * piece is forgotten with the session, and costs nothing because the material is spent on the last blow.
 */
public final class HandShaping {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StrataIndustria.MOD_ID);

    private static final Codec<HandShaping> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("last", Map.of()).forGetter(h -> h.last),
            Codec.BOOL.optionalFieldOf("hinted", false).forGetter(h -> h.hinted)
    ).apply(i, HandShaping::new));

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<HandShaping>> TYPE = ATTACHMENTS.register("hand_shaping",
            () -> AttachmentType.<HandShaping>builder(() -> new HandShaping(Map.of(), false))
                    .serialize(CODEC.fieldOf("shaping"))
                    .copyOnDeath()
                    .build());

    private final Map<String, String> last;
    boolean hinted;

    // The piece in hand. Not saved.
    Identifier shape;
    String material = "";
    int blows;
    int groove;
    long lastClick = Long.MIN_VALUE;
    long lastBlow = Long.MIN_VALUE;
    boolean glintPending;
    Vec3 spot = Vec3.ZERO;

    // The pieces shown on the surface (see ShapingView).
    UUID workpiece;
    UUID ghost;
    long lastActive;
    long relaxAt = Long.MIN_VALUE;
    int viewBlows, viewTotal;
    ItemStack viewMaterial, viewResult;

    private HandShaping(Map<String, String> last, boolean hinted) {
        this.last = new HashMap<>(last);
        this.hinted = hinted;
    }

    public static HandShaping of(ServerPlayer player) {
        return player.getData(TYPE);
    }

    /** The player's state if they have ever shaped anything, else null. */
    static HandShaping existing(ServerPlayer player) {
        return player.hasData(TYPE) ? player.getData(TYPE) : null;
    }

    /** The shape this player last worked this material into, so the next piece starts on it. */
    Identifier lastShape(String materialKey) {
        String id = last.get(materialKey);
        return id == null ? null : Identifier.tryParse(id);
    }

    void remember(String materialKey, Identifier shape) {
        last.put(materialKey, shape.toString());
    }

    /** Forgets the blows on the piece in hand. */
    void reset() {
        blows = 0;
        groove = 0;
        glintPending = false;
        lastBlow = Long.MIN_VALUE;
    }

    /** The blows now on this shape of this material, zero for anything else. */
    int blowsOn(String materialKey, Identifier shapeId) {
        return material.equals(materialKey) && shapeId.equals(shape) ? blows : 0;
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
