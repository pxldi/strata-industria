package dev.strataindustria.knapping;

import com.mojang.math.Transformation;
import dev.strataindustria.StrataIndustria;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * What you see while shaping by hand (redesign L5): the piece on the surface, shrinking and squashing a step per
 * blow, and the ghost of the result floating over it until the first blow, then growing out of it. Both are
 * vanilla item displays that are never saved, moved by the server; they go when the shape is done, the player
 * puts the material away, or nothing has been struck for a few seconds.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class ShapingView {
    static final String TAG = StrataIndustria.MOD_ID + ".shaping_view";
    /** Ticks a blow's squash takes to settle, and the width of one growth step. */
    private static final int SETTLE_TICKS = 4;
    /** Ticks after the last click that the pieces stay up. */
    static final int LINGER_TICKS = 100;
    private static final float SIZE = 0.38f;

    private ShapingView() {}

    /** Shows (or moves) the pieces for the shape in hand. {@code squash} is 0 at rest and about 1 right after a blow. */
    static void show(ServerLevel level, HandShaping state, ItemStack material, ItemStack result, int blows, int total, float squash, long now) {
        float t = total <= 0 ? 0.0f : Math.min(1.0f, blows / (float) total);
        float sq = squash * 0.25f;
        Vec3 at = state.spot;
        float inScale = SIZE * (1.0f - 0.5f * t);
        state.workpiece = put(level, state.workpiece, material, at.add(0.0, 0.03, 0.0), inScale * (1.0f + sq), inScale * (1.0f - sq), squash > 0 ? 0 : SETTLE_TICKS);
        if (t > 0.0f) {
            float outScale = SIZE * (0.45f + 0.55f * t);
            state.ghost = put(level, state.ghost, result, at.add(0.0, 0.045, 0.0), outScale * (1.0f + sq * 0.5f), outScale * (1.0f - sq * 0.5f), squash > 0 ? 0 : SETTLE_TICKS);
        } else {
            // The ghost of the result floats over the piece before the first blow.
            state.ghost = put(level, state.ghost, result, at.add(0.0, 0.42, 0.0), SIZE * 0.55f, SIZE * 0.55f, SETTLE_TICKS);
        }
        state.lastActive = now;
        state.relaxAt = squash > 0 ? now + 2 : Long.MIN_VALUE;
        state.viewBlows = blows;
        state.viewTotal = total;
        state.viewMaterial = material;
        state.viewResult = result;
    }

    /** Settles the pieces back to rest after the squash of a blow. */
    static void relax(ServerLevel level, HandShaping state, long now) {
        state.relaxAt = Long.MIN_VALUE;
        if (state.viewMaterial == null || state.viewResult == null) return;
        show(level, state, state.viewMaterial, state.viewResult, state.viewBlows, state.viewTotal, 0.0f, now);
    }

    static void clear(ServerLevel level, HandShaping state) {
        discard(level, state.workpiece);
        discard(level, state.ghost);
        state.workpiece = null;
        state.ghost = null;
        state.relaxAt = Long.MIN_VALUE;
        state.viewMaterial = null;
        state.viewResult = null;
    }

    private static void discard(ServerLevel level, UUID id) {
        if (id == null) return;
        Entity entity = level.getEntity(id);
        if (entity != null) entity.discard();
    }

    /** Creates or updates one item display lying flat at {@code at}. */
    private static UUID put(ServerLevel level, UUID existing, ItemStack stack, Vec3 at, float sx, float sy, int duration) {
        Entity found = existing == null ? null : level.getEntity(existing);
        Display.ItemDisplay display = found instanceof Display.ItemDisplay d ? d : null;
        if (display == null) {
            display = net.minecraft.world.entity.EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
            if (display == null) return null;
            display.setPos(at.x, at.y, at.z);
            level.addFreshEntity(display);
        }
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        out.store("Pos", Vec3.CODEC, at);
        CompoundTag tag = out.buildResult();
        net.minecraft.nbt.ListTag tags = new net.minecraft.nbt.ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf(TAG));
        tag.put("Tags", tags);
        // A sprite lies flat when turned a quarter about X; the scale is in the sprite's own plane.
        Transformation lie = new Transformation(new Vector3f(), new Quaternionf().rotationX((float) -Math.PI / 2), new Vector3f(sx, sy, SIZE), new Quaternionf());
        TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        view.store("transformation", Transformation.EXTENDED_CODEC, lie);
        view.store("item", ItemStack.CODEC, stack.copyWithCount(1));
        view.store("item_display", ItemDisplayContext.CODEC, ItemDisplayContext.FIXED);
        view.putInt("interpolation_duration", duration);
        view.putInt("start_interpolation", 0);
        view.putInt("teleport_duration", 2);
        view.store("UUID", UUIDUtil.CODEC, display.getUUID());
        CompoundTag merged = view.buildResult().merge(tag);
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), merged));
        return display.getUUID();
    }

    /** Pieces left in a saved chunk by a crash or a stop mid-shaping are not wanted. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().entityTags().contains(TAG)) event.setCanceled(true);
    }
}
