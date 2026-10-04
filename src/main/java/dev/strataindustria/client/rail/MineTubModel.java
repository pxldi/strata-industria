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
 * The mine tub: a plank tub 14 long and 13 wide with two iron bands, a rim, and four small wheels. Like the vanilla
 * cart its long side runs along x; model space has +y down and the ground at y = 6. Wood reads the upper part of the
 * 64 x 64 sheet and iron the strip under y 54 (laid out by {@code RailTextures}).
 */
public class MineTubModel extends EntityModel<MineTubRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(StrataIndustria.id("mine_tub"), "main");

    private final ModelPart couplings;

    public MineTubModel(ModelPart root) {
        super(root);
        couplings = root.getChild("couplings");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Floor and the four walls.
        root.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 0).addBox(-7, 2, -6, 14, 2, 12), PartPose.ZERO);
        root.addOrReplaceChild("walls", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-7, -5, -7, 14, 7, 1)
                        .texOffs(0, 16).addBox(-7, -5, 6, 14, 7, 1)
                        .texOffs(30, 16).addBox(-7, -5, -6, 1, 7, 12)
                        .texOffs(30, 16).addBox(6, -5, -6, 1, 7, 12),
                PartPose.ZERO);
        // The rim, a little proud of the walls.
        root.addOrReplaceChild("rim", CubeListBuilder.create()
                        .texOffs(0, 37).addBox(-7.5f, -6, -7.5f, 15, 2, 1.5f)
                        .texOffs(0, 37).addBox(-7.5f, -6, 6, 15, 2, 1.5f)
                        .texOffs(30, 37).addBox(-7.5f, -6, -6, 1.5f, 2, 12)
                        .texOffs(30, 37).addBox(6, -6, -6, 1.5f, 2, 12),
                PartPose.ZERO);
        // Two iron bands round the body, seen as a strap up each wall.
        root.addOrReplaceChild("bands", CubeListBuilder.create()
                        .texOffs(0, 54).addBox(-3.5f, -5, -7.5f, 1, 7, 1)
                        .texOffs(0, 54).addBox(-3.5f, -5, 6.5f, 1, 7, 1)
                        .texOffs(0, 54).addBox(2.5f, -5, -7.5f, 1, 7, 1)
                        .texOffs(0, 54).addBox(2.5f, -5, 6.5f, 1, 7, 1),
                PartPose.ZERO);
        // Four wheels outside the walls.
        root.addOrReplaceChild("wheels", CubeListBuilder.create()
                        .texOffs(36, 54).addBox(-5.5f, 3, -8.5f, 3, 3, 1)
                        .texOffs(36, 54).addBox(2.5f, 3, -8.5f, 3, 3, 1)
                        .texOffs(36, 54).addBox(-5.5f, 3, 7.5f, 3, 3, 1)
                        .texOffs(36, 54).addBox(2.5f, 3, 7.5f, 3, 3, 1),
                PartPose.ZERO);
        // The hitch bars at both ends, shown once the tub is coupled.
        root.addOrReplaceChild("couplings", CubeListBuilder.create()
                        .texOffs(36, 54).addBox(-10.5f, 0, -0.5f, 3.5f, 1, 1)
                        .texOffs(36, 54).addBox(7, 0, -0.5f, 3.5f, 1, 1),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MineTubRenderState state) {
        super.setupAnim(state);
        couplings.visible = state.coupled;
    }
}
