package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.fluid.FluidFilterBlock;
import dev.strataindustria.fluid.FluidFilterBlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The fluid in the fluid filter's little window (tier 5 spec 23.2): the fluid's own tank model squashed to a thin
 * plate behind the glass, on the top of the housing, or on the north side when the pipe runs up and down.
 */
public class FluidFilterRenderer implements BlockEntityRenderer<FluidFilterBlockEntity, FluidFilterRenderer.State> {
    /** Fluids with a tank model; anything else is drawn as water. */
    private static final Set<String> MODELS = Set.of("water", "creosote", "steam", "sulfur_dioxide", "lye", "tannin", "latex",
            "crude_oil", "naphtha", "diesel", "heavy_oil", "refinery_gas", "ethylene", "butadiene", "vinyl_chloride", "hydrogen_chloride",
            "sulfuric_acid", "brine", "hydrogen", "oxygen", "chlorine");

    private final ItemModelResolver itemModelResolver;
    private final Map<String, Supplier<ItemStack>> plates = new HashMap<>();

    public FluidFilterRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        for (String name : MODELS) plates.put(name, RotorRenderer.rotorStack("tank_" + name));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidFilterBlockEntity filter, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(filter, state, partialTick, cameraPos, crumbling);
        state.plate.clear();
        state.on = false;
        if (!filter.isSet()) return;
        String name = BuiltInRegistries.FLUID.getKey(filter.fluid()).getPath();
        state.up = filter.getBlockState().getValue(FluidFilterBlock.FACING).getAxis() != Direction.Axis.Y;
        state.on = true;
        itemModelResolver.updateForTopItem(state.plate, plates.getOrDefault(name, plates.get("water")).get(), ItemDisplayContext.NONE, filter.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.on || state.plate.isEmpty()) return;
        pose.pushPose();
        if (state.up) {
            pose.translate(0.5, 12.9 / 16.0, 0.5);
            pose.scale(6.0f / 16.0f, 0.02f, 6.0f / 16.0f);
        } else {
            pose.translate(0.5, 0.5, 3.1 / 16.0);
            pose.scale(6.0f / 16.0f, 6.0f / 16.0f, 0.02f);
        }
        state.plate.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState plate = new ItemStackRenderState();
        boolean on, up;
    }
}
