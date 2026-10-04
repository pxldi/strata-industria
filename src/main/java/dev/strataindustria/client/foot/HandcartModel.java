package dev.strataindustria.client.foot;

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
 * The handcart: a plank bed on two wheels with a pair of shafts at the front. Model space has +y down, the shafts
 * point to -z, and the ground is at y = 6. The texture is laid out by {@code TransportTextures}.
 */
public class HandcartModel extends EntityModel<HandcartRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(StrataIndustria.id("handcart"), "main");

    private final ModelPart shafts;
    private final ModelPart leftWheel;
    private final ModelPart rightWheel;

    public HandcartModel(ModelPart root) {
        super(root);
        shafts = root.getChild("shafts");
        leftWheel = root.getChild("left_wheel");
        rightWheel = root.getChild("right_wheel");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bed = root.addOrReplaceChild("bed", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8, 0, -8, 16, 2, 16)
                        .texOffs(0, 18).addBox(-8, -5, -8, 2, 5, 16)
                        .texOffs(0, 18).addBox(6, -5, -8, 2, 5, 16)
                        .texOffs(36, 18).addBox(-6, -5, -8, 12, 5, 2)
                        .texOffs(36, 18).addBox(-6, -5, 6, 12, 5, 2),
                PartPose.ZERO);

        root.addOrReplaceChild("left_wheel", wheel(), PartPose.offset(-10, 1, 0));
        root.addOrReplaceChild("right_wheel", wheel(), PartPose.offset(10, 1, 0));

        root.addOrReplaceChild("shafts", CubeListBuilder.create()
                        .texOffs(32, 26).addBox(-6, -1, -22, 2, 2, 14)
                        .texOffs(32, 26).addBox(4, -1, -22, 2, 2, 14)
                        .texOffs(32, 42).addBox(-6, -1, -22, 12, 2, 2),
                PartPose.offset(0, -1, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Three crossed boards make an eight-sided wheel; a bronze hub sits on the axle. */
    private static CubeListBuilder wheel() {
        return CubeListBuilder.create()
                .texOffs(0, 46).addBox(-1, -5, -3, 2, 10, 6)
                .texOffs(16, 46).addBox(-1, -3, -5, 2, 6, 10)
                .texOffs(40, 46).addBox(-1, -4, -4, 2, 8, 8)
                .texOffs(0, 42).addBox(-2, -1, -1, 4, 2, 2);
    }

    @Override
    public void setupAnim(HandcartRenderState state) {
        super.setupAnim(state);
        shafts.xRot = state.pulled ? -0.12f : 0.5f;
        leftWheel.xRot = state.wheelAngle;
        rightWheel.xRot = state.wheelAngle;
    }
}
