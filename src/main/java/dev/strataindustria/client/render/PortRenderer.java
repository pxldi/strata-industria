package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.electric.machine.ChemicalMachineBlockEntity;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The small output ports on an electrolyser (tier 5 spec 11.2 and 23.2): a wrench-set face shows a ring in its
 * product's colour, or a crossed ring when closed. Faces left on auto show nothing.
 */
public class PortRenderer implements BlockEntityRenderer<ChemicalMachineBlockEntity, PortRenderer.State> {
    private final ItemModelResolver itemModelResolver;
    private final Supplier<ItemStack>[] rings;

    @SuppressWarnings("unchecked")
    public PortRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.rings = new Supplier[] {RotorRenderer.rotorStack("port_1"), RotorRenderer.rotorStack("port_2"), RotorRenderer.rotorStack("port_3"),
                RotorRenderer.rotorStack("port_none")};
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChemicalMachineBlockEntity machine, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(machine, state, partialTick, cameraPos, crumbling);
        int outputs = machine.layout().fluidOutputs();
        for (Direction face : Direction.values()) {
            ItemStackRenderState ring = state.rings[face.get3DDataValue()];
            ring.clear();
            int mode = machine.faceMode(face);
            if (mode == 0) continue;
            ItemStack stack = rings[mode > outputs ? 3 : Math.min(mode, 3) - 1].get();
            itemModelResolver.updateForTopItem(ring, stack, ItemDisplayContext.NONE, machine.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Direction face : Direction.values()) {
            ItemStackRenderState ring = state.rings[face.get3DDataValue()];
            if (ring.isEmpty()) continue;
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            RotorRenderer.orient(pose, face);
            ring.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] rings = new ItemStackRenderState[6];

        State() {
            for (int i = 0; i < 6; i++) rings[i] = new ItemStackRenderState();
        }
    }
}
