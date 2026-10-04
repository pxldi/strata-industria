package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.steam.SteamHammerBlock;
import dev.strataindustria.steam.SteamHammerBlockEntity;
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

/** The steam hammer's ram, which rests raised and drops onto the anvil with each blow, and the piece lying under it. */
public class SteamHammerRenderer implements BlockEntityRenderer<SteamHammerBlockEntity, SteamHammerRenderer.State> {
    /** How far the ram is raised between blows. */
    private static final float LIFT = 3.0f / 16.0f;

    private final ItemModelResolver itemModelResolver;
    private final Supplier<ItemStack> ram = RotorRenderer.rotorStack("steam_hammer_ram");

    public SteamHammerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SteamHammerBlockEntity hammer, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(hammer, state, partialTick, cameraPos, crumbling);
        long time = hammer.getLevel() == null ? 0 : hammer.getLevel().getGameTime();
        state.lift = LIFT * (1.0f - hammer.swing(time, partialTick));
        state.yaw = 180.0f - hammer.getBlockState().getValue(SteamHammerBlock.FACING).toYRot();
        itemModelResolver.updateForTopItem(state.ram, ram.get(), ItemDisplayContext.NONE, hammer.getLevel(), null, 0);
        state.piece.clear();
        ItemStack piece = hammer.getItem(AnvilBlockEntity.INPUT);
        if (!piece.isEmpty()) {
            itemModelResolver.updateForTopItem(state.piece, piece, ItemDisplayContext.FIXED, hammer.getLevel(), null,
                    (int) hammer.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        // The item renderer draws the model about its centre.
        pose.translate(0.5, 0.5 + state.lift, 0.5);
        pose.rotateDegrees(Axis.YP, state.yaw);
        state.ram.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        if (!state.piece.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, SteamHammerBlockEntity.FACE + 0.01, 0.5);
            pose.rotateDegrees(Axis.YP, state.yaw);
            pose.rotateDegrees(Axis.XP, 90);
            pose.scale(0.35f, 0.35f, 0.35f);
            state.piece.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState ram = new ItemStackRenderState();
        final ItemStackRenderState piece = new ItemStackRenderState();
        float lift;
        float yaw;
    }
}
