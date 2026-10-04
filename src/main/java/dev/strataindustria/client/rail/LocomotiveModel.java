package dev.strataindustria.client.rail;

import dev.strataindustria.StrataIndustria;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The steam locomotive: a 0-4-0 tank engine, chimney end towards +x, cab at the back. Like the wagons it runs along
 * x with +y down and the rail at y = 6. Every part reads its net from {@link LocomotiveAtlas}. The coupling rods
 * turn with the wheels, the sight glasses show the water, and the hitch shows once a wagon is behind it.
 */
public class LocomotiveModel extends EntityModel<MineTubRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(StrataIndustria.id("steam_locomotive"), "main");

    private final ModelPart hitch;
    private final ModelPart rods;
    private final ModelPart needles;

    public LocomotiveModel(ModelPart root) {
        super(root);
        hitch = root.getChild("hitch");
        rods = root.getChild("rods");
        needles = root.getChild("needles");
    }

    private static CubeListBuilder box(CubeListBuilder builder, String part, float x, float y, float z) {
        LocomotiveAtlas.Cell cell = LocomotiveAtlas.cell(part);
        return builder.texOffs(cell.u(), cell.v()).addBox(x, y, z, cell.dx(), cell.dy(), cell.dz());
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Frame, with the cab's back wall and the roof on posts.
        root.addOrReplaceChild("frame", box(CubeListBuilder.create(), "frame", -9, 3, -5), PartPose.ZERO);
        root.addOrReplaceChild("cab", box(box(box(box(box(box(box(box(box(CubeListBuilder.create(),
                        "back_wall", -9, -13, -5),
                        "side_panel", -8, -5, -5), "side_panel", -8, -5, 4),
                        "roof", -10.5f, -14, -6),
                        "post", -9, -13, -5), "post", -9, -13, 4), "post", -2, -13, -5), "post", -2, -13, 4),
                        "beam", -10, 0, -6),
                PartPose.ZERO);
        // The boiler: a tall and a wide box make the round barrel; bands, dome and the smokebox on the front.
        root.addOrReplaceChild("boiler", box(box(box(box(box(box(box(CubeListBuilder.create(),
                        "boiler_a", -1, -7, -3),
                        "boiler_b", -1, -6, -4.5f),
                        "band", 2, -6.5f, -5), "band", 4.5f, -6.5f, -5),
                        "smokebox", 6, -6, -4.5f),
                        "dome", 1, -10, -1.5f), "dome_cap", 2, -11, -0.5f),
                PartPose.ZERO);
        root.addOrReplaceChild("chimney", box(box(CubeListBuilder.create(), "chimney", 7, -11, -1), "chimney_cap", 6.5f, -12, -1.5f), PartPose.ZERO);
        root.addOrReplaceChild("tanks", box(box(CubeListBuilder.create(), "tank", -1, -4, 4.5f), "tank", -1, -4, -7.5f), PartPose.ZERO);
        root.addOrReplaceChild("glasses", box(box(CubeListBuilder.create(), "glass", 4.5f, -3.5f, 7.5f), "glass", 4.5f, -3.5f, -8.5f), PartPose.ZERO);
        root.addOrReplaceChild("needles", box(box(CubeListBuilder.create(), "needle", 3.5f, 1, 7.6f), "needle", 3.5f, 1, -8.6f), PartPose.ZERO);
        // Front: the buffer beam, two buffers, the lamp. Rear: two buffers.
        root.addOrReplaceChild("front", box(box(box(box(CubeListBuilder.create(),
                        "beam", 9, 0, -6), "buffer", 10, 0.5f, -4), "buffer", 10, 0.5f, 2), "lamp", 9, -4, -1),
                PartPose.ZERO);
        root.addOrReplaceChild("rear", box(box(CubeListBuilder.create(), "buffer", -11, 0.5f, -4), "buffer", -11, 0.5f, 2), PartPose.ZERO);
        root.addOrReplaceChild("wheels", box(box(box(box(CubeListBuilder.create(),
                        "wheel", -6, 2, -6.5f), "wheel", 2, 2, -6.5f), "wheel", -6, 2, 5.5f), "wheel", 2, 2, 5.5f),
                PartPose.ZERO);
        root.addOrReplaceChild("rods", box(box(CubeListBuilder.create(), "rod", -4, 3, -7), "rod", -4, 3, 6), PartPose.ZERO);
        root.addOrReplaceChild("hitch", box(CubeListBuilder.create(), "hitch", -15, 1, -0.5f), PartPose.ZERO);
        return LayerDefinition.create(mesh, LocomotiveAtlas.WIDTH, LocomotiveAtlas.HEIGHT);
    }

    @Override
    public void setupAnim(MineTubRenderState state) {
        super.setupAnim(state);
        hitch.visible = state.coupled;
        // The crank pins go round with the wheels: the rods rise and fall and slide a pixel.
        rods.x = (float) Math.cos(state.wheelPhase);
        rods.y = (float) Math.sin(state.wheelPhase);
        needles.y = -state.gauge * 0.28f;
    }
}
