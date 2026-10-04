package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.fluid.FluidTankBlockEntity;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The fluid behind a tank's glass (tier 4 spec 21.4): a column that rises with what this block of the
 * stack holds. Each fluid is a full-height render-only model, {@code items/rotor/tank_<fluid>}, squashed to the level.
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State> {
    /** Fluids with a tank model; anything else is drawn as water. */
    private static final Set<String> MODELS = Set.of("water", "creosote", "steam", "sulfur_dioxide", "lye", "tannin", "latex",
            "crude_oil", "naphtha", "diesel", "heavy_oil", "refinery_gas", "ethylene", "butadiene", "vinyl_chloride", "hydrogen_chloride");

    private final ItemModelResolver itemModelResolver;
    private final Map<String, Supplier<ItemStack>> columns = new HashMap<>();

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        for (String name : MODELS) columns.put(name, RotorRenderer.rotorStack("tank_" + name));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity tank, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTick, cameraPos, crumbling);
        state.column.clear();
        Fluid fluid = tank.fluid();
        if (tank.amount() <= 0) return;
        String name = BuiltInRegistries.FLUID.getKey(fluid).getPath();
        Supplier<ItemStack> column = columns.getOrDefault(name, columns.get("water"));
        state.level = Math.min(1.0f, tank.amount() / (float) FluidTankBlockEntity.CAPACITY);
        itemModelResolver.updateForTopItem(state.column, column.get(), ItemDisplayContext.NONE, tank.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.column.isEmpty()) return;
        pose.pushPose();
        // The item renderer draws the model about its centre; the column stands on the tank floor.
        pose.translate(0.5, 0.0, 0.5);
        pose.scale(1.0f, state.level, 1.0f);
        pose.translate(0.0, 0.5, 0.0);
        state.column.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState column = new ItemStackRenderState();
        float level;
    }
}
