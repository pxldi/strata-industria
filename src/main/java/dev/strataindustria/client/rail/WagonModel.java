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
 * The steel wagons: ore, tank and flat. Like the mine tub they run along x, model space has +y down and the rail at
 * y = 6, and every part reads one 64 x 64 sheet laid out by {@code RailwayTextures}: paint above y 54, iron in the
 * strip from y 54 to 57, brass at x 32 and up below it and dark steel at x 31 and down.
 */
public class WagonModel extends EntityModel<MineTubRenderState> {
    public static final ModelLayerLocation ORE_LAYER = new ModelLayerLocation(StrataIndustria.id("ore_wagon"), "main");
    public static final ModelLayerLocation TANK_LAYER = new ModelLayerLocation(StrataIndustria.id("tank_wagon"), "main");
    public static final ModelLayerLocation FLAT_LAYER = new ModelLayerLocation(StrataIndustria.id("flat_wagon"), "main");

    private final ModelPart couplings;
    private final ModelPart needle;

    public WagonModel(ModelPart root) {
        super(root);
        couplings = root.getChild("couplings");
        needle = root.hasChild("needle") ? root.getChild("needle") : null;
    }

    // ---------------------------------------------------------------- shared parts

    /** Frame, buffers, wheels and the hitch bars shown once coupled. */
    private static PartDefinition chassis(PartDefinition root, int deckTop) {
        root.addOrReplaceChild("frame", CubeListBuilder.create()
                        .texOffs(0, 58).addBox(-8, deckTop + 2, -5, 16, 2, 10)
                        .texOffs(0, 58).addBox(-8.5f, deckTop + 1, -5.5f, 1, 2, 11)
                        .texOffs(0, 58).addBox(7.5f, deckTop + 1, -5.5f, 1, 2, 11),
                PartPose.ZERO);
        // Four spoked wheels outside the frame, and the axles between.
        root.addOrReplaceChild("wheels", CubeListBuilder.create()
                        .texOffs(36, 54).addBox(-5.5f, 3, -8.5f, 3, 3, 1)
                        .texOffs(36, 54).addBox(2.5f, 3, -8.5f, 3, 3, 1)
                        .texOffs(36, 54).addBox(-5.5f, 3, 7.5f, 3, 3, 1)
                        .texOffs(36, 54).addBox(2.5f, 3, 7.5f, 3, 3, 1)
                        .texOffs(0, 54).addBox(-4.5f, 4, -7.5f, 1, 1, 15)
                        .texOffs(0, 54).addBox(3.5f, 4, -7.5f, 1, 1, 15),
                PartPose.ZERO);
        // Round buffers on stalks at each end.
        root.addOrReplaceChild("buffers", CubeListBuilder.create()
                        .texOffs(36, 54).addBox(-10, deckTop, -4, 2, 2, 2)
                        .texOffs(36, 54).addBox(-10, deckTop, 2, 2, 2, 2)
                        .texOffs(36, 54).addBox(8, deckTop, -4, 2, 2, 2)
                        .texOffs(36, 54).addBox(8, deckTop, 2, 2, 2, 2),
                PartPose.ZERO);
        root.addOrReplaceChild("couplings", CubeListBuilder.create()
                        .texOffs(36, 54).addBox(-11.5f, deckTop + 1, -0.5f, 3.5f, 1, 1)
                        .texOffs(36, 54).addBox(8, deckTop + 1, -0.5f, 3.5f, 1, 1),
                PartPose.ZERO);
        return root;
    }

    // ---------------------------------------------------------------- the three bodies

