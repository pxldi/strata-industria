package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.automation.InserterBlock;
import dev.strataindustria.automation.InserterBlockEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The inserter's arm, swinging over its hub from the block behind to the block in front, with the item in its claw. */
public class InserterRenderer implements BlockEntityRenderer<InserterBlockEntity, InserterRenderer.State> {
    /** Height of the arm's hub above the block floor. */
    private static final double HUB = 12.0 / 16.0;

    private final ItemModelResolver itemModelResolver;
    private final Supplier<ItemStack> arm = RotorRenderer.rotorStack("inserter_arm");

    public InserterRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InserterBlockEntity inserter, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(inserter, state, partialTick, cameraPos, crumbling);
        long time = inserter.getLevel() == null ? 0 : inserter.getLevel().getGameTime();
        // 0 is the arm behind, pointing back and flat; 1 is over the front, pointing forward.
        state.pitch = 180.0f * (1.0f - inserter.reach(time, partialTick));
        state.yaw = 180.0f - inserter.getBlockState().getValue(InserterBlock.FACING).toYRot();
        itemModelResolver.updateForTopItem(state.arm, arm.get(), ItemDisplayContext.NONE, inserter.getLevel(), null, 0);
        state.held.clear();
        ItemStack held = inserter.held();
        if (!held.isEmpty()) {
            itemModelResolver.updateForTopItem(state.held, held, ItemDisplayContext.FIXED, inserter.getLevel(), null,
                    (int) inserter.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, HUB, 0.5);
        pose.rotateDegrees(Axis.YP, state.yaw);
        pose.rotateDegrees(Axis.XP, state.pitch);
        state.arm.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (!state.held.isEmpty()) {
            // The claw hangs at the arm's tip; the item stays upright as the arm turns.
            pose.translate(0.0, -0.02, -0.66);
            pose.rotateDegrees(Axis.XP, -state.pitch);
            pose.scale(0.3f, 0.3f, 0.3f);
            state.held.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(InserterBlockEntity inserter) {
        return new AABB(inserter.getBlockPos()).inflate(1.0);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState arm = new ItemStackRenderState();
        final ItemStackRenderState held = new ItemStackRenderState();
        float pitch;
        float yaw;
    }
}
