package dev.strataindustria.client.telegraph;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.telegraph.DispatchBoardBlock;
import dev.strataindustria.transport.telegraph.DispatchBoardBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * The chalk on the slate of a dispatch board (outposts spec 9.1): one row for each outpost that reports, its name on the
 * left and how it stands on the right. Written small enough for five rows to fit the frame.
 */
public class DispatchBoardRenderer implements BlockEntityRenderer<DispatchBoardBlockEntity, DispatchBoardRenderer.State> {
    /** Font pixels to blocks. */
    private static final float SCALE = 0.0072f;
    private static final int ROWS = 5;
    /** Width of the slate's writing area in font pixels, and the gap between the name and the state word. */
    private static final int WIDTH = 92, GAP = 4;
    private static final int CHALK = 0xFFE9E5D8, DIM = 0xFF8C9096, TROUBLE = 0xFFD98B72;
    /** Distance of the writing from the middle of the block along the face normal, just proud of the slate. */
    private static final float FACE = 0.5f - 2.0f / 16.0f + 0.004f;

    private final Font font;

    public DispatchBoardRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DispatchBoardBlockEntity board, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(board, state, partialTick, cameraPos, crumbling);
        state.facing = board.getBlockState().getValue(DispatchBoardBlock.FACING);
        state.rows.clear();
        List<DispatchBoardBlockEntity.Row> rows = board.rows();
        for (int i = 0; i < Math.min(ROWS, rows.size()); i++) {
            DispatchBoardBlockEntity.Row row = rows.get(i);
            Component word = Component.translatable(StrataIndustria.MOD_ID + ".dispatch.state.short." + row.state());
            int stateWidth = font.width(word);
            String name = font.plainSubstrByWidth(row.name(), WIDTH - stateWidth - GAP);
            int colour = row.silent() ? DIM : row.state() == 0 ? CHALK : row.state() == 1 ? 0xFFC4C8CE : TROUBLE;
            state.rows.add(new Line(Component.literal(name).getVisualOrderText(), word.getVisualOrderText(), stateWidth, colour));
        }
        state.more = Math.max(0, rows.size() - ROWS);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.rows.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(-state.facing.toYRot())));
        pose.translate(0.0, 0.3, FACE);
        pose.scale(SCALE, -SCALE, SCALE);
        int y = 0;
        for (Line line : state.rows) {
            collector.submitText(pose, -WIDTH / 2.0f, y, line.name, false, Font.DisplayMode.POLYGON_OFFSET, state.lightCoords, line.colour, 0, 0);
            collector.submitText(pose, WIDTH / 2.0f - line.stateWidth, y, line.word, false, Font.DisplayMode.POLYGON_OFFSET, state.lightCoords, line.colour, 0, 0);
            y += 16;
        }
        if (state.more > 0) {
            FormattedCharSequence more = Component.translatable(StrataIndustria.MOD_ID + ".dispatch.more", state.more).getVisualOrderText();
            collector.submitText(pose, -font.width(more) / 2.0f, y, more, false, Font.DisplayMode.POLYGON_OFFSET, state.lightCoords, DIM, 0, 0);
        }
        pose.popPose();
    }

    private record Line(FormattedCharSequence name, FormattedCharSequence word, int stateWidth, int colour) {}

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        final List<Line> rows = new ArrayList<>();
        int more;
    }
}
