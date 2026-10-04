package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.tanning.SoakingBarrelBlock;
import dev.strataindustria.tanning.SoakingBarrelBlockEntity;
import java.util.EnumMap;
import java.util.Map;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shows the fluid in an open soaking barrel (tier 3 spec 12.1) as a surface that rises with the tank.
 * Each fluid's surface is a flat render-only model, {@code items/rotor/barrel_<fluid>}.
 */
public class SoakingBarrelRenderer implements BlockEntityRenderer<SoakingBarrelBlockEntity, SoakingBarrelRenderer.State> {
    /** The barrel's inside floor and rim, in block units. */
    private static final float FLOOR = 1.0f / 16.0f, RIM = 15.0f / 16.0f;

    private final ItemModelResolver itemModelResolver;
    private final Map<SoakingBarrelBlockEntity.TankFluid, Supplier<ItemStack>> surfaces = new EnumMap<>(SoakingBarrelBlockEntity.TankFluid.class);

    public SoakingBarrelRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        surfaces.put(SoakingBarrelBlockEntity.TankFluid.WATER, RotorRenderer.rotorStack("barrel_water"));
        surfaces.put(SoakingBarrelBlockEntity.TankFluid.LYE, RotorRenderer.rotorStack("barrel_lye"));
        surfaces.put(SoakingBarrelBlockEntity.TankFluid.TANNIN, RotorRenderer.rotorStack("barrel_tannin"));
        surfaces.put(SoakingBarrelBlockEntity.TankFluid.OTHER, RotorRenderer.rotorStack("barrel_water"));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SoakingBarrelBlockEntity barrel, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(barrel, state, partialTick, cameraPos, crumbling);
        state.item.clear();
        Supplier<ItemStack> surface = surfaces.get(SoakingBarrelBlockEntity.TankFluid.of(barrel.fluid()));
        if (surface == null || barrel.amount() <= 0 || barrel.getBlockState().getValue(SoakingBarrelBlock.SEALED)) return;
        state.height = FLOOR + (RIM - FLOOR) * Math.min(1.0f, barrel.amount() / (float) SoakingBarrelBlockEntity.CAPACITY);
        itemModelResolver.updateForTopItem(state.item, surface.get(), ItemDisplayContext.NONE, barrel.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, state.height, 0.5);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float height;
    }
}
