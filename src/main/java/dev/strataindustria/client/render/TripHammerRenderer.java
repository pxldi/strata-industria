package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.machine.TripHammerBlock;
import dev.strataindustria.machine.TripHammerBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The trip hammer's arm: it rests lifted and drops onto the anvil with each blow. */
public class TripHammerRenderer implements BlockEntityRenderer<TripHammerBlockEntity, TripHammerRenderer.State> {
    /** How far the arm is lifted between blows, in degrees. */
    private static final float LIFT = 24.0f;

    private final ItemModelResolver itemModelResolver;
    private final ItemStack arm;

    public TripHammerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.arm = new ItemStack(Items.STICK);
        this.arm.set(DataComponents.ITEM_MODEL, StrataIndustria.id("rotor/trip_hammer_arm"));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TripHammerBlockEntity hammer, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(hammer, state, partialTick, cameraPos, crumbling);
        long time = hammer.getLevel() == null ? 0 : hammer.getLevel().getGameTime();
        state.lift = LIFT * (1.0f - hammer.swing(time, partialTick));
        state.yaw = 180.0f - hammer.getBlockState().getValue(TripHammerBlock.FACING).toYRot();
        itemModelResolver.updateForTopItem(state.item, arm, ItemDisplayContext.NONE, hammer.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.YP, state.yaw);
        // The arm pivots on the crossbar at (8, 26, 14) in model pixels.
        pose.translate(0.0, 18.0 / 16.0, 6.0 / 16.0);
        pose.rotateDegrees(Axis.XP, state.lift);
        pose.translate(0.0, -18.0 / 16.0, -6.0 / 16.0);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TripHammerBlockEntity hammer) {
        return new AABB(hammer.getBlockPos()).inflate(1);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float lift;
        float yaw;
    }
}
