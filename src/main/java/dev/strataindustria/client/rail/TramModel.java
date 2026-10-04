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
 * The electric tram: a small single-ended car, cab end towards +x, on two bogies. Like the wagons it runs along x with
 * +y down and the rail at y = 6. Every part reads its net from {@link TramAtlas}. The trolley pole stands on the roof
 * at the rear and leans back to the wire, the bell on the roof swings when it is rung, and the hitch shows once a
 * wagon is behind it.
 */
public class TramModel extends EntityModel<MineTubRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(StrataIndustria.id("electric_tram"), "main");

    private final ModelPart hitch;
    private final ModelPart pole;
    private final ModelPart bell;

    public TramModel(ModelPart root) {
        super(root);
        hitch = root.getChild("hitch");
        pole = root.getChild("pole");
        bell = root.getChild("bell");
    }

    private static CubeListBuilder box(CubeListBuilder builder, String part, float x, float y, float z) {
        TramAtlas.Cell cell = TramAtlas.cell(part);
        return builder.texOffs(cell.u(), cell.v()).addBox(x, y, z, cell.dx(), cell.dy(), cell.dz());
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("frame", box(CubeListBuilder.create(), "frame", -16, 3, -8), PartPose.ZERO);
        root.addOrReplaceChild("body", box(box(CubeListBuilder.create(), "body_low", -16, -3, -9), "body_up", -16, -9, -9), PartPose.ZERO);
        root.addOrReplaceChild("roof", box(box(box(CubeListBuilder.create(), "roof", -17, -11, -10), "vent", -9, -13, -3), "vent", 3, -13, -3), PartPose.ZERO);
        root.addOrReplaceChild("front", box(box(CubeListBuilder.create(), "bumper", 16, 1, -8), "lamp", 16, -6, -2), PartPose.ZERO);
        root.addOrReplaceChild("rear", box(CubeListBuilder.create(), "bumper", -17, 1, -8), PartPose.ZERO);
        root.addOrReplaceChild("wheels", box(box(box(box(box(box(box(box(CubeListBuilder.create(),
                        "wheel", -14, 2, -10), "wheel", -7, 2, -10), "wheel", 4, 2, -10), "wheel", 11, 2, -10),
                        "wheel", -14, 2, 9), "wheel", -7, 2, 9), "wheel", 4, 2, 9), "wheel", 11, 2, 9),
                PartPose.ZERO);
        root.addOrReplaceChild("hitch", box(CubeListBuilder.create(), "hitch", -21, 1, -0.5f), PartPose.ZERO);
        root.addOrReplaceChild("pole_base", box(CubeListBuilder.create(), "pole_base", -13, -13, -1.5f), PartPose.ZERO);
        // The pole turns on its foot; the shoe rides on its tip.
        PartDefinition pole = root.addOrReplaceChild("pole", box(CubeListBuilder.create(), "pole", -1, -72, -1), PartPose.offset(-11.5f, -13, 0));
        pole.addOrReplaceChild("shoe", box(CubeListBuilder.create(), "shoe", -2, -1, -1.5f), PartPose.offset(0, -72, 0));
        // The bell hangs from the roof at the cab end.
        root.addOrReplaceChild("bell", box(CubeListBuilder.create(), "bell", -1.5f, 0, -1.5f), PartPose.offset(13.5f, -14, 0));
        return LayerDefinition.create(mesh, TramAtlas.WIDTH, TramAtlas.HEIGHT);
    }

    @Override
    public void setupAnim(MineTubRenderState state) {
        super.setupAnim(state);
        hitch.visible = state.coupled;
        pole.zRot = state.poleAngle;
        bell.zRot = state.bellSwing;
    }
}