    /** A steel hopper body 16 long and 15 wide with a rim, corner posts and two bands round it. */
    public static LayerDefinition createOreLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = chassis(mesh.getRoot(), 1);
        root.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -6.5f, 16, 2, 13), PartPose.ZERO);
        root.addOrReplaceChild("walls", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-8, -8, -7.5f, 16, 8, 1)
                        .texOffs(0, 16).addBox(-8, -8, 6.5f, 16, 8, 1)
                        .texOffs(0, 26).addBox(-8, -8, -6.5f, 1, 8, 13)
                        .texOffs(0, 26).addBox(7, -8, -6.5f, 1, 8, 13),
                PartPose.ZERO);
        root.addOrReplaceChild("rim", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-8.5f, -9, -8, 17, 1.5f, 1.5f)
                        .texOffs(0, 40).addBox(-8.5f, -9, 6.5f, 17, 1.5f, 1.5f)
                        .texOffs(0, 40).addBox(-8.5f, -9, -6.5f, 1.5f, 1.5f, 13)
                        .texOffs(0, 40).addBox(7, -9, -6.5f, 1.5f, 1.5f, 13),
                PartPose.ZERO);
        root.addOrReplaceChild("bands", CubeListBuilder.create()
                        .texOffs(0, 54).addBox(-4, -8, -8, 1, 9, 1)
                        .texOffs(0, 54).addBox(3, -8, -8, 1, 9, 1)
                        .texOffs(0, 54).addBox(-4, -8, 7, 1, 9, 1)
                        .texOffs(0, 54).addBox(3, -8, 7, 1, 9, 1),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** A flatbed with a horizontal tank on two saddles, a brass dome and a gauge needle on the near side. */
    public static LayerDefinition createTankLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = chassis(mesh.getRoot(), 1);
        root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 1, -6.5f, 16, 2, 13), PartPose.ZERO);
        // The tank is a plus-shaped section: a tall middle and a wide belly make a round-looking drum.
        root.addOrReplaceChild("tank", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-7, -8, -3, 14, 9, 6)
                        .texOffs(0, 31).addBox(-7, -6, -5, 14, 5, 10),
                PartPose.ZERO);
        root.addOrReplaceChild("straps", CubeListBuilder.create()
                        .texOffs(0, 54).addBox(-4.5f, -8.5f, -3.5f, 1, 10, 7)
                        .texOffs(0, 54).addBox(3.5f, -8.5f, -3.5f, 1, 10, 7)
                        .texOffs(0, 54).addBox(-4.5f, -6.5f, -5.5f, 1, 6, 11)
                        .texOffs(0, 54).addBox(3.5f, -6.5f, -5.5f, 1, 6, 11),
                PartPose.ZERO);
        root.addOrReplaceChild("dome", CubeListBuilder.create()
                        .texOffs(32, 58).addBox(-2, -10, -2, 4, 2, 4)
                        .texOffs(32, 58).addBox(-1, -11, -1, 2, 1, 2),
                PartPose.ZERO);
        // A sight glass on the near side: a dark slot and a bright needle that rides up it.
        root.addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 58).addBox(-1, -6, 5.1f, 2, 7, 1), PartPose.ZERO);
        root.addOrReplaceChild("needle", CubeListBuilder.create().texOffs(32, 58).addBox(-1.5f, 0, 5.6f, 3, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** A timber deck with four low stakes at the corners. */
    public static LayerDefinition createFlatLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = chassis(mesh.getRoot(), 1);
        root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 1.5f, -6.5f, 16, 2, 13), PartPose.ZERO);
        root.addOrReplaceChild("stakes", CubeListBuilder.create()
                        .texOffs(0, 54).addBox(-8, -1.5f, -6.5f, 1, 3, 1)
                        .texOffs(0, 54).addBox(7, -1.5f, -6.5f, 1, 3, 1)
                        .texOffs(0, 54).addBox(-8, -1.5f, 5.5f, 1, 3, 1)
                        .texOffs(0, 54).addBox(7, -1.5f, 5.5f, 1, 3, 1),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MineTubRenderState state) {
        super.setupAnim(state);
        couplings.visible = state.coupled;
        if (needle != null) needle.y = -state.gauge * 0.4f;
    }
}
